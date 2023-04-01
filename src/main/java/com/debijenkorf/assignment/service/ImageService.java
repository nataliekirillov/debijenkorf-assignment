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

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map;


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
    private Map<String, ImageType> imageTypes;
    private final CloseableHttpClient httpClient = HttpClients.createDefault();

    @PostConstruct
    public void postConstruct() {
        this.imageTypes = Map.of(
                "Thumbnail", new ImageType(5, 5, 90, "#FFFFFF", ImageTypeEnum.JPG, ScaleTypeEnum.FILL),
                "Original", new ImageType(0, 0, 100, "#FFFFFF", ImageTypeEnum.JPG, ScaleTypeEnum.FILL)
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
            if (type.equalsIgnoreCase("original")) {
                image = getImageFromSource(filename);
            } else {
                image = getAndStoreS3("original", filename);
//                image = compressImage(type, new ByteArrayInputStream(image));
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
        if (!type.equalsIgnoreCase("original")) {
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

    private byte[] compressImage(String type, InputStream is) {
        ImageType imageType = imageTypes.get(type);
        ImageWriter writer = ImageIO.getImageWritersByFormatName(imageType.getType().name()).next();
        ImageWriteParam param = writer.getDefaultWriteParam();

        try {
            BufferedImage image = ImageIO.read(is);
            File compressedImageFile = new File("compressed_image.jpg");
            OutputStream os = new FileOutputStream(compressedImageFile);
            ImageOutputStream ios = ImageIO.createImageOutputStream(os);

            writer.setOutput(ios);
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(imageType.getQuality());
            writer.write(null, new IIOImage(image, null, null), param);

            // todo
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to compress image");
        }

        return null;
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
