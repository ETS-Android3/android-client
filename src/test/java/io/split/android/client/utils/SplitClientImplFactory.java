package io.split.android.client.utils;

import static org.mockito.Mockito.mock;

import io.split.android.client.EvaluatorImpl;
import io.split.android.client.EventPropertiesProcessor;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitClientImpl;
import io.split.android.client.SplitFactory;
import io.split.android.client.api.Key;
import io.split.android.client.attributes.AttributesManager;
import io.split.android.client.attributes.AttributesMergerImpl;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.service.synchronizer.SyncManager;
import io.split.android.client.shared.SplitClientContainer;
import io.split.android.client.storage.mysegments.MySegmentsStorageContainer;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.storage.TelemetryStorage;
import io.split.android.client.validators.KeyValidatorImpl;
import io.split.android.client.validators.SplitValidator;
import io.split.android.client.validators.SplitValidatorImpl;
import io.split.android.client.validators.TreatmentManager;
import io.split.android.client.validators.TreatmentManagerFactory;
import io.split.android.client.validators.TreatmentManagerFactoryImpl;
import io.split.android.engine.experiments.SplitParser;

/**
 * Created by fernandomartin on 2/17/18.
 */

public class SplitClientImplFactory {

    public static SplitClientImpl get(Key key, SplitsStorage splitsStorage) {
        SplitClientConfig cfg = SplitClientConfig.builder().build();
        SplitEventsManager eventsManager = new SplitEventsManager(cfg);
        SplitParser splitParser = new SplitParser(mock(MySegmentsStorageContainer.class));
        TelemetryStorage telemetryStorage = mock(TelemetryStorage.class);
        TreatmentManagerFactory treatmentManagerFactory = new TreatmentManagerFactoryImpl(
                new KeyValidatorImpl(), new SplitValidatorImpl(), new ImpressionListener.NoopImpressionListener(),
                false, new AttributesMergerImpl(), telemetryStorage, new EvaluatorImpl(splitsStorage, splitParser)
        );

        AttributesManager attributesManager = mock(AttributesManager.class);
        SplitClientImpl c = new SplitClientImpl(
                mock(SplitFactory.class),
                mock(SplitClientContainer.class),
                key,
                splitParser,
                new ImpressionListener.NoopImpressionListener(),
                cfg,
                eventsManager,
                splitsStorage,
                mock(EventPropertiesProcessor.class),
                mock(SyncManager.class),
                attributesManager,
                telemetryStorage,
                mock(SplitValidator.class),
                treatmentManagerFactory.getTreatmentManager(key, eventsManager, attributesManager)
        );
        eventsManager.notifyInternalEvent(SplitInternalEvent.MY_SEGMENTS_UPDATED);
        eventsManager.notifyInternalEvent(SplitInternalEvent.SPLITS_UPDATED);
        return c;
    }

    public static SplitClientImpl get(Key key, SplitsStorage splitsStorage, ImpressionListener impressionListener) {
        SplitParser splitParser = new SplitParser(mock(MySegmentsStorageContainer.class));
        SplitClientConfig cfg = SplitClientConfig.builder().build();
        return new SplitClientImpl(
                mock(SplitFactory.class),
                mock(SplitClientContainer.class),
                key,
                splitParser,
                impressionListener,
                cfg,
                new SplitEventsManager(cfg),
                splitsStorage,
                mock(EventPropertiesProcessor.class),
                mock(SyncManager.class),
                mock(AttributesManager.class),
                mock(TelemetryStorage.class),
                mock(SplitValidator.class),
                mock(TreatmentManager.class)
        );
    }

    public static SplitClientImpl get(Key key, SplitsStorage splitsStorage, SplitEventsManager eventsManager) {
        SplitParser splitParser = new SplitParser(mock(MySegmentsStorageContainer.class));
        return new SplitClientImpl(
                mock(SplitFactory.class),
                mock(SplitClientContainer.class),
                key,
                splitParser,
                new ImpressionListener.NoopImpressionListener(),
                SplitClientConfig.builder().build(),
                eventsManager,
                splitsStorage,
                mock(EventPropertiesProcessor.class),
                mock(SyncManager.class),
                mock(AttributesManager.class),
                mock(TelemetryStorage.class),
                mock(SplitValidator.class),
                mock(TreatmentManager.class)
        );
    }
}
