package com.ecg.replyts.core.api.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author mhuttar
 */
public class PairwiseTest {

    @Test(expected = IllegalArgumentException.class)
    public void rejectsOddNumOfParams() throws Exception {
        Pairwise.pairsAreEqual(1, 2, 3);
    }

    @Test
    public void equalsIfPairsAreEqual() throws Exception {
        Assert.assertTrue(Pairwise.pairsAreEqual(1, 1, 2, 2, 5, 5, 8, 8));
    }

    @Test
    public void doesNotEqualIfLastTwoAreDifferent() throws Exception {
        Assert.assertFalse(Pairwise.pairsAreEqual(1, 1, 2, 2, 5, 5, 8, 9));
    }

    @Test
    public void doesNotEqualIfMiddleTwoAreDifferent() throws Exception {
        Assert.assertFalse(Pairwise.pairsAreEqual(1, 1, 2, 10, 5, 5, 8, 8));
    }

    @Test
    public void doesNotEqualIfFirstTwoAreDifferent() throws Exception {
        Assert.assertFalse(Pairwise.pairsAreEqual(1, 10, 2, 2, 5, 5, 8, 8));
    }


}
