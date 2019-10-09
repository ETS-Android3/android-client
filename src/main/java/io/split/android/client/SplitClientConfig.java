package io.split.android.client;


import io.split.android.android_client.BuildConfig;
import io.split.android.client.impressions.ImpressionListener;

import java.io.IOException;
import java.util.Properties;

import io.split.android.client.utils.Logger;

/**
 * Configurations for the SplitClient.
 */
public class SplitClientConfig {

    private final String _endpoint;
    private final String _eventsEndpoint;
    private static String _hostname;
    private static String _ip;

    private final int _featuresRefreshRate;
    private final int _segmentsRefreshRate;
    private final int _impressionsRefreshRate;
    private final int _impressionsQueueSize;
    private final int _impressionsMaxSentAttempts = 3;
    private final long _impressionsChunkOudatedTime = 3600 * 1000; // One day millis

    private final int _metricsRefreshRate;
    private final int _connectionTimeout;
    private final int _readTimeout;
    private final int _numThreadsForSegmentFetch;
    private final boolean _debugEnabled;
    private final boolean _labelsEnabled;
    private final int _ready;
    private final ImpressionListener _impressionListener;
    private final int _waitBeforeShutdown;
    private long _impressionsChunkSize;

    //.Track configuration
    private final int _eventsQueueSize;
    private final int _eventsPerPush;
    private final long _eventFlushInterval;
    private final String _trafficType;
    private final int _eventsMaxSentAttemps = 3;
    private final int _maxQueueSizeInBytes = 5242880; // 5mb

    // Validation settings
    private static final int _maximumKeyLength = 250;
    private static final String _trackEventNamePattern = "^[a-zA-Z0-9][-_.:a-zA-Z0-9]{0,79}$";

    // Data folder
    private static final String _defaultDataFolder = "split_data";

    // To be set during startup
    public static String splitSdkVersion;

    public static Builder builder() {
        return new Builder();
    }

    private SplitClientConfig(String endpoint,
                              String eventsEndpoint,
                              int pollForFeatureChangesEveryNSeconds,
                              int segmentsRefreshRate,
                              int impressionsRefreshRate,
                              int impressionsQueueSize,
                              long impressionsChunkSize, int metricsRefreshRate,
                              int connectionTimeout,
                              int readTimeout,
                              int numThreadsForSegmentFetch,
                              int ready,
                              boolean debugEnabled,
                              boolean labelsEnabled,
                              ImpressionListener impressionListener,
                              int waitBeforeShutdown,
                              String hostname,
                              String ip,
                              int eventsQueueSize,
                              int eventsPerPush,
                              long eventFlushInterval,
                              String trafficType) {
        _endpoint = endpoint;
        _eventsEndpoint = eventsEndpoint;
        _featuresRefreshRate = pollForFeatureChangesEveryNSeconds;
        _segmentsRefreshRate = segmentsRefreshRate;
        _impressionsRefreshRate = impressionsRefreshRate;
        _impressionsQueueSize = impressionsQueueSize;
        _metricsRefreshRate = metricsRefreshRate;
        _connectionTimeout = connectionTimeout;
        _readTimeout = readTimeout;
        _numThreadsForSegmentFetch = numThreadsForSegmentFetch;
        _ready = ready;
        _debugEnabled = debugEnabled;
        _labelsEnabled = labelsEnabled;
        _impressionListener = impressionListener;
        _waitBeforeShutdown = waitBeforeShutdown;
        _impressionsChunkSize = impressionsChunkSize;
        _hostname = hostname;
        _ip = ip;

        _eventsQueueSize = eventsQueueSize;
        _eventsPerPush = eventsPerPush;
        _eventFlushInterval = eventFlushInterval;
        _trafficType = trafficType;

        splitSdkVersion = "Android-" + BuildConfig.VERSION_NAME;

        if (_debugEnabled) {
            Logger.instance().debugLevel(true);
        }
    }

    private static boolean isTestMode() {
        boolean result;
        try {
            Class.forName("io.split.android.client.SplitClientConfigTest");
            result = true;
        } catch (final Exception e) {
            result = false;
        }
        return result;
    }

    public String trafficType() {
        return _trafficType;
    }

    public long eventFlushInterval() {
        return _eventFlushInterval;
    }

    public int eventsQueueSize() {
        return _eventsQueueSize;
    }

    public int eventsPerPush() {
        return _eventsPerPush;
    }

    public String endpoint() {
        return _endpoint;
    }

    public String eventsEndpoint() {
        return _eventsEndpoint;
    }

    public int featuresRefreshRate() {
        return _featuresRefreshRate;
    }

