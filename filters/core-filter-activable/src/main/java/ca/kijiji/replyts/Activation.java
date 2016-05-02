package ca.kijiji.replyts;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Function;

import java.util.HashSet;
import java.util.Set;

public class Activation {

    private static final String RUN_FOR_KEY = "runFor";
    private static final String CATEGORIES_KEY = "categories";
    private static final String EXCEPT_CATEGORIES_KEY = "exceptCategories";
    private static final String USERTYPE_KEY = "userType";

    private final Set<Integer> runForCategories;
    private final Set<Integer> exceptForCategories;
    private final Set<String> runForUserTypes;

    public Activation(JsonNode configuration) {
        Function<JsonNode, Integer> jsonNodeToInteger = new Function<JsonNode, Integer>() {
            @Override
            public Integer apply(JsonNode jsonNode) {
                return jsonNode == null ? -1 : jsonNode.asInt();
            }
        };

        Function<JsonNode, String> jsonNodeToString = new Function<JsonNode, String>() {
            @Override
            public String apply(JsonNode jsonNode) {
                return jsonNode == null ? "" : jsonNode.asText();
            }
        };

        JsonNode runFor = configuration.get(RUN_FOR_KEY);

        runForCategories = convertJsonArrayToSet(runFor, CATEGORIES_KEY, jsonNodeToInteger);
        exceptForCategories = convertJsonArrayToSet(runFor, EXCEPT_CATEGORIES_KEY, jsonNodeToInteger);
        runForUserTypes = convertJsonArrayToSet(runFor, USERTYPE_KEY, jsonNodeToString);
    }

    private <T> Set<T> convertJsonArrayToSet(JsonNode runFor, String configKey, Function<JsonNode, T> transformer) {
        Set<T> set = new HashSet<>();

        if (runFor == null) {
            return set;
        }

        JsonNode array = runFor.get(configKey);

        if (array == null) {
            return set;
        }

        if (!array.isArray()) {
            throw new IllegalArgumentException("input node is not an array: " + array);
        }

        for (JsonNode category : array) {
            set.add(transformer.apply(category));
        }
        return set;
    }

    public Set<Integer> getRunForCategories() {
        return runForCategories;
    }

    public Set<Integer> getExceptForCategories() {
        return exceptForCategories;
    }

    public Set<String> getRunForUserTypes() {
        return runForUserTypes;
    }
}
