package com.restaurantos.common.util;

import lombok.Setter;

import java.time.LocalDateTime;

import static java.util.Objects.nonNull;

public class DateUtil {

    @Setter
    private static LocalDateTime fixedDateTime = null;

    public static LocalDateTime getCurrentDateTime() {
        return nonNull(fixedDateTime) ? fixedDateTime : LocalDateTime.now();
    }

    public static void clearFixedDateTime() {
        fixedDateTime = null;
    }
}

