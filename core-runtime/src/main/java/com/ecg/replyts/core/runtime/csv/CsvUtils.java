package com.ecg.replyts.core.runtime.csv;

import java.util.Iterator;

public final class CsvUtils {

    private CsvUtils() {

    }

    public static String toCsv(Iterable<? extends CsvSerializable> iterable) {
        if (iterable == null) {
            return "";
        }

        Iterator<? extends CsvSerializable> elemIterator = iterable.iterator();
        StringBuilder csv = new StringBuilder();
        while (elemIterator.hasNext()) {
            CsvSerializable elem = elemIterator.next();
            if (elem != null) {
                csv.append(elem.toCsvLine());
                csv.append("\n");
            }
        }
        return csv.toString().trim();
    }
}