    public int segmentsRefreshRate() {
        return _segmentsRefreshRate;
    }

    public int numThreadsForSegmentFetch() {
        return _numThreadsForSegmentFetch;
    }

    public int impressionsRefreshRate() {
        return _impressionsRefreshRate;
    }

    public int impressionsQueueSize() {
        return _impressionsQueueSize;
    }

    public long impressionsChunkSize() {
        return _impressionsChunkSize;
    }

    public int metricsRefreshRate() {
        return _metricsRefreshRate;
    }

    public int connectionTimeout() {
        return _connectionTimeout;
    }

    public int readTimeout() {
        return _readTimeout;
    }

    public boolean debugEnabled() {
        return _debugEnabled;
    }

    public boolean labelsEnabled() {
        return _labelsEnabled;
    }

    public int blockUntilReady() {
        return _ready;
    }

    public ImpressionListener impressionListener() {
        return _impressionListener;
    }

    public int waitBeforeShutdown() {
        return _waitBeforeShutdown;
    }

    public String hostname() {
        return _hostname;
    }

    /**
     * Maximum attempts count while sending impressions.
     * to the server. Internal setting.
     *
     * @return Maximum attempts limit.
     */

    int impressionsMaxSentAttempts() {
        return _impressionsMaxSentAttempts;
    }

    /**
     * Elapsed time in millis to consider that a chunk of impression
     * is outdated. Internal property
     *
     * @return Time in millis.
     */
    long impressionsChunkOutdatedTime() {
        return _impressionsChunkOudatedTime;
    }

    /**
     * Maximum attempts count while sending tracks
     * to the server. Internal setting.
     *
     * @return Maximum attempts limit.
     */

    int eventsMaxSentAttempts() {
        return _eventsMaxSentAttemps;
    }

    /**
     * Maximum events queue size in bytes
     *
     * @return Maximum events queue size in bytes.
     */
    int maxQueueSizeInBytes() {
        return _maxQueueSizeInBytes;
    }


    /**
     * Regex to validate Track event name
     *
     * @return Regex pattern string
     */
    String trackEventNamePattern() {
        return _trackEventNamePattern;
    }


    /**
     * Maximum key char length for matching and bucketing
     *
     * @return Maximum char length
     */
    int maximumKeyLength() {
        return _maximumKeyLength;
    }

    /**
     * Default data folder to use when some
     * problem arises while creating it
     * based on api key
     *
     * @return Default data folder
     */
    String defaultDataFolder() {
        return _defaultDataFolder;
    }

    public String ip() {
        return _ip;
    }

    public static final class Builder {

        private String _endpoint = "https://sdk.split.io/api";
        private boolean _endpointSet = false;
        private String _eventsEndpoint = "https://events.split.io/api";
        private boolean _eventsEndpointSet = false;

        private int _featuresRefreshRate = 3600;
        private int _segmentsRefreshRate = 1800;
        private int _impressionsRefreshRate = 1800;
        private int _impressionsQueueSize = 30000;
        private int _connectionTimeout = 15000;
        private int _readTimeout = 15000;
        private int _numThreadsForSegmentFetch = 2;
        private boolean _debugEnabled = false;
        private int _ready = -1; // -1 means no blocking
        private int _metricsRefreshRate = 1800;
        private boolean _labelsEnabled = true;
        private ImpressionListener _impressionListener;
        private int _waitBeforeShutdown = 5000;
        private long _impressionsChunkSize = 2 * 1024; //2KB default size

        //.track configuration
        private int _eventsQueueSize = 10000;
        private long _eventFlushInterval = 1800;
        private int _eventsPerPush = 2000;
        private String _trafficType = null;

        private String _hostname = "unknown";
        private String _ip = "unknown";

        public Builder() {
        }

        /**
         * Default Traffic Type to use in .track method
         *
         * @param trafficType
         * @return this builder
         */
        public Builder trafficType(String trafficType) {
            _trafficType = trafficType;
            return this;
        }

        /**
         * Max size of the queue to trigger a flush
         *
         * @param eventsQueueSize
         * @return this builder
         */
        public Builder eventsQueueSize(int eventsQueueSize) {
            _eventsQueueSize = eventsQueueSize;
            return this;
        }

        /**
         * Max size of the batch to push events
         *
         * @param eventsPerPush
         * @return this builder
         */
        public Builder eventsPerPush(int eventsPerPush) {
            _eventsPerPush = eventsPerPush;
            return this;
        }

