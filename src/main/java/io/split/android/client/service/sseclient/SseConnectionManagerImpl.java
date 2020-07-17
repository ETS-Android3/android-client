package io.split.android.client.service.sseclient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.sseclient.notifications.StreamingError;
import io.split.android.client.service.sseclient.notifications.StreamingMessageParser;
import io.split.android.client.utils.Logger;

import static androidx.core.util.Preconditions.checkNotNull;
import static io.split.android.client.service.executor.SplitTaskType.GENERIC_TASK;
import static java.lang.reflect.Modifier.PRIVATE;

public class SseConnectionManagerImpl implements SseConnectionManager, SseClientListener, SplitTaskExecutionListener {

    private final static int SSE_KEEPALIVE_TIME_IN_SECONDS = 70;
    private final static int RECONNECT_TIME_BEFORE_TOKEN_EXP_IN_SECONDS = 600;
    private final static int DISCONNECT_ON_BG_TIME_IN_SECONDS = 60;
    private final static int TOKEN_EXPIRED_ERROR_CODE = 40142;

    private final SseClient mSseClient;
    private final SplitTaskExecutor mTaskExecutor;
    private final SplitTaskFactory mSplitTaskFactory;
    private final ReconnectBackoffCounter mAuthBackoffCounter;
    private final ReconnectBackoffCounter mSseBackoffCounter;

    private String mResetSseKeepAliveTimerTaskId = null;
    private String mSseTokenExpiredTimerTaskId = null;
    private String mAuthReconnectionTimerTaskId = null;
    private String mSseReconnectionTimerTaskId = null;

    private SseJwtToken mLastJwtTokenObtained = null;
    private AtomicBoolean mIsHostAppInBackground;
    private AtomicBoolean mIsStopped;
    private AtomicBoolean mIsAuthenticating;
    private WeakReference<SseConnectionManagerListener> mListenerRef;
    private StreamingMessageParser mStreamingMessageParser;

    public SseConnectionManagerImpl(@NonNull SseClient sseClient,
                                    @NonNull SplitTaskExecutor taskExecutor,
                                    @NonNull SplitTaskFactory splitTaskFactory,
                                    @NonNull StreamingMessageParser streamingMessageParser,
                                    @NonNull ReconnectBackoffCounter authBackoffCounter,
                                    @NonNull ReconnectBackoffCounter sseBackoffCounter) {

        mSseClient = checkNotNull(sseClient);
        mSplitTaskFactory = checkNotNull(splitTaskFactory);
        mTaskExecutor = checkNotNull(taskExecutor);
        mStreamingMessageParser = checkNotNull(streamingMessageParser);
        mAuthBackoffCounter = checkNotNull(authBackoffCounter);
        mSseBackoffCounter = checkNotNull(sseBackoffCounter);
        mIsHostAppInBackground = new AtomicBoolean(false);
        mIsStopped = new AtomicBoolean(false);
        mIsAuthenticating = new AtomicBoolean(false);
        mSseClient.setListener(this);
    }

    @Override
    public void setListener(SseConnectionManagerListener listener) {
        mListenerRef = new WeakReference<>(listener);
    }

    @Override
    public void start() {
        triggerSseAuthentication();
    }

    @Override
    public void stop() {
        mIsStopped.set(true);
        cancelRefreshTokenTimer();
        cancelSseKeepAliveTimer();
        mSseClient.close();
    }

    @Override
    public void pause() {
        if (mIsStopped.get()) {
            return;
        }
        mIsHostAppInBackground.set(true);
        if(mAuthReconnectionTimerTaskId != null && mIsAuthenticating.get()) {
            mIsAuthenticating.set(false);
        }
        cancelAuthReconnectionTimer();
        cancelSseReconnectionTimer();
        mSseClient.scheduleDisconnection(DISCONNECT_ON_BG_TIME_IN_SECONDS);
    }

    @Override
    public void resume() {
        if (mIsStopped.get()) {
            return;
        }
        mIsHostAppInBackground.set(false);
        // Checking sse client status, cancel scheduled disconnect if necessary
        // and check if cancel was successful
        boolean isDisconnectionTimerCancelled = mSseClient.cancelDisconnectionTimer();
        if (!mIsAuthenticating.get() && mSseClient.readyState() == SseClient.CLOSED ||
                (mSseClient.readyState() == SseClient.OPEN && !isDisconnectionTimerCancelled)) {
            mIsAuthenticating.set(true);
            triggerSseAuthentication();
        }
    }

    private void notifySseAvailable() {
        SseConnectionManagerListener listener = mListenerRef.get();
        if (listener != null) {
            listener.onSseAvailable();
        }
    }

    private void notifySseNotAvailable() {
        SseConnectionManagerListener listener = mListenerRef.get();
        if (listener != null) {
            listener.onSseNotAvailable();
        }
    }

