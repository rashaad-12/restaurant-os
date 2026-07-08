package com.restaurantos.analyticservice.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

@Getter
@Setter
public class Condition {

    private ConditionType conditionType;
    private List<Condition> conditions;

    private String field;
    private FieldOperator operator;
    private List<String> values;
    private ConditionType valueCondition;

    public boolean isGroup() {
        return isNotEmpty(conditions);
    }
}
