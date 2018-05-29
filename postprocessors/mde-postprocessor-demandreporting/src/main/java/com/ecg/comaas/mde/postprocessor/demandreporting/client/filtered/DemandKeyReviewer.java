package com.ecg.comaas.mde.postprocessor.demandreporting.client.filtered;

/**
 * This should be implemented by something which knows for which publisher-keys the demand should be included in the
 * result or not. For example the implementation checks if the publisher key relates to an ApiUser and checks the
 * APIUsers settings.
 */
public interface DemandKeyReviewer {

    /**
     * Should demand from a publisher with this key be included in the result?
     * 
     * @param publisherKey The key of the publisher to check (e.g. "MESA-vdf_camao" or "mob-iPhone").
     * @return <code>true</code> if it should be included (counted), <code>false</code> if not.
     */
    public boolean includeDemandFrom(String publisherKey);

}
