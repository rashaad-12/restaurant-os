package com.restaurantos.analyticservice.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FieldOperator {

    EQ(ESOperator.TERM, false),
    NOT_EQ(ESOperator.TERM, true),

    CN(ESOperator.CONTAINS, false),
    NOT_CN(ESOperator.CONTAINS, true),
    PREFIX(ESOperator.PREFIX, false),
    NOT_PREFIX(ESOperator.PREFIX, true),
    SUFFIX(ESOperator.SUFFIX, false),
    NOT_SUFFIX(ESOperator.SUFFIX, true),

    NN(ESOperator.EXISTS, false),
    NU(ESOperator.EXISTS, true),

    RN(ESOperator.RANGE_BETWEEN, false),
    GTE(ESOperator.RANGE_GTE, false),
    LTE(ESOperator.RANGE_LTE, false),
    GT(ESOperator.RANGE_GT, false),
    LT(ESOperator.RANGE_LT, false);

    public enum ESOperator {
        TERM, CONTAINS, PREFIX, SUFFIX, EXISTS,
        RANGE_BETWEEN, RANGE_GTE, RANGE_LTE, RANGE_GT, RANGE_LT
    }

    private final ESOperator esOperator;
    private final boolean negate;

    public ESOperator esOperator() {
        return esOperator;
    }

    public boolean isRange() {
        return esOperator.name().startsWith("RANGE");
    }
}
