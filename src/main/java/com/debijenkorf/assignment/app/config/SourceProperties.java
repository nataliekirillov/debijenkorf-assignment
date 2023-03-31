package com.debijenkorf.assignment.app.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationProperties()
@ConfigurationPropertiesScan
@Getter
public class SourceProperties {
    @Value("${source.root.url}")
    private String rootUrl;
}
