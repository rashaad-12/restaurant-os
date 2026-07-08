package com.restaurantos.elasticservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.restaurantos.elasticservice.enums.ChangeEventOperation;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeEvent {

    private ChangeRecord before;
    private ChangeRecord after;

    @JsonProperty("op")
    private ChangeEventOperation operation;
}
