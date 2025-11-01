package com.restaurantos.coresecurity.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AuthType {

    INTERNAL("Internal"),

    CUSTOMER("Customer"),

    OTP("OTP");

    private final String value;

}
