package com.debijenkorf.assignment.service;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.debijenkorf.assignment.app.config.S3Properties;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AmazonS3Service {
    private S3Properties s3Properties;
    private AmazonS3 client;

    @PostConstruct
    public void postConstruct() {
        AWSCredentials credentials = new BasicAWSCredentials(s3Properties.getAccessKey(), s3Properties.getSecretKey());
        this.client = AmazonS3ClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(s3Properties.getRegion())
                .build();
    }

    @Autowired
    public void setS3Properties(S3Properties s3Properties) {
        this.s3Properties = s3Properties;
    }
}
