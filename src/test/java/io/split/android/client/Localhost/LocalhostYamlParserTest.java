package io.split.android.client.Localhost;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import java.util.Map;

import io.split.android.client.dtos.Split;

public class LocalhostYamlParserTest {

    LocalhostFileParser parser;

    @Before
    public void setup() {
        parser = new LocalhostYamlFileParser(new ResourcesFileStorage());
    }

    @Test
    public void testCorrectFormat() {

        Map<String, Split> splits = parser.parse("splits.yaml");

        Assert.assertEquals(8, splits.size());

        Split split0 = splits.get("split_0");
        Split split1 = splits.get("split_1");
        Split split2 = splits.get("split_2");
        Split myFeature = splits.get("my_feature");
        Split otherFeature3 = splits.get("other_feature_3");
        Split xFeature = splits.get("x_feature");
        Split otherFeature = splits.get("other_feature");
        Split otherFeature2 = splits.get("other_feature_2");

        Assert.assertNotNull(split0);
        Assert.assertNotNull(split1);
        Assert.assertNotNull(split2);
        Assert.assertNotNull(myFeature);
        Assert.assertNotNull(otherFeature3);
        Assert.assertNotNull(xFeature);
        Assert.assertNotNull(otherFeature);
        Assert.assertNotNull(otherFeature2);

        Assert.assertEquals("split_0", split0.name);
        Assert.assertEquals("off", split0.defaultTreatment);
        Assert.assertNotNull(split0.configurations);
        Assert.assertEquals("{ \"size\" : 20 }", split0.configurations.get("off"));

        Assert.assertEquals("split_1", split1.name);
        Assert.assertEquals("on", split1.defaultTreatment);
        Assert.assertNull(split1.configurations);

        Assert.assertEquals("split_2", split2.name);
        Assert.assertEquals("off", split2.defaultTreatment);
        Assert.assertNotNull(split2.configurations);
        Assert.assertEquals("{ \"size\" : 20 }", split2.configurations.get("off"));

        Assert.assertEquals("my_feature", myFeature.name);
        Assert.assertEquals("on", myFeature.defaultTreatment);
        Assert.assertNotNull(myFeature.configurations);
        Assert.assertEquals("{\"desc\" : \"this applies only to ON treatment\"}", myFeature.configurations.get("on"));

        Assert.assertEquals("other_feature_3", otherFeature3.name);
        Assert.assertEquals("off", otherFeature3.defaultTreatment);
        Assert.assertNull(otherFeature3.configurations);

        Assert.assertEquals("x_feature", xFeature.name);
        Assert.assertEquals("off", xFeature.defaultTreatment);
        Assert.assertNotNull(xFeature.configurations);
        Assert.assertEquals("{\"desc\" : \"this applies only to OFF and only for only_key. The rest will receive ON\"}", xFeature.configurations.get("off"));

        Assert.assertEquals("other_feature", otherFeature.name);
        Assert.assertEquals("on", otherFeature.defaultTreatment);
        Assert.assertNull(otherFeature.configurations);

        Assert.assertEquals("other_feature_2", otherFeature2.name);
        Assert.assertEquals("on", otherFeature2.defaultTreatment);
        Assert.assertNull(otherFeature2.configurations);

    }

    @Test
    public void testWrongYamlSyntax() {
        Map<String, Split> splits = parser.parse("splits_no_yaml.yaml");
        Assert.assertNull(splits);
    }

    @Test
    public void testMissingTreatment() {
        Map<String, Split> splits = parser.parse("splits_missing_treatment.yaml");
        Assert.assertNull(splits);

    }

    @Test
    public void testMissingNameInFirstSplit() {
        Map<String, Split> splits = parser.parse("splits_missing_name.yaml");
        Assert.assertNull(splits);
    }

}
