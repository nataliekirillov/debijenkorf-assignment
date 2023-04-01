package com.debijenkorf.assignment.strategy;

import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;

@Component
public class S3DirectoryStrategy implements DirectoryStrategy {
    private static final int DIR_SIZE = 4;
    private static final String DELIMITER = "/";

    /**
     * Get the path for saving a file in S3
     *
     * @param typeName Definition type
     * @param filename The file we would like to save
     * @return The path for saving a file in S3
     */
    @Override
    public String getDirectoryStrategy(String typeName, String filename) {
        String filenameNoExt = FilenameUtils.removeExtension(filename);
        String filenameNoDelimiter = filename.replace(DELIMITER, "_").replace("%2F", "_");
        String filenameNoExtNoDelimiter = filenameNoExt.replace(DELIMITER, "_").replace("%2F", "_");

        int length = filenameNoExtNoDelimiter.length();
        if (length <= 4) {
            return String.join(DELIMITER, typeName, filenameNoDelimiter);
        } else if (length <= 8) {
            return String.join(DELIMITER, typeName, getFirstDirectory(filenameNoExtNoDelimiter), filenameNoDelimiter);
        } else {
            return String.join(DELIMITER, typeName, getFirstDirectory(filenameNoExtNoDelimiter),
                    getSecondDirectory(filenameNoExtNoDelimiter), filenameNoDelimiter);
        }
    }

    /**
     * Get the first subdirectory for saving a file in S3
     *
     * @param fileName The file we would like to save
     * @return The first subdirectory for saving a file in S3
     */
    private String getFirstDirectory(String fileName) {
        return fileName.substring(0, DIR_SIZE);
    }

    /**
     * Get the second subdirectory for saving a file in S3
     *
     * @param fileName The file we would like to save
     * @return The second subdirectory for saving a file in S3
     */
    private String getSecondDirectory(String fileName) {
        return fileName.substring(DIR_SIZE, DIR_SIZE * 2);
    }
}
