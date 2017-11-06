package io.split.android.engine.matchers;

import io.split.android.engine.matchers.LessThanOrEqualToMatcher;
import io.split.android.client.dtos.DataType;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for AllKeysMatcher
 */
public class LessThanOrEqualToMatcherTest {

    @Test
    public void works() {
        LessThanOrEqualToMatcher matcher = new LessThanOrEqualToMatcher(10, DataType.NUMBER);
        assertThat(matcher.match(null, null, null, null), is(false));
        assertThat(matcher.match(1, null, null, null), is(true));
        assertThat(matcher.match(new Long(-1), null, null, null), is(true));
        assertThat(matcher.match(9, null, null, null), is(true));
        assertThat(matcher.match(new Long(10), null, null, null), is(true));
        assertThat(matcher.match(11, null, null, null), is(false));
        assertThat(matcher.match(100, null, null, null), is(false));
    }

    @Test
    public void works_negative() {
        LessThanOrEqualToMatcher matcher = new LessThanOrEqualToMatcher(-10, DataType.NUMBER);
        assertThat(matcher.match(null, null, null, null), is(false));
        assertThat(matcher.match(1, null, null, null), is(false));
        assertThat(matcher.match(new Long(-1), null, null, null), is(false));
        assertThat(matcher.match(9, null, null, null), is(false));
        assertThat(matcher.match(new Long(10), null, null, null), is(false));
        assertThat(matcher.match(11, null, null, null), is(false));
        assertThat(matcher.match(-9, null, null, null), is(false));
        assertThat(matcher.match(-10, null, null, null), is(true));
        assertThat(matcher.match(-11, null, null, null), is(true));
    }

    @Test
    public void works_dates() {
        long april11_2016_23_59 = 1460419199000L;
        long april12_2016_midnight_19 = 1460420360000L;
        long april12_2016_midnight_20 = 1460420421903L;
        long april12_2016_midnight_20_59 = 1460420459000L;
        long april12_2016_1_20 = 1460424039000L;

        LessThanOrEqualToMatcher matcher = new LessThanOrEqualToMatcher(april12_2016_midnight_20, DataType.DATETIME);
        assertThat(matcher.match(april11_2016_23_59, null, null, null), is(true));
        assertThat(matcher.match(april12_2016_midnight_19, null, null, null), is(true));
        assertThat(matcher.match(april12_2016_midnight_20, null, null, null), is(true));
        assertThat(matcher.match(april12_2016_midnight_20_59, null, null, null), is(true));
        assertThat(matcher.match(april12_2016_1_20, null, null, null), is(false));
    }


}
