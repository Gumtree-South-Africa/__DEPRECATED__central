package com.ecg.replyts.core.runtime.persistence.mail;

import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.List;

class Manifest {

    private final List<String> chunkKeys;

    Manifest(List<String> chunkKeys) {
        this.chunkKeys = ImmutableList.copyOf(chunkKeys);
    }

    public List<String> getChunkKeys() {
        return chunkKeys;
    }

    public String generate() {
        return JsonObjects.builder().attr("chunks", JsonObjects.newJsonArray(chunkKeys)).toJson();
    }

    public static Manifest parse(String json) {
        List<String> chunkKeys = Lists.newArrayList();
        ArrayNode arr = (ArrayNode) JsonObjects.parse(json).get("chunks");
        for (JsonNode node : arr) {
            chunkKeys.add(node.asText());
        }

        return new Manifest(chunkKeys);
    }

    @Override
    public int hashCode() {
        return chunkKeys.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other instanceof Manifest) {
            return chunkKeys.equals(((Manifest) other).getChunkKeys());
        }
        return false;
    }

}
