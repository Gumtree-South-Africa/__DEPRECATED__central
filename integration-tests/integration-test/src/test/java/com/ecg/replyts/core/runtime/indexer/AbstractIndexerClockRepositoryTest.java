package com.ecg.replyts.core.runtime.indexer;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by pragone
 * Created on 18/10/15 at 7:06 PM
 *
 * @author Paolo Ragone <pragone@ebay.com>
 */
public abstract class AbstractIndexerClockRepositoryTest<E extends IndexerClockRepository> {
    private E clockRepository;

    @Before
    public void setUp() throws Exception {
        clockRepository = createClockRepository();
        clockRepository.clear();
    }

    protected abstract E createClockRepository() throws Exception;

    @Test
    public void storeAndRetrieveDate() {
        DateTime refDate = new DateTime();
        clockRepository.set(refDate);
        assertEquals(refDate, clockRepository.get());
    }

    @Test
    public void retrievesNullIfNoLastRunDateExists() {
        assertNull(clockRepository.get());
    }

    @Test
    public void clearingDateTimeErasesFromPersistence() {
        clockRepository.set(new DateTime());
        clockRepository.clear();
        assertEquals(null, clockRepository.get());
    }

    protected E getClockRepository() {
        return clockRepository;
    }
}
