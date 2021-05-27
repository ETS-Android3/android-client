package io.split.android.client.service;

import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

import io.split.android.client.service.impressions.ImpressionCounter;
import io.split.android.client.service.impressions.ImpressionUtils;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class ImpressionCounterTest {

    private long makeTimestamp(int year, int month, int day, int hour, int minute, int second) {
        return ZonedDateTime.of(year, month, day, hour, minute, second, 0, ZoneId.of("UTC")).toInstant().toEpochMilli();
    }

    @Test
    public void testTruncateTimeFrame() {
        assertThat(ImpressionUtils.truncateTimeframe(makeTimestamp(2020, 9, 2, 10, 53, 12)),
                is(equalTo(makeTimestamp(2020, 9, 2, 10, 0, 0))));
        assertThat(ImpressionUtils.truncateTimeframe(makeTimestamp(2020, 9, 2, 10, 0, 0)),
                is(equalTo(makeTimestamp(2020, 9, 2, 10, 0, 0))));
        assertThat(ImpressionUtils.truncateTimeframe(makeTimestamp(2020, 9, 2, 10, 53, 0 )),
                is(equalTo(makeTimestamp(2020, 9, 2, 10, 0, 0))));
        assertThat(ImpressionUtils.truncateTimeframe(makeTimestamp(2020, 9, 2, 10, 0, 12)),
                is(equalTo(makeTimestamp(2020, 9, 2, 10, 0, 0))));
        assertThat(ImpressionUtils.truncateTimeframe(makeTimestamp(1970, 1, 1, 0, 0, 0)),
                is(equalTo(makeTimestamp(1970, 1, 1, 0, 0, 0))));
    }

    @Test
    public void testBasicUsage() {
        final ImpressionCounter counter = new ImpressionCounter();
        final long timestamp = makeTimestamp(2020, 9, 2, 10, 10, 12);
        counter.inc("feature1", timestamp, 1);
        counter.inc("feature1", timestamp + 1, 1);
        counter.inc("feature1", timestamp + 2, 1);
        counter.inc("feature2", timestamp + 3, 2);
        counter.inc("feature2", timestamp + 4, 2);
        Map<ImpressionCounter.Key, Integer> counted = counter.popAll();
        assertThat(counted.size(), is(equalTo(2)));
        assertThat(counted.get(new ImpressionCounter.Key("feature1", ImpressionUtils.truncateTimeframe(timestamp))), is(equalTo(3)));
        assertThat(counted.get(new ImpressionCounter.Key("feature2", ImpressionUtils.truncateTimeframe(timestamp))), is(equalTo(4)));
        assertThat(counter.popAll().size(), is(equalTo(0)));

        final long nextHourTimestamp = makeTimestamp(2020, 9, 2, 11, 10, 12);
        counter.inc("feature1", timestamp, 1);
        counter.inc("feature1", timestamp + 1, 1);
        counter.inc("feature1", timestamp + 2, 1);
        counter.inc("feature2", timestamp + 3, 2);
        counter.inc("feature2", timestamp + 4, 2);
        counter.inc("feature1", nextHourTimestamp, 1);
        counter.inc("feature1", nextHourTimestamp + 1, 1);
        counter.inc("feature1", nextHourTimestamp + 2, 1);
        counter.inc("feature2", nextHourTimestamp + 3, 2);
        counter.inc("feature2", nextHourTimestamp + 4, 2);
        counted = counter.popAll();
        assertThat(counted.size(), is(equalTo(4)));
        assertThat(counted.get(new ImpressionCounter.Key("feature1", ImpressionUtils.truncateTimeframe(timestamp))), is(equalTo(3)));
        assertThat(counted.get(new ImpressionCounter.Key("feature2", ImpressionUtils.truncateTimeframe(timestamp))), is(equalTo(4)));
        assertThat(counted.get(new ImpressionCounter.Key("feature1", ImpressionUtils.truncateTimeframe(nextHourTimestamp))), is(equalTo(3)));
        assertThat(counted.get(new ImpressionCounter.Key("feature2", ImpressionUtils.truncateTimeframe(nextHourTimestamp))), is(equalTo(4)));
        assertThat(counter.popAll().size(), is(equalTo(0)));
    }

    @Test
    public void manyConcurrentCalls() throws InterruptedException {
        final int iterations = 10000000;
        final long timestamp =  makeTimestamp(2020, 9, 2, 10, 10, 12);
        final long nextHourTimestamp = makeTimestamp(2020, 9, 2, 11, 10, 12);
        ImpressionCounter counter = new ImpressionCounter();
        Thread t1 = new Thread(() -> {
            int times = iterations;
            while (times-- > 0) {
                counter.inc("feature1", timestamp, 1);
                counter.inc("feature2", timestamp, 1);
                counter.inc("feature1", nextHourTimestamp, 2);
                counter.inc("feature2", nextHourTimestamp, 2);
            }
        });
        Thread t2 = new Thread(() -> {
            int times = iterations;
            while (times-- > 0) {
                counter.inc("feature1", timestamp, 2);
                counter.inc("feature2", timestamp, 2);
                counter.inc("feature1", nextHourTimestamp, 1);
                counter.inc("feature2", nextHourTimestamp, 1);
            }
        });

        t1.setDaemon(true); t2.setDaemon(true);
        t1.start(); t2.start();
        t1.join(); t2.join();

        Map<ImpressionCounter.Key, Integer> counted = counter.popAll();
        assertThat(counted.size(), is(equalTo(4)));
        assertThat(counted.get(new ImpressionCounter.Key("feature1", ImpressionUtils.truncateTimeframe(timestamp))), is(equalTo(iterations * 3)));
        assertThat(counted.get(new ImpressionCounter.Key("feature2", ImpressionUtils.truncateTimeframe(timestamp))), is(equalTo(iterations * 3)));
        assertThat(counted.get(new ImpressionCounter.Key("feature1", ImpressionUtils.truncateTimeframe(nextHourTimestamp))), is(equalTo(iterations * 3)));
        assertThat(counted.get(new ImpressionCounter.Key("feature2", ImpressionUtils.truncateTimeframe(nextHourTimestamp))), is(equalTo(iterations * 3)));
    }
}
