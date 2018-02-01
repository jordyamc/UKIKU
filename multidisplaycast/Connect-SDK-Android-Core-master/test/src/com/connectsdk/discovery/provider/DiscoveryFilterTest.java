package com.connectsdk.discovery.provider;

import com.connectsdk.discovery.DiscoveryFilter;

import junit.framework.Assert;

import org.junit.Test;

/**
 * Created by oleksii.frolov on 2/12/2015.
 */
public class DiscoveryFilterTest {
    
    @Test
    public void testEqualsWithNullObject() {
        DiscoveryFilter filter = new DiscoveryFilter("DLNA", "filter");
        Assert.assertFalse(filter.equals(null));
    }

    @Test
    public void testEqualsWithWrongObject() {
        DiscoveryFilter filter = new DiscoveryFilter("DLNA", "filter");
        Assert.assertFalse(filter.equals(new Object()));
    }
}
