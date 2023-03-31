package com.debijenkorf.assignment.app.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationProperties
@ConfigurationPropertiesScan
@Getter
public class S3Properties {
    @Value("${aws.endpoint}")
    private String endpoint;

    @Value("${aws.accesskey}")
    private String accessKey;

    @Value("${aws.secretkey}")
    private String secretKey;

    @Value("${aws.region}")
    private String region;

    @Value("${aws.bucket}")
    private String bucket;
}
