package com.debijenkorf.assignment.app.configuration;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationProperties()
@ConfigurationPropertiesScan
@Getter
public class LogDBProperties {
    @Value("${logdb.endpoint}")
    private String endpoint;

    @Value("${logdb.name}")
    private String name;

    @Value("${logdb.username}")
    private String username;

    @Value("${logdb.password}")
    private String password;
}
