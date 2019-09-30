package io.split.android.engine.matchers;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import io.split.android.engine.matchers.AllKeysMatcher;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for AllKeysMatcher
 */
public class AllKeysMatcherTest {

    @SuppressWarnings("ConstantConditions")
    @Test
    public void works() {
        AllKeysMatcher matcher = new AllKeysMatcher();
        assertThat(matcher.match(null, null, null, null), is(false));
        for (int i = 0; i < 100; i++) {
            String randomKey = RandomStringUtils.random(10);
            assertThat(matcher.match(randomKey, null, null, null), is(true));
        }

    }

}
