package io.split.android.client.localhost;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import javax.security.auth.Destroyable;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.lifecycle.SplitLifecycleAware;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.splits.LoadSplitsTask;
import io.split.android.client.storage.splits.SplitsStorage;

public class LocalhostSynchronizer implements SplitLifecycleAware, Destroyable {
    private final SplitTaskExecutor mTaskExecutor;
    private final int mRefreshRate;
    private final SplitsStorage mSplitsStorage;

    public LocalhostSynchronizer(@NonNull SplitTaskExecutor taskExecutor,
                                 @NonNull SplitClientConfig splitClientConfig,
                                 @NonNull SplitsStorage splitsStorage) {
        mTaskExecutor = checkNotNull(taskExecutor);
        mRefreshRate = checkNotNull(splitClientConfig).offlineRefreshRate();
        mSplitsStorage = checkNotNull(splitsStorage);
    }

    public void start() {
        SplitTask loadTask = new LoadSplitsTask(mSplitsStorage);
        if (mRefreshRate > 0) {
            mTaskExecutor.schedule(
                    loadTask, 0,
                    mRefreshRate, null);
        } else {
            mTaskExecutor.submit(loadTask, null);
        }
    }

    public void pause() {
        mTaskExecutor.pause();
    }

    public void resume() {
        mTaskExecutor.resume();
    }

    public void stop() {
        mTaskExecutor.stop();
    }
}
