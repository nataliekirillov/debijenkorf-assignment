package com.debijenkorf.assignment.service;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.debijenkorf.assignment.app.configuration.SourceProperties;
import com.debijenkorf.assignment.data.ImageType;
import com.debijenkorf.assignment.enums.ImageTypeEnum;
import com.debijenkorf.assignment.enums.ScaleTypeEnum;
import com.debijenkorf.assignment.strategy.S3DirectoryStrategy;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;


/**
 * A service responsible for the retrieval of images
 */
@Service
@Slf4j
public class ImageService {
    private DbLogger dbLog;
    private S3Service s3Service;
    private SourceProperties sourceProperties;
    private S3DirectoryStrategy directoryStrategy;
    private List<ImageType> supportedImageTypes;
    private final CloseableHttpClient httpClient = HttpClients.createDefault();

    @PostConstruct
    public void postConstruct() {
        this.supportedImageTypes = List.of(
                new ImageType("Thumbnail", 5, 5, 90, "#FFFFFF", ImageTypeEnum.JPG, ScaleTypeEnum.FILL),
                new ImageType("Original", 0, 0, 100, "#FFFFFF", ImageTypeEnum.JPG, ScaleTypeEnum.FILL)
        );
    }

    /**
     * Return an image to the user
     *
     * @param type     Definition type
     * @param filename File path
     * @return Requested image
     */
    public byte[] getImage(String type, String filename) {
        if (!isTypeSupported(type)) {
            String msg = "No predefined type: " + type;
            dbLog.info(msg);
            log.info(msg);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, msg);
        }

        return getImageFromSource();
    }

    /**
     * Get image from S3
     *
     * @return byte[] representing image file from S3
     */
    public byte[] getImageFromS3(String type, String filename) {
        String s3FilePath = directoryStrategy.getDirectoryStrategy(type, filename);
        try {
            InputStream is = s3Service.download(s3FilePath);
            return IOUtils.toByteArray(is);
        } catch (AmazonS3Exception e) {
            throw new RuntimeException(e);
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
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 404 || statusCode > 500) {
                dbLog.error("Source URL responded with: " + statusCode);
                log.error("Source URL responded with: {}", statusCode);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to get image from source");
            }
            return IOUtils.toByteArray(response.getEntity().getContent());
        } catch (IOException e) {
            dbLog.info("Failed to get image from source");
            log.info("Failed to get image from source");
        }
        return null;
    }

    /**
     * Flush an image from S3
     *
     * @param type     Definition type
     * @param filename File path
     */
    public void flushImage(String type, String filename) {
        String s3FilePath = directoryStrategy.getDirectoryStrategy(type, filename);

        try {
            s3Service.delete(s3FilePath);
        } catch (AmazonS3Exception e) {
            String msg = "Failed to delete file";
            dbLog.error(msg);
            log.error(msg + ": {}", e.getMessage());
            log.debug(msg, e.getMessage());
        }
    }

    private boolean isTypeSupported(String type) {
        return supportedImageTypes.stream().anyMatch(x -> x.getName().equalsIgnoreCase(type));
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
    public void setDbLog(DbLogger dbLog) {
        this.dbLog = dbLog;
    }

    @Autowired
    public void setDirectoryStrategy(S3DirectoryStrategy directoryStrategy) {
        this.directoryStrategy = directoryStrategy;
    }
}
