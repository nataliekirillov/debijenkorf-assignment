package com.debijenkorf.assignment.service;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.debijenkorf.assignment.app.configuration.SourceProperties;
import com.debijenkorf.assignment.data.ImageType;
import com.debijenkorf.assignment.enums.ImageTypeEnum;
import com.debijenkorf.assignment.enums.ScaleTypeEnum;
import com.debijenkorf.assignment.strategy.S3DirectoryStrategy;
import jakarta.annotation.PostConstruct;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;

/**
 * A service responsible for the retrieval of images
 */
@Service
public class ImageService {
    private Logger log;
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

    public byte[] getImage(String type, String filename) {
        if (!isTypeSupported(type)) {
            log.info("No predefined type: " + type);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No predefined type: " + type);
        }

        return getImageFromS3(type, filename);
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
            return IOUtils.toByteArray(response.getEntity().getContent());
        } catch (IOException e) {
            log.error("Failed to get image from source");
        }

        return null;
    }

    public void flushImage(String type, String filename) {
        String s3FilePath = directoryStrategy.getDirectoryStrategy(type, filename);

        try {
            s3Service.delete(s3FilePath);
        } catch (AmazonS3Exception e) {
            log.error("Failed to delete file");
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
    public void setLog(Logger log) {
        this.log = log;
    }

    @Autowired
    public void setDirectoryStrategy(S3DirectoryStrategy directoryStrategy) {
        this.directoryStrategy = directoryStrategy;
    }
}