        /**
         * How often to flush data to the collection services
         *
         * @param eventFlushInterval
         * @return this builder
         */
        public Builder eventFlushInterval(long eventFlushInterval) {
            _eventFlushInterval = eventFlushInterval;
            return this;
        }

        /**
         * The rest endpoint that sdk will hit for latest features and segments.
         *
         * @param endpoint MUST NOT be null
         * @return this builder
         */
        public Builder endpoint(String endpoint, String eventsEndpoint) {
            _endpoint = endpoint;
            _eventsEndpoint = eventsEndpoint;
            return this;
        }

        /**
         * The SDK will poll the endpoint for changes to features at this period.
         * <p>
         * Implementation Note: The SDK actually polls at a random interval
         * chosen between (0.5 * n, n). This is to ensure that
         * SDKs that are deployed simultaneously on different machines do not
         * inundate the backend with requests at the same interval.
         * </p>
         *
         * @param seconds MUST be greater than 0. Default value is 60.
         * @return this builder
         */
        public Builder featuresRefreshRate(int seconds) {
            _featuresRefreshRate = seconds;
            return this;
        }

        /**
         * The SDK will poll the endpoint for changes to segments at this period in seconds.
         * <p>
         * Implementation Note: The SDK actually polls at a random interval
         * chosen between (0.5 * n, n). This is to ensure that
         * SDKs that are deployed simultaneously on different machines do not
         * inundate the backend with requests at the same interval.
         * </p>
         *
         * @param seconds MUST be greater than 0. Default value is 60.
         * @return this builder
         */
        public Builder segmentsRefreshRate(int seconds) {
            _segmentsRefreshRate = seconds;
            return this;
        }

        /**
         * The ImpressionListener captures the which key saw what treatment ("on", "off", etc)
         * at what time. This log is periodically pushed back to split endpoint.
         * This parameter controls how quickly does the cache expire after a write.
         * <p/>
         * This is an ADVANCED parameter
         *
         * @param seconds MUST be > 0.
         * @return this builder
         */
        public Builder impressionsRefreshRate(int seconds) {
            _impressionsRefreshRate = seconds;
            return this;
        }

        /**
         * The impression listener captures the which key saw what treatment ("on", "off", etc)
         * at what time. This log is periodically pushed back to split endpoint.
         * This parameter controls the in-memory queue size to store them before they are
         * pushed back to split endpoint.
         * <p>
         * If the value chosen is too small and more than the default size(5000) of impressions
         * are generated, the old ones will be dropped and the sdk will show a warning.
         * <p/>
         * <p>
         * This is an ADVANCED parameter.
         *
         * @param impressionsQueueSize MUST be > 0. Default is 5000.
         * @return this builder
         */
        public Builder impressionsQueueSize(int impressionsQueueSize) {
            _impressionsQueueSize = impressionsQueueSize;
            return this;
        }

        /**
         * You can provide your own ImpressionListener to capture all impressions
         * generated by SplitClient. An Impression is generated each time getTreatment is called.
         * <p>
         * <p>
         * Note that we will wrap any ImpressionListener provided in our own implementation
         * with an Executor controlling impressions going into your ImpressionListener. This is
         * done to protect SplitClient from any slowness caused by your ImpressionListener. The
         * Executor will be given the capacity you provide as parameter which is the
         * number of impressions that can be saved in a blocking queue while waiting for
         * your ImpressionListener to log them. Of course, the larger the value of capacity,
         * the more memory can be taken up.
         * <p>
         * <p>
         * The executor will create two threads.
         * <p>
         * <p>
         * This is an ADVANCED function.
         *
         * @param impressionListener
         * @return this builder
         */
        public Builder impressionListener(ImpressionListener impressionListener) {
            _impressionListener = impressionListener;
            return this;
        }

        /**
         * The diagnostic metrics collected by the SDK are pushed back to split endpoint
         * at this period.
         * <p/>
         * This is an ADVANCED parameter
         *
         * @param seconds MUST be > 0.
         * @return this builder
         */
        public Builder metricsRefreshRate(int seconds) {
            _metricsRefreshRate = seconds;
            return this;
        }

        /**
         * Http client connection timeout. Default value is 15000ms.
         *
         * @param ms MUST be greater than 0.
         * @return this builder
         */

        public Builder connectionTimeout(int ms) {
            _connectionTimeout = ms;
            return this;
        }

        /**
         * Http client read timeout. Default value is 15000ms.
         *
         * @param ms MUST be greater than 0.
         * @return this builder
         */
        public Builder readTimeout(int ms) {
            _readTimeout = ms;
            return this;
        }

        public Builder enableDebug() {
            _debugEnabled = true;
            return this;
        }

