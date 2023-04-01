package com.debijenkorf.assignment.data;

import com.debijenkorf.assignment.enums.ScaleTypeEnum;
import com.debijenkorf.assignment.enums.ImageTypeEnum;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ImageType {
    private int height;
    private int width;
    private int quality;
    private String fillColor;
    private ImageTypeEnum type;
    private ScaleTypeEnum scaleType;
}
