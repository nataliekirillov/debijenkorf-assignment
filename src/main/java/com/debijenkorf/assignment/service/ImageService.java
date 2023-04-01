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

import java.io.ByteArrayInputStream;
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

        // found image in S3 - return it
        byte[] image = getImageFromS3(type, filename);
        if (image.length != 0) {
            return image;
        }

        // didn't find image in source - return error
        image = getImageFromSource(filename);
        if (image.length == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found on source");
        }

        // found image in source - store it
        String s3Filepath = directoryStrategy.getDirectoryStrategy(type, filename);
        try {
            s3Service.upload(s3Filepath, new ByteArrayInputStream(image));
        } catch (IOException e) {
            dbLog.error("Failed to save image to S3");
            log.error("Failed to save image to S3: {}", e.getMessage());
            log.debug("Failed to save image to S3", e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to store image on S3");
        }

        return getImage(type, filename);
    }

    /**
     * Get image from S3
     *
     * @return byte[] representing image file from S3
     */
    public byte[] getImageFromS3(String type, String filename) {
        String s3Filepath = directoryStrategy.getDirectoryStrategy(type, filename);
        try {
            InputStream is = s3Service.download(s3Filepath);
            return IOUtils.toByteArray(is);
        } catch (AmazonS3Exception e) {
            log.info("File not found in S3");
            dbLog.info("File not found in S3");
        } catch (IOException e) {
            log.error("Failed to get file from S3: {}", e.getMessage());
            log.debug("Failed to get file from S3", e);
            dbLog.error("Failed to get file from S3");
        }

        return new byte[0];
    }

    /**
     * Get image from source
     *
     * @return byte[] representing image file from source
     */
    public byte[] getImageFromSource(String filename) {
        HttpGet request = new HttpGet(String.join("/", sourceProperties.getRootUrl(), filename));

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 404 || statusCode > 500) {
                dbLog.error("Source URL responded with: " + statusCode);
                log.error("Source URL responded with: {}", statusCode);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Source server error: failed to get image from source");
            }
            return response.getEntity().getContent().readAllBytes();
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
        String s3Filepath = directoryStrategy.getDirectoryStrategy(type, filename);

        try {
            s3Service.delete(s3Filepath);
        } catch (AmazonS3Exception e) {
            String msg = "Failed to delete file";
            dbLog.error(msg);
            log.error(msg + ": {}", e.getMessage());
            log.debug(msg, e);
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
