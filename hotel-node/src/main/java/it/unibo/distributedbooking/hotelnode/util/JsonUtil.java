package it.unibo.distributedbooking.hotelnode.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;

public final class JsonUtil {

    private static final ObjectMapper MAPPER = createMapper();

    private JsonUtil() {
    }

    private static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    public static <T> T fromJson(final InputStream input, final Class<T> clazz) throws IOException {
        return MAPPER.readValue(input, clazz);
    }

    public static <T> T fromJson(final String json, final Class<T> clazz) throws JsonProcessingException {
        return MAPPER.readValue(json, clazz);
    }

    public static byte[] toJsonBytes(final Object value) throws JsonProcessingException {
        return MAPPER.writeValueAsBytes(value);
    }

    public static String toJson(final Object value) throws JsonProcessingException {
        return MAPPER.writeValueAsString(value);
    }
}