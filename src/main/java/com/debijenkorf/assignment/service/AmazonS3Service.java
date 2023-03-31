package com.debijenkorf.assignment.service;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.debijenkorf.assignment.app.config.S3Properties;
import jakarta.annotation.PostConstruct;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

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

    public InputStream downloadFile(String filename) {
        S3Object object = client.getObject(s3Properties.getBucket(), filename);
        return object.getObjectContent();
    }

    public void uploadFile(String path, InputStream is) {
        try {
            byte[] resultByte = DigestUtils.md5(is);
            String streamMD5 = new String(Base64.encodeBase64(resultByte));
            ObjectMetadata metaData = new ObjectMetadata();
            metaData.setContentMD5(streamMD5);
            client.putObject(s3Properties.getBucket(), path, is, metaData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Autowired
    public void setS3Properties(S3Properties s3Properties) {
        this.s3Properties = s3Properties;
    }
}
