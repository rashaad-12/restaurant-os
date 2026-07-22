package com.restaurantos.analyticservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Component
public class IndexResolver {

    private final String prefix;

    public IndexResolver(@Value("${elasticsearch.prefix}") String prefix) {
        this.prefix = prefix;
    }

    public String forOrg(String orgCode) {
        if (isBlank(orgCode)) throw new IllegalArgumentException("orgId is required");
        return (prefix + orgCode.trim()).toLowerCase();
    }
}
