package io.split.android.client.localhost;

import static com.google.common.base.Preconditions.checkNotNull;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.collect.Maps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.split.android.client.dtos.Split;
import io.split.android.client.events.EventsManagerCoordinator;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.storage.legacy.FileStorage;
import io.split.android.client.storage.splits.ProcessedSplitChange;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.utils.FileUtils;
import io.split.android.client.utils.Logger;

public class LocalhostSplitsStorage implements SplitsStorage {

    private String mLocalhostFileName;
    private final Context mContext;
    private final Map<String, Split> mInMemorySplits = Maps.newConcurrentMap();
    private final FileStorage mFileStorage;
    private LocalhostFileParser mParser;
    private final EventsManagerCoordinator mEventsManager;
    private final FileUtils mFileUtils = new FileUtils();
    private String mLastContentLoaded = "";

    public LocalhostSplitsStorage(@Nullable String fileName,
                                  @NonNull Context context,
                                  @NonNull FileStorage fileStorage,
                                  @NonNull EventsManagerCoordinator eventsManager) {
        this.mLocalhostFileName = fileName;
        this.mContext = checkNotNull(context);
        this.mFileStorage = checkNotNull(fileStorage);
        this.mEventsManager = checkNotNull(eventsManager);
        this.setup();
    }

    @Override
    public void loadLocal() {
        loadSplits();
    }

    @Override
    public Split get(@NonNull String name) {
        return mInMemorySplits.get(name);
    }

    @Override
    public Map<String, Split> getMany(@NonNull List<String> splitNames) {
        Map<String, Split> splits = new HashMap<>();
        synchronized (this) {
            // Just double checking
            if (splitNames == null || splitNames.isEmpty()) {
                splits.putAll(mInMemorySplits);
                return splits;
            }

            for (String name : splitNames) {
                Split split = mInMemorySplits.get(name);
                if (split != null) {
                    splits.put(name, split);
                }
            }
        }
        return splits;
    }

    @Override
    public Map<String, Split> getAll() {
        Map<String, Split> splits = new HashMap<>();
        synchronized (this) {
            splits.putAll(mInMemorySplits);
        }
        return splits;
    }

    @Override
    public void update(ProcessedSplitChange splitChange) {
    }

    @Override
    public void updateWithoutChecks(Split split) {
    }

    @Override
    public boolean isValidTrafficType(@NonNull String name) {
        return true;
    }

    @Override
    public long getTill() {
        return 1;
    }

    @Override
    public long getUpdateTimestamp() {
        return 1;
    }

    @Override
    public String getSplitsFilterQueryString() {
        return "";
    }

    @Override
    public void updateSplitsFilterQueryString(String queryString) {
    }

    @Override
    public void clear() {
        mInMemorySplits.clear();
    }

    private void setup() {

        String fileName = mLocalhostFileName;
        if (fileName == null) {
            // First checking Yaml because it's the default
            fileName = getYamlFileName(mContext);
            if (fileName != null) {
                mLocalhostFileName = fileName;
            } else {
                // If yaml is not used, then check for the deprecated format
                mLocalhostFileName = ServiceConstants.DEFAULT_SPLITS_FILENAME + "." + ServiceConstants.PROPERTIES_EXTENSION;
                Logger.w("Localhost mode: .split mocks will be deprecated soon in favor of YAML files, which provide more targeting power. Take a look in our documentation.");
            }
        }

        if (mFileUtils.isPropertiesFileName(mLocalhostFileName)) {
            mParser = new LocalhostPropertiesFileParser();
        } else {
            mParser = new LocalhostYamlFileParser();
        }

        copyFileResourceToDataFolder(mLocalhostFileName, mFileStorage, mContext);
    }

    private void loadSplits() {
        String content;
        try {
            content = mFileStorage.read(mLocalhostFileName);
            Logger.i("Localhost file reloaded: " + mLocalhostFileName);
        } catch (IOException e) {
            Logger.e("Error reading localhost yaml file");
            return;
        }

        if (content == null) {
            return;
        }

        synchronized (this) {
            mInMemorySplits.clear();
            Map<String, Split> values = mParser.parse(content);
            if (values != null) {
                mInMemorySplits.putAll(values);
            }
            if (!content.equals(mLastContentLoaded)) {
                mEventsManager.notifyInternalEvent(SplitInternalEvent.SPLITS_LOADED_FROM_STORAGE);
                mEventsManager.notifyInternalEvent(SplitInternalEvent.SPLITS_FETCHED);
                mEventsManager.notifyInternalEvent(SplitInternalEvent.SPLITS_UPDATED);
            }
            mLastContentLoaded = content;
        }
    }

    @Nullable
    private String getYamlFileName(Context context) {

        List<String> extensions = Arrays.asList(ServiceConstants.YAML_EXTENSION, ServiceConstants.YML_EXTENSION);
        for (String extension : extensions) {
            String fileName = checkFileType(context, mFileUtils, extension);
            if (fileName != null) {
                return fileName;
            }
        }
        return null;
    }

    @Nullable
    private String checkFileType(Context context, FileUtils fileUtils, String extension) {
        String fileName = ServiceConstants.DEFAULT_SPLITS_FILENAME + "." + extension;
        if(fileUtils.fileExists(fileName, context)) {
            return  fileName;
        }
        return null;
    }

    private void copyFileResourceToDataFolder(String fileName, FileStorage fileStorage, Context context) {
        String content;
        try {
            FileUtils fileUtils = new FileUtils();
            content = fileUtils.loadFileContent(fileName, context);
            if(content != null) {
                fileStorage.write(fileName, content);
                Logger.i("LOCALHOST MODE: File location is: " + mFileStorage.getRootPath() + "/" + fileName);
            }
        } catch (IOException e) {
            Logger.e(e.getLocalizedMessage());
        }
    }
}
