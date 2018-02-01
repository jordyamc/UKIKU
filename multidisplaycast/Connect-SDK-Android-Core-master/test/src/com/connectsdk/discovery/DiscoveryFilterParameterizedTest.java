package com.connectsdk.discovery;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

/**
 * Created by oleksii.frolov on 2/12/2015.
 */
@RunWith(Parameterized.class)
public class DiscoveryFilterParameterizedTest {

    private final String mIdA;
    private final String mFilterA;
    private final String mIdB;
    private final String mFilterB;
    private final Boolean mResult;
    
    @Parameterized.Parameters
    public static Collection<Object[]> configs() {
        return Arrays.asList(new Object[][] {
                {"id", "filter", "id", "filter", Boolean.TRUE},
                {"id", "filter", "id", "another", Boolean.FALSE},
                {"id", "filter", "another", "filter", Boolean.FALSE},
                {null, "filter", null, "filter", Boolean.TRUE},
                {"id", null, "id", null, Boolean.TRUE},
                {null, null, null, null, Boolean.TRUE},
                {"id", "filter", null, "filter", Boolean.FALSE},
        });         
    }
    
    public DiscoveryFilterParameterizedTest(String idA, String filterA, String idB, String filterB, Boolean result) {
        this.mIdA = idA;
        this.mFilterA = filterA;
        this.mIdB = idB;
        this.mFilterB = filterB;
        this.mResult = result;
    }
    
    @Test
    public void testEquals() {
        DiscoveryFilter filterA = new DiscoveryFilter(mIdA, mFilterA);
        DiscoveryFilter filterB = new DiscoveryFilter(mIdB, mFilterB);
        Assert.assertEquals(mResult.booleanValue(), filterA.equals(filterB));
        Assert.assertEquals(mResult.booleanValue(), filterB.equals(filterA));
    }

    @Test
    public void testHashCode() {
        DiscoveryFilter filterA = new DiscoveryFilter(mIdA, mFilterA);
        DiscoveryFilter filterB = new DiscoveryFilter(mIdB, mFilterB);
        Assert.assertEquals(mResult.booleanValue(), filterA.hashCode() == filterB.hashCode());
    }
}
