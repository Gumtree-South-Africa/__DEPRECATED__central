package com.ecg.messagecenter.pushmessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author mdarapour
 */
public class MdsAdImageLookup extends AdImageLookup {
    private static final Logger LOG = LoggerFactory.getLogger(MdsAdImageLookup.class);

    @Override
    public String lookupAdImageUrl(Long adId) {
        LOG.warn("MDS Ad Image Lookup is not implemented yet!");
        return "http://i.ebayimg.com/dummy";
    }
}
