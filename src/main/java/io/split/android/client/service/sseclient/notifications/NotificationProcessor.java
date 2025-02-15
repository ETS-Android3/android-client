package io.split.android.client.service.sseclient.notifications;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import com.google.gson.JsonSyntaxException;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.split.android.client.dtos.Split;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.sseclient.notifications.mysegments.MySegmentsNotificationProcessor;
import io.split.android.client.service.sseclient.notifications.mysegments.MySegmentsNotificationProcessorRegistry;
import io.split.android.client.utils.Logger;

public class NotificationProcessor implements MySegmentsNotificationProcessorRegistry {

    private final NotificationParser mNotificationParser;
    private final SplitTaskExecutor mSplitTaskExecutor;
    private final SplitTaskFactory mSplitTaskFactory;
    private final BlockingQueue<SplitsChangeNotification> mSplitsUpdateNotificationsQueue;
    private final ConcurrentMap<String, MySegmentsNotificationProcessor> mMySegmentsNotificationProcessors;
    private final MySegmentsPayloadDecoder mMySegmentsPayloadDecoder;

    public NotificationProcessor(
            @NonNull SplitTaskExecutor splitTaskExecutor,
            @NonNull SplitTaskFactory splitTaskFactory,
            @NonNull NotificationParser notificationParser,
            @NonNull BlockingQueue<SplitsChangeNotification> splitsUpdateNotificationsQueue,
            @NonNull MySegmentsPayloadDecoder mySegmentsPayloadDecoder) {
        mSplitTaskExecutor = checkNotNull(splitTaskExecutor);
        mSplitTaskFactory = checkNotNull(splitTaskFactory);
        mNotificationParser = checkNotNull(notificationParser);
        mSplitsUpdateNotificationsQueue = checkNotNull(splitsUpdateNotificationsQueue);
        mMySegmentsPayloadDecoder = checkNotNull(mySegmentsPayloadDecoder);
        mMySegmentsNotificationProcessors = new ConcurrentHashMap<>();
    }

    public void process(IncomingNotification incomingNotification) {
        try {
            String notificationJson = incomingNotification.getJsonData();
            switch (incomingNotification.getType()) {
                case SPLIT_UPDATE:
                    processSplitUpdate(mNotificationParser.parseSplitUpdate(notificationJson));
                    break;
                case SPLIT_KILL:
                    processSplitKill(mNotificationParser.parseSplitKill(notificationJson));
                    break;
                case MY_SEGMENTS_UPDATE:
                    processMySegmentUpdate(mNotificationParser.parseMySegmentUpdate(notificationJson),
                            mNotificationParser.extractUserKeyHashFromChannel(incomingNotification.getChannel()));
                    break;
                case MY_SEGMENTS_UPDATE_V2:
                    processMySegmentUpdateV2(mNotificationParser.parseMySegmentUpdateV2(notificationJson));
                    break;
                default:
                    Logger.e("Unknow notification arrived: " + notificationJson);
            }
        } catch (JsonSyntaxException e) {
            Logger.e("Error processing incoming push notification: " +
                    e.getLocalizedMessage());
        } catch (Exception e) {
            Logger.e("Unknown error while processing incoming push notification: " +
                    e.getLocalizedMessage());
        }
    }

    @Override
    public void registerMySegmentsProcessor(String matchingKey, MySegmentsNotificationProcessor processor) {
        mMySegmentsNotificationProcessors.put(matchingKey, processor);
    }

    @Override
    public void unregisterMySegmentsProcessor(String matchingKey) {
        mMySegmentsNotificationProcessors.remove(matchingKey);
    }

    private void processSplitUpdate(SplitsChangeNotification notification) {
        mSplitsUpdateNotificationsQueue.offer(notification);
    }

    private void processSplitKill(SplitKillNotification notification) {
        Split split = new Split();
        split.name = notification.getSplitName();
        split.defaultTreatment = notification.getDefaultTreatment();
        split.changeNumber = notification.getChangeNumber();
        mSplitTaskExecutor.submit(mSplitTaskFactory.createSplitKillTask(split), null);
        mSplitsUpdateNotificationsQueue.offer(new SplitsChangeNotification(split.changeNumber));
    }

    private void processMySegmentUpdate(MySegmentChangeNotification notification, String hashedUserKey) {
        for (Map.Entry<String, MySegmentsNotificationProcessor> processor : mMySegmentsNotificationProcessors.entrySet()) {
            String encodedProcessorKey = mMySegmentsPayloadDecoder.hashUserKeyForMySegmentsV1(processor.getKey());

            if (encodedProcessorKey == null) {
                continue;
            }

            if (encodedProcessorKey.equals(hashedUserKey)) {
                processor.getValue().processMySegmentsUpdate(notification);
            }
        }
    }

    private void processMySegmentUpdateV2(MySegmentChangeV2Notification notification) {
        for (MySegmentsNotificationProcessor processor : mMySegmentsNotificationProcessors.values()) {
            processor.processMySegmentsUpdateV2(notification);
        }
    }
}
