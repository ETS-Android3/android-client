package io.split.android.engine.matchers;

import com.google.common.collect.Lists;

import io.split.android.engine.matchers.strings.WhitelistMatcher;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for NegatableMatcher.
 *
 */
public class NegatableMatcherTest {

    @Test
    public void works_all_keys() {
        AllKeysMatcher delegate = new AllKeysMatcher();
        AttributeMatcher.NegatableMatcher matcher = new AttributeMatcher.NegatableMatcher(delegate, true);

        test(matcher, "foo", false);
    }

    @Test
    public void works_whitelist() {
        WhitelistMatcher delegate = new WhitelistMatcher(Lists.newArrayList("a", "b"));
        AttributeMatcher.NegatableMatcher matcher = new AttributeMatcher.NegatableMatcher(delegate, true);

        test(matcher, "a", false);
        test(matcher, "b", false);
        test(matcher, "c", true);
    }

    private void test(AttributeMatcher.NegatableMatcher negationMatcher, String key, boolean expected) {
        assertThat(negationMatcher.match(key, null, null, null), is(expected));
        assertThat(negationMatcher.delegate().match(key, null, null, null), is(!expected));

    }


}
