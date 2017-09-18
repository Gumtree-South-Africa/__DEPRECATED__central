package com.ecg.replyts.core.webapi.control.health;

import com.datastax.driver.core.*;
import com.google.common.reflect.TypeToken;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.*;

public class StubRow implements Row {

    private static final String UNSUPPORTED_METHOD = "Unsupported method of Cassandra Row Stub";

    private final Map<String, String> data;

    static Row clusterRow(String peer, String dataCenter, String hostId, String rack, String releaseVersion, String schemaVersion) {
        Map<String, String> data = new HashMap<>();
        data.put("peer", peer);
        data.put("data_center", dataCenter);
        data.put("host_id", hostId);
        data.put("rack", rack);
        data.put("release_version", releaseVersion);
        data.put("schema_version", schemaVersion);

        return new StubRow(data);
    }

    StubRow(Map<String, String> data) {
        this.data = data;
    }

    @Override
    public String getString(String name) {
        return data.get(name);
    }

    @Override
    public ColumnDefinitions getColumnDefinitions() {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public boolean isNull(int i) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public boolean isNull(String name) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public boolean getBool(int i) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public boolean getBool(String name) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public int getInt(int i) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public int getInt(String name) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public long getLong(int i) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public long getLong(String name) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public Date getDate(int i) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public Date getDate(String name) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public float getFloat(int i) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public float getFloat(String name) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public double getDouble(int i) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public double getDouble(String name) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public ByteBuffer getBytesUnsafe(int i) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public ByteBuffer getBytesUnsafe(String name) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public ByteBuffer getBytes(int i) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public ByteBuffer getBytes(String name) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public String getString(int i) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public BigInteger getVarint(int i) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public BigInteger getVarint(String name) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public BigDecimal getDecimal(int i) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public BigDecimal getDecimal(String name) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public UUID getUUID(int i) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public UUID getUUID(String name) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public InetAddress getInet(int i) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public InetAddress getInet(String name) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public Token getToken(int i) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public Token getToken(String name) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public Token getPartitionKeyToken() {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public <T> List<T> getList(int i, Class<T> elementsClass) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public <T> List<T> getList(int i, TypeToken<T> elementsType) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public <T> List<T> getList(String name, Class<T> elementsClass) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public <T> List<T> getList(String name, TypeToken<T> elementsType) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public <T> Set<T> getSet(int i, Class<T> elementsClass) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public <T> Set<T> getSet(int i, TypeToken<T> elementsType) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public <T> Set<T> getSet(String name, Class<T> elementsClass) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public <T> Set<T> getSet(String name, TypeToken<T> elementsType) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public <K, V> Map<K, V> getMap(int i, Class<K> keysClass, Class<V> valuesClass) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public <K, V> Map<K, V> getMap(int i, TypeToken<K> keysType, TypeToken<V> valuesType) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public UDTValue getUDTValue(int i) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public TupleValue getTupleValue(int i) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public Object getObject(int i) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public <K, V> Map<K, V> getMap(String name, Class<K> keysClass, Class<V> valuesClass) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public <K, V> Map<K, V> getMap(String name, TypeToken<K> keysType, TypeToken<V> valuesType) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public UDTValue getUDTValue(String name) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public TupleValue getTupleValue(String name) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    @Override
    public Object getObject(String name) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }
}