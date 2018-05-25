package com.ecg.comaas.gtuk.filter.geoiplookup;

import au.com.bytecode.opencsv.CSVReader;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.List;

/**
 * MaxMind GeoIP lookup service utilises a csv file of IP ranges/countries
 *
 * Code originally copied from Entity API
 */
public class MaxMindGeoIpService implements GeoIpService {

    private static Logger logger = org.slf4j.LoggerFactory.getLogger(MaxMindGeoIpService.class);

    private RangeTreeNode tree = null;

    /**
     * Create lookup serivce
     *
     * @param file Location of MaxMind geo-ip database
     */
    public MaxMindGeoIpService(Resource file) {
        logger.debug("loading data from: " + file.getDescription());
        try {
            tree = loadData(new FileReader(file.getFile()));
        } catch (IOException e) {
            throw new RuntimeException("cannot load maxmind data from file: " + file.getDescription());
        }
    }

    /**
     * Create lookup service
     *
     * @param data MaxMind geo-ip CSV database input stream
     */
    public MaxMindGeoIpService(Reader data) {
        logger.debug("loading data from reader");
        tree = loadData(data);
    }

    @Override
    public final String getCountryCode(String ip) {

        long address = 0L;

        try {
            address = addressToLong(ip);
        } catch (Exception e) {
            logger.error("cannot parse ip address", e);
            return "";
        }

        MaxMindGeoIpEntry entry = tree.find(address);
        if (entry == null) {
            return "";
        } else {
            return entry.getCode();
        }
    }

    private long addressToLong(String ip) {
        if (ip == null) {
            throw new RuntimeException("cannot parse null as dotted quad");
        }
        String[] quads = ip.split("\\.");
        if (quads.length != 4) {
            throw new RuntimeException("cannot parse string as dotted quad: " + ip);
        }

        long n = 0L;

        n += Long.parseLong(quads[0]);
        n <<= 8;
        n += Long.parseLong(quads[1]);
        n <<= 8;
        n += Long.parseLong(quads[2]);
        n <<= 8;
        n += Long.parseLong(quads[3]);

        return n;
    }

    private RangeTreeNode loadData(Reader input) {
        RangeTreeNode node = null;

        try {
            CSVReader reader = new CSVReader(input, ',', '"');

            List<String[]> entries = reader.readAll();

            Collections.shuffle(entries);

            for (String[] line : entries) {
                logger.debug("min: " + line[2] + " - max: " + line[3] + " - code: " + line[4]);

                MaxMindGeoIpEntry entry = new MaxMindGeoIpEntry(Long.valueOf(line[2]), Long.valueOf(line[3]), line[4]);
                if (node == null) {
                    node = new RangeTreeNode(entry);
                } else {
                    node.add(entry);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("cannot load maxmind geoip data", e);
        }
        return node;
    }

    /**
     * Numeric ip address to geographic region entry
     */
    final class MaxMindGeoIpEntry {

        private final long min;
        private final long max;
        private final String code;

        /**
         * Create entry
         *
         * @param min  Lower bound of ip address as integer
         * @param max  Upper bound of ip address as integer
         * @param code Country code
         */
        public MaxMindGeoIpEntry(long min, long max, String code) {
            assert (min <= max);
            assert (code != null);
            this.min = min;
            this.max = max;
            this.code = code;
        }

        public boolean contains(long value) {
            return value >= min && value <= max;
        }

        public long getMin() {
            return min;
        }

        public long getMax() {
            return max;
        }

        public String getCode() {
            return code;
        }
    }

    /**
     * Node in binary tree whose nodes contain a range instead of a single value
     */
    final class RangeTreeNode {
        private final MaxMindGeoIpEntry value;
        private RangeTreeNode left;
        private RangeTreeNode right;

        /**
         * Create node
         *
         * @param value Value of node
         */
        public RangeTreeNode(final MaxMindGeoIpEntry value) {
            assert (value != null);
            this.value = value;
            this.left = null;
            this.right = null;
        }

        public MaxMindGeoIpEntry getValue() {
            return value;
        }

        public void add(MaxMindGeoIpEntry value) {
            RangeTreeNode child = new RangeTreeNode(value);
            add(child);
        }

        public void add(RangeTreeNode child) {
            if (child.getValue().getMin() > getValue().getMax()) {
                if (right == null) {
                    right = child;
                } else {
                    right.add(child);
                }
            } else if (child.getValue().getMax() < getValue().getMin()) {
                if (left == null) {
                    left = child;
                } else {
                    left.add(child);
                }
            } else {
                throw new RuntimeException("maxmind geo-ip data contains an overlapping range");
            }
        }

        public MaxMindGeoIpEntry find(long value) {
            if (getValue().contains(value)) {
                return getValue();
            } else if (value < getValue().getMin()) {
                if (left == null) {
                    return null;
                } else {
                    return left.find(value);
                }
            } else {
                if (right == null) {
                    return null;
                } else {
                    return right.find(value);
                }
            }
        }
    }
}