        /**
         * Disable label capturing
         *
         * @return this builder
         */
        public Builder disableLabels() {
            _labelsEnabled = false;
            return this;
        }


        /**
         * The SDK kicks off background threads to download data necessary
         * for using the SDK. You can choose to block until the SDK has
         * downloaded split definitions so that you will not get
         * the 'control' treatment.
         * <p/>
         * <p/>
         * If this parameter is set to a non-negative value, the SDK
         * will block for that number of milliseconds for the data to be downloaded.
         * <p/>
         * <p/>
         * If the download is not successful in this time period, a TimeOutException
         * will be thrown.
         * <p/>
         * <p/>
         * A negative value implies that the SDK building MUST NOT block. In this
         * scenario, the SDK might return the 'control' treatment until the
         * desired data has been downloaded.
         *
         * @param milliseconds MUST BE greater than or equal to 0;
         * @return this builder
         */
        public Builder ready(int milliseconds) {
            _ready = milliseconds;
            return this;
        }

        /**
         * How long to wait for impressions background thread before shutting down
         * the underlying connections.
         *
         * @param waitTime tine in milliseconds
         * @return this builder
         */
        public Builder waitBeforeShutdown(int waitTime) {
            _waitBeforeShutdown = waitTime;
            return this;
        }

        /**
         * Maximum size for impressions chunk to dump to storage and post.
         *
         * @param size MUST be > 0.
         * @return this builder
         */
        public Builder impressionsChunkSize(long size) {
            _impressionsChunkSize = size;
            return this;
        }

        /**
         * The host name for the current device.
         *
         * @param hostname
         * @return this builder
         */
        public Builder hostname(String hostname) {
            _hostname = hostname;
            return this;
        }

        /**
         * The current device IP adress.
         *
         * @param ip
         * @return this builder
         */
        public Builder ip(String ip) {
            _ip = ip;
            return this;
        }

        public SplitClientConfig build() {

            if (_featuresRefreshRate < 30) {
                throw new IllegalArgumentException("featuresRefreshRate must be >= 30: " + _featuresRefreshRate);
            }

            if (_segmentsRefreshRate < 30) {
                throw new IllegalArgumentException("segmentsRefreshRate must be >= 30: " + _segmentsRefreshRate);
            }

            if (_impressionsRefreshRate < 30) {
                throw new IllegalArgumentException("impressionsRefreshRate must be >= 30: " + _impressionsRefreshRate);
            }

            if (_metricsRefreshRate < 30) {
                throw new IllegalArgumentException("metricsRefreshRate must be >= 30: " + _metricsRefreshRate);
            }

            if (_impressionsQueueSize <= 0) {
                throw new IllegalArgumentException("impressionsQueueSize must be > 0: " + _impressionsQueueSize);
            }

            if (_impressionsChunkSize <= 0) {
                throw new IllegalArgumentException("impressionsChunkSize must be > 0: " + _impressionsChunkSize);
            }

            if (_connectionTimeout <= 0) {
                throw new IllegalArgumentException("connectionTimeOutInMs must be > 0: " + _connectionTimeout);
            }

            if (_readTimeout <= 0) {
                throw new IllegalArgumentException("readTimeout must be > 0: " + _readTimeout);
            }

            if (_endpoint == null) {
                throw new IllegalArgumentException("endpoint must not be null");
            }

            if (_eventsEndpoint == null) {
                throw new IllegalArgumentException("events endpoint must not be null");
            }

            if (_endpointSet && !_eventsEndpointSet) {
                throw new IllegalArgumentException("If endpoint is set, you must also set the events endpoint");
            }

            if (_numThreadsForSegmentFetch <= 0) {
                throw new IllegalArgumentException("Number of threads for fetching segments MUST be greater than zero");
            }

            return new SplitClientConfig(
                    _endpoint,
                    _eventsEndpoint,
                    _featuresRefreshRate,
                    _segmentsRefreshRate,
                    _impressionsRefreshRate,
                    _impressionsQueueSize,
                    _impressionsChunkSize, _metricsRefreshRate,
                    _connectionTimeout,
                    _readTimeout,
                    _numThreadsForSegmentFetch,
                    _ready,
                    _debugEnabled,
                    _labelsEnabled,
                    _impressionListener,
                    _waitBeforeShutdown,
                    _hostname,
                    _ip,
                    _eventsQueueSize,
                    _eventsPerPush,
                    _eventFlushInterval,
                    _trafficType);
        }

        public void set_impressionsChunkSize(long _impressionsChunkSize) {
            this._impressionsChunkSize = _impressionsChunkSize;
        }
    }
}