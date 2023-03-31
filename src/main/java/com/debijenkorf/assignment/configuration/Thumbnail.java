package com.debijenkorf.assignment.configuration;

import com.debijenkorf.assignment.enums.ScaleTypeEnum;
import com.debijenkorf.assignment.enums.ImageTypeEnum;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Thumbnail {
    protected int height;
    protected int width;
    protected int quality;
    protected String fillColor;
    protected ImageTypeEnum type;
    protected ScaleTypeEnum scaleType;
}
