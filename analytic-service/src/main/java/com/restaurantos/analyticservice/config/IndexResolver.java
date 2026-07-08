package com.restaurantos.analyticservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Component
public class IndexResolver {

    @Value("${elasticsearch.prefix}")
    private final String prefix;

    public IndexResolver( String prefix) {
        this.prefix = prefix;
    }

    public String forOrg(String orgCode) {
        if (isBlank(orgCode)) throw new IllegalArgumentException("orgId is required");
        return (prefix + orgCode.trim()).toLowerCase();
    }
}
