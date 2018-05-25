package com.ecg.gumtree.comaas.common.gumshield;

/**
 * Known Good DTO
 */
public final class KnownGoodResponse {

    private Long id;

    private KnownGoodStatus status;

    /**
     * Constructor
     */
    public KnownGoodResponse() {
        this(null, null);
    }

    /**
     * @param id the advert this response refers to
     * @param status the known good status
     */
    public KnownGoodResponse(Long id, KnownGoodStatus status) {
        this.id = id;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long v) {
        id = v;
    }

    public KnownGoodStatus getStatus() {
        return status;
    }

    public void setStatus(KnownGoodStatus v) {
        status = v;
    }
}
