package com.debijenkorf.assignment.service;

import com.debijenkorf.assignment.app.config.SourceProperties;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.ClientProtocolException;
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
    private final CloseableHttpClient httpClient = HttpClients.createDefault();
    private SourceProperties sourceProperties;
    private AmazonS3Service s3Service;
    private LogService log;

    public byte[] getImage() {
        return null;
    }

    /**
     * Get image from S3
     *
     * @return byte[] representing image file from S3
     */
    public byte[] getImageFromS3() {
        InputStream is = s3Service.downloadFile("abcd.jpg");
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
        } catch (ClientProtocolException | IOException e) {
            log.error("Failed to get image from source");
        }

        return null;
    }

    @Autowired
    public void setSourceProperties(SourceProperties sourceProperties) {
        this.sourceProperties = sourceProperties;
    }

    @Autowired
    public void setS3Service(AmazonS3Service s3Service) {
        this.s3Service = s3Service;
    }

    @Autowired
    public void setLog(LogService log) {
        this.log = log;
    }
}
