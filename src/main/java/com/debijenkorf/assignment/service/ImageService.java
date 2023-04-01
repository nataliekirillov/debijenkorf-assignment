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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

import static com.debijenkorf.assignment.util.ImageUtil.toBufferedImage;
import static com.debijenkorf.assignment.util.ImageUtil.toByteArray;


/**
 * A service responsible for the retrieval of images
 */
@Service
@Slf4j
public class ImageService {
    private static final String DEFAULT_IMAGE_TYPE = "original";

    private DbLogger dbLog;
    private S3Service s3Service;
    private SourceProperties sourceProperties;
    private S3DirectoryStrategy directoryStrategy;
    private Map<String, ImageType> imageTypes;
    private final CloseableHttpClient httpClient = HttpClients.createDefault();

    @PostConstruct
    public void postConstruct() {
        this.imageTypes = Map.of(
                "thumbnail", new ImageType(100, 100, 90, "#ff0000", ImageTypeEnum.JPG, ScaleTypeEnum.FILL),
                "fill", new ImageType(100, 100, 90, "#ff0000", ImageTypeEnum.JPG, ScaleTypeEnum.FILL),
                "crop", new ImageType(1000, 1000, 90, "#ff0000", ImageTypeEnum.JPG, ScaleTypeEnum.CROP),
                "skew", new ImageType(100, 100, 90, "#ff0000", ImageTypeEnum.JPG, ScaleTypeEnum.SKEW),
                "skew-high", new ImageType(300, 100, 90, "#ff0000", ImageTypeEnum.JPG, ScaleTypeEnum.SKEW),
                DEFAULT_IMAGE_TYPE, new ImageType(0, 0, 100, "#FFFFFF", ImageTypeEnum.JPG, ScaleTypeEnum.FILL)
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

        return getAndStoreS3(type, filename);
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
            log.info("File not found in S3: {}", filename);
            dbLog.info("File not found in S3: " + filename);
        } catch (IOException e) {
            log.error("Failed to get file from S3: {}", e.getMessage());
            log.debug("Failed to get file from S3", e);
            dbLog.error("Failed to get file from S3");
        }

        return new byte[0];
    }

    public byte[] getAndStoreS3(String type, String filename) {
        byte [] image = getImageFromS3(type, filename);

        if (image.length == 0) {
            if (type.equalsIgnoreCase(DEFAULT_IMAGE_TYPE)) {
                image = getImageFromSource(filename);
            } else {
                image = getAndStoreS3(DEFAULT_IMAGE_TYPE, filename);
                image = resizeImage(type, image);
            }

            // found image in source - store it
            storeImage(type, filename, image);

            // check that storing worked correctly
            if (!Arrays.equals(image, getImageFromS3(type, filename))) {
                log.error("Failed to get image from S3");
                dbLog.error("Failed to get image from S3");
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found on source");
            }
        }

        return image;
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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found on source");
        }
    }

    /**
     * Flush an image from S3
     *
     * @param type     Definition type
     * @param filename File path
     */
    public void flushImage(String type, String filename) {
        if (!type.equalsIgnoreCase(DEFAULT_IMAGE_TYPE)) {
            deleteImage(type, filename);
            return;
        }

        imageTypes.keySet().forEach(x -> deleteImage(x, filename));
    }

    private void deleteImage(String type, String filename) {
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
        return imageTypes.keySet().stream().anyMatch(x -> x.equalsIgnoreCase(type));
    }

    private void storeImage(String type, String filename, byte[] image){
        String s3Filepath = directoryStrategy.getDirectoryStrategy(type, filename);
        try {
            s3Service.upload(s3Filepath, new ByteArrayInputStream(image));
        } catch (AmazonS3Exception e) {
            dbLog.error("Failed to save image to S3");
            log.error("Failed to save image to S3: {}", e.getMessage());
            log.debug("Failed to save image to S3", e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to store image on S3");
        }
    }

    private byte[] resizeImage(String type, byte[] image) {
        ImageType imageType = imageTypes.get(type.toLowerCase());

        try {
            Image resultingImage = getResultingImage(imageType, toBufferedImage(image));

            BufferedImage target = new BufferedImage(imageType.getWidth(), imageType.getHeight(),
                    BufferedImage.TYPE_INT_RGB);

            int widthBuffer = Math.abs((resultingImage.getWidth(null) - target.getWidth()) / 2);
            int heightBuffer = Math.abs((resultingImage.getHeight(null) - target.getHeight()) / 2);

            Graphics2D graphics = target.createGraphics();

            switch (imageType.getScaleType()) {
                case CROP -> graphics.drawImage(resultingImage, 0, 0, target.getWidth(), target.getHeight(),
                        widthBuffer, heightBuffer, resultingImage.getWidth(null) - widthBuffer,
                        resultingImage.getHeight(null) - heightBuffer, null);
                case FILL -> graphics.drawImage(resultingImage, widthBuffer, heightBuffer,
                        target.getWidth() - widthBuffer, target.getHeight() - heightBuffer, 0, 0,
                        resultingImage.getWidth(null), resultingImage.getHeight(null),
                        Color.decode(imageType.getFillColor()), null);
                case SKEW -> graphics.drawImage(resultingImage, 0, 0, target.getWidth(), target.getHeight(), 0, 0,
                        resultingImage.getWidth(null), resultingImage.getHeight(null), null);
            }

            return toByteArray(target, imageType.getType().toString());
        } catch (IOException e) {
            String msg = "Failed to resize image";
            log.error(msg + ": {}", e.getMessage());
            log.debug(msg, e);
            dbLog.error(msg);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, msg);
        }
    }

    public Image getResultingImage(ImageType imageType, BufferedImage origin) {
        Image resultingImage;

        boolean wider = (imageType.getWidth() / (double) imageType.getHeight()) >= (origin.getWidth() / (double) origin.getHeight());
        boolean crop = imageType.getScaleType().equals(ScaleTypeEnum.CROP);
        boolean fill = imageType.getScaleType().equals(ScaleTypeEnum.FILL);

        if ((crop && wider) || (fill && !wider)) {
            // CROP && WIDER - keep the ratio and match the width
            // FILL && HIGHER - keep the ratio and match the width
            resultingImage = origin.getScaledInstance(imageType.getWidth(), -1, Image.SCALE_DEFAULT);
        } else if (crop || fill) {
            // CROP && HIGHER - keep the ratio and match the height
            // FILL && WIDER - keep the ratio and match the height
            resultingImage = origin.getScaledInstance(-1, imageType.getHeight(), Image.SCALE_DEFAULT);
        } else {
            // SKEW - fits the original image to the requested width and height
            resultingImage = origin.getScaledInstance(imageType.getWidth(), imageType.getHeight(), Image.SCALE_DEFAULT);
        }

        return resultingImage;
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
