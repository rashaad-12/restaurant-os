package com.restaurantos.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static java.util.Objects.isNull;

public class JsonUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);;

    public static <T> T readFromFile(String filePath, TypeReference<T> typeReference) {
        try (InputStream inputStream = JsonUtil.class.getClassLoader().getResourceAsStream(filePath)) {
            if (isNull(inputStream)) {
                throw new FileNotFoundException("Resource not found: " + filePath);
            }
            return objectMapper.readValue(inputStream, typeReference);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T readFromFile(String filePath, Class<T> clazz) {
        try (InputStream inputStream = JsonUtil.class.getClassLoader().getResourceAsStream(filePath)) {
            if (isNull(inputStream)) {
                throw new FileNotFoundException("Resource not found: " + filePath);
            }
            return objectMapper.readValue(inputStream, clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String writeValueAsString(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("Unable to write object as JSON", e);
        }
    }
}
