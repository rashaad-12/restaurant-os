package com.restaurantos.elasticservice.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ChangeEventOperation {

    @JsonProperty("c")
    CREATE,

    @JsonProperty("u")
    UPDATE,

    @JsonProperty("d")
    DELETE,

    // Debezium snapshot read: an existing row captured during the initial snapshot. Treated as an
    // upsert so pre-existing orders are backfilled into the index.
    @JsonProperty("r")
    READ
}
