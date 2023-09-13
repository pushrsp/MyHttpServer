package me.project.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;

import java.io.IOException;

public class Json {

    private static ObjectMapper mapper = defaultObjectMapper();

    private static ObjectMapper defaultObjectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return om;
    }

    public static JsonNode parse(String source) throws IOException {
        return mapper.readTree(source);
    }

    public static <T> T fromJson(JsonNode node, Class<T> clazz) throws JsonProcessingException {
        return mapper.treeToValue(node, clazz);
    }

    public static JsonNode toJson(Object o) {
        return mapper.valueToTree(o);
    }

    public static String stringify(JsonNode node) throws JsonProcessingException {
        return stringify(node, false);
    }

    public static String stringify(JsonNode node, boolean pretty) throws JsonProcessingException {
        return generateJson(node, pretty);
    }

    private static String generateJson(Object o, boolean pretty) throws JsonProcessingException {
        ObjectWriter objectWriter = mapper.writer();
        if(pretty)
            objectWriter = objectWriter.with(SerializationFeature.INDENT_OUTPUT);

        return objectWriter.writeValueAsString(o);
    }
}
