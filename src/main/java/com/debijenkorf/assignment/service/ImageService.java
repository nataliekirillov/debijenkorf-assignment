package com.debijenkorf.assignment.service;

import com.debijenkorf.assignment.app.configuration.SourceProperties;
import com.debijenkorf.assignment.strategy.S3DirectoryStrategy;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

/**
 * A service responsible for the retrieval of images
 */
@Service
public class ImageService {
    private Logger log;
    private S3Service s3Service;
    private SourceProperties sourceProperties;
    private S3DirectoryStrategy directoryStrategy;
    private final CloseableHttpClient httpClient = HttpClients.createDefault();



    public byte[] getImage(String type, String filename) {
        return null;
    }

    /**
     * Get image from S3
     *
     * @return byte[] representing image file from S3
     */
    public byte[] getImageFromS3() {
        InputStream is = s3Service.download("abcd.jpg");
        try {
            return IOUtils.toByteArray(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get image from source
     *
     * @return byte[] representing image file from source
     */
    public byte[] getImageFromSource() {
        HttpGet request = new HttpGet(sourceProperties.getRootUrl());

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            return IOUtils.toByteArray(response.getEntity().getContent());
        } catch (IOException e) {
            log.error("Failed to get image from source");
        }

        return null;
    }

    public void flushImage(String type, String filename) {
        String s3FilePath = directoryStrategy.getDirectoryStrategy(type, filename);
        s3Service.delete(s3FilePath);
    }

    @Autowired
    public void setSourceProperties(SourceProperties sourceProperties) {
        this.sourceProperties = sourceProperties;
    }

    @Autowired
    public void setS3Service(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @Autowired
    public void setLog(Logger log) {
        this.log = log;
    }

    @Autowired
    public void setDirectoryStrategy(S3DirectoryStrategy directoryStrategy) {
        this.directoryStrategy = directoryStrategy;
    }
}