    private void connectToSse(String token, List<String> channels) {
        mSseClient.connect(token, channels);
    }

    private void triggerSseAuthentication() {
        Logger.d("Connecting to SSE server");
        mTaskExecutor.submit(
                mSplitTaskFactory.createSseAuthenticationTask(),
                this);
    }

    @Override
    public void onOpen() {
        if (mIsStopped.get()) {
            return;
        }
        cancelAuthReconnectionTimer();
        cancelSseReconnectionTimer();
        mSseBackoffCounter.resetCounter();
        resetSseKeepAliveTimer();
        notifySseAvailable();
    }

    @Override
    public void onMessage(Map<String, String> values) {
        if (mIsStopped.get()) {
            return;
        }
        resetSseKeepAliveTimer();
        processEvent(values);
    }

    private void processEvent(Map<String, String> values) {
        StreamingError error = mStreamingMessageParser.parseError(values);
        if(error != null && error.getCode() == TOKEN_EXPIRED_ERROR_CODE) {
            cancelSseReconnectionTimer();
            cancelAuthReconnectionTimer();
            cancelRefreshTokenTimer();
            cancelSseKeepAliveTimer();
            if (mIsHostAppInBackground.get()) {
                mSseClient.cancelDisconnectionTimer();
                return;
            }
            triggerSseAuthentication();
        }
    }

    @Override
    public void onKeepAlive() {
        resetSseKeepAliveTimer();
    }

    @Override
    public void onError(boolean isRecoverable) {
        if (mIsStopped.get()) {
            return;
        }
        notifySseNotAvailable();

        cancelSseKeepAliveTimer();
        if (mIsHostAppInBackground.get()) {
            mSseClient.cancelDisconnectionTimer();
            return;
        }

        if (isRecoverable) {
            scheduleSseReconnection();
        }
    }

    @Override
    public void onDisconnect() {
        mIsAuthenticating.set(false);
        cancelSseKeepAliveTimer();
        cancelRefreshTokenTimer();
    }

    private void cancelSseKeepAliveTimer() {
        if (mResetSseKeepAliveTimerTaskId != null) {
            mTaskExecutor.stopTask(mResetSseKeepAliveTimerTaskId);
            mResetSseKeepAliveTimerTaskId = null;
        }
    }

    private void cancelRefreshTokenTimer() {
        if (mSseTokenExpiredTimerTaskId != null) {
            mTaskExecutor.stopTask(mSseTokenExpiredTimerTaskId);
            mSseTokenExpiredTimerTaskId = null;
        }
    }

    private void cancelAuthReconnectionTimer() {
        if (mAuthReconnectionTimerTaskId != null) {
            mTaskExecutor.stopTask(mAuthReconnectionTimerTaskId);
            mAuthReconnectionTimerTaskId = null;
        }
    }

    private void cancelSseReconnectionTimer() {
        if (mSseReconnectionTimerTaskId != null) {
            mTaskExecutor.stopTask(mSseReconnectionTimerTaskId);
            mSseReconnectionTimerTaskId = null;
        }
    }

    private void refreshSseToken() {
        cancelSseKeepAliveTimer();
        mSseClient.disconnect();
        triggerSseAuthentication();
    }

    private void resetSseKeepAliveTimer() {
        cancelSseKeepAliveTimer();
        mResetSseKeepAliveTimerTaskId = mTaskExecutor.schedule(
                new SseKeepAliveTimer(),
                SSE_KEEPALIVE_TIME_IN_SECONDS,
                null);
    }

    private void resetSseTokenExpiredTimer(long issueAtTime, long expirationTime) {
        long reconnectTime = reconnectTimeBeforeTokenExpiration(issueAtTime, expirationTime);
        mSseTokenExpiredTimerTaskId = mTaskExecutor.schedule(
                new SseTokenExpiredTimer(), reconnectTime, null);
    }

    @VisibleForTesting(otherwise = PRIVATE)
    public long reconnectTimeBeforeTokenExpiration(long issuedAtTime, long expirationTime) {
        return Math.max((expirationTime - issuedAtTime) - RECONNECT_TIME_BEFORE_TOKEN_EXP_IN_SECONDS
                , 0L);
    }

    private void scheduleReconnection() {
        cancelAuthReconnectionTimer();
        mIsAuthenticating.set(true);
        mAuthReconnectionTimerTaskId = mTaskExecutor.schedule(
                mSplitTaskFactory.createSseAuthenticationTask(),
                mAuthBackoffCounter.getNextRetryTime(), this);
    }

    private void scheduleSseReconnection() {
        cancelSseReconnectionTimer();
        mSseReconnectionTimerTaskId = mTaskExecutor.schedule(
                new SseReconnectionTimer(),
                mSseBackoffCounter.getNextRetryTime(), null);
    }

