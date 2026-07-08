package com.restaurantos.analyticservice.query;

import com.restaurantos.analyticservice.enums.FieldType;

import java.util.Map;
import java.util.Set;

import static java.util.Objects.isNull;

public class FieldTypes {

    private final Map<String, FieldType> types;
    private final Set<String> nestedPaths;

    public FieldTypes(Map<String, FieldType> types, Set<String> nestedPaths) {
        this.types = types;
        this.nestedPaths = nestedPaths;
    }

    public FieldType typeOf(String field) {
        return types.getOrDefault(field, FieldType.KEYWORD);
    }


    public String nestedPath(String field) {
        if (isNull(field)) return null;

        int dot = field.indexOf('.');
        if (dot < 0) return null;

        String root = field.substring(0, dot);
        return nestedPaths.contains(root) ? root : null;
    }
}
