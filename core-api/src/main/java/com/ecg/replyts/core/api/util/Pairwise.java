package com.ecg.replyts.core.api.util;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * Utility method for writing more convenient equals methods.
 *
 * @author mhuttar
 */
public final class Pairwise {

    private Pairwise() {

    }

    /**
     * Assures that every tuple of objects[n] equals objects[n+1] where n is ever even number.
     * e.g.
     * <br/>
     * <code>Pairwise.equals(1, 1, 3, 3, 5, 5)</code> is true (1=1, 3=3, 5=5).
     * <br/>
     * <code>Pairwise.equals(1, 2)</code> is false (1 != 2)
     */
    public static boolean pairsAreEqual(Object... objects) {
        Preconditions.checkArgument(objects.length % 2 == 0);
        for (int n = 0; n < objects.length; n += 2) {
            if (!Objects.equal(objects[n], objects[n + 1])) {
                return false;
            }
        }
        return true;

    }
}