    //
//      Split Task Executor Listener implementation
//
    @Override
    public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
        if (mIsStopped.get()) {
            return;
        }

        switch (taskInfo.getTaskType()) {
            case SSE_AUTHENTICATION_TASK:
                mIsAuthenticating.set(false);
                if(mIsHostAppInBackground.get()) {
                    return;
                }
                if (isUnexepectedError(taskInfo)) {
                    scheduleReconnection();
                    notifySseNotAvailable();
                    return;
                }

                Logger.d("Streaming enabled: " + isStreamingEnabled(taskInfo));
                if ((!SplitTaskExecutionStatus.SUCCESS.equals(taskInfo.getStatus())
                        && !isApiKeyValid(taskInfo))) {
                    Logger.e("Couldn't connect to SSE server. Invalid apikey ");
                    stop();
                    notifySseNotAvailable();
                    return;
                }

                if (SplitTaskExecutionStatus.SUCCESS.equals(taskInfo.getStatus())
                        && !isStreamingEnabled(taskInfo)) {
                    Logger.e("Will not connect to SSE server. Streaming disabled.");
                    stop();
                    notifySseNotAvailable();
                    return;
                }

                SseJwtToken jwtToken = unpackResult(taskInfo);
                if (jwtToken != null && jwtToken.getChannels().size() > 0) {
                    cancelAuthReconnectionTimer();
                    cancelSseReconnectionTimer();
                    mAuthBackoffCounter.resetCounter();
                    storeJwt(jwtToken);
                    connectToSse(jwtToken.getRawJwt(), jwtToken.getChannels());
                    resetSseTokenExpiredTimer(jwtToken.getIssuedAtTime(), jwtToken.getExpirationTime());
                } else {
                    scheduleReconnection();
                    notifySseNotAvailable();
                }
                break;
            default:
                Logger.e("Push notification manager unknown task: "
                        + taskInfo.getTaskType());
        }
    }

    private boolean isUnexepectedError(SplitTaskExecutionInfo taskInfo) {
        Boolean unexpectedErrorOcurred =
                taskInfo.getBoolValue(SplitTaskExecutionInfo.UNEXPECTED_ERROR);
        return unexpectedErrorOcurred != null && unexpectedErrorOcurred.booleanValue();
    }

    private boolean isApiKeyValid(SplitTaskExecutionInfo taskInfo) {
        Boolean isApiKeyValid =
                taskInfo.getBoolValue(SplitTaskExecutionInfo.IS_VALID_API_KEY);
        return isApiKeyValid != null && isApiKeyValid.booleanValue();
    }

    private boolean isStreamingEnabled(SplitTaskExecutionInfo taskInfo) {
        Boolean isStreamingEnabled =
                taskInfo.getBoolValue(SplitTaskExecutionInfo.IS_STREAMING_ENABLED);
        return isStreamingEnabled != null && isStreamingEnabled.booleanValue();
    }

    synchronized private void storeJwt(SseJwtToken token) {
        mLastJwtTokenObtained = token;
    }

    synchronized private SseJwtToken getLastJwt() {
        return mLastJwtTokenObtained;
    }

    @Nullable
    private SseJwtToken unpackResult(SplitTaskExecutionInfo taskInfo) {


        Object token = taskInfo.getObjectValue(SplitTaskExecutionInfo.PARSED_SSE_JWT);
        if (token != null) {
            try {
                return (SseJwtToken) token;
            } catch (ClassCastException e) {
                Logger.e("Sse authentication error. JWT not valid: " +
                        e.getLocalizedMessage());
            }
        } else {
            Logger.e("Sse authentication error. Token not available.");
        }

        return null;
    }

    @VisibleForTesting(otherwise = PRIVATE)
    public class SseKeepAliveTimer implements SplitTask {
        @NonNull
        @Override
        public SplitTaskExecutionInfo execute() {
            triggerSseAuthentication();
            notifySseNotAvailable();
            return SplitTaskExecutionInfo.success(GENERIC_TASK);
        }
    }

    @VisibleForTesting(otherwise = PRIVATE)
    public class SseTokenExpiredTimer implements SplitTask {
        @NonNull
        @Override
        public SplitTaskExecutionInfo execute() {
            Logger.d("Refreshing sse token");
            refreshSseToken();
            return SplitTaskExecutionInfo.success(GENERIC_TASK);
        }
    }

    @VisibleForTesting(otherwise = PRIVATE)
    public class SseReconnectionTimer implements SplitTask {
        @NonNull
        @Override
        public SplitTaskExecutionInfo execute() {
            Logger.d("Reconnecting to SSE server");
            SseJwtToken token = getLastJwt();
            connectToSse(token.getRawJwt(), token.getChannels());
            return SplitTaskExecutionInfo.success(GENERIC_TASK);
        }
    }
}
