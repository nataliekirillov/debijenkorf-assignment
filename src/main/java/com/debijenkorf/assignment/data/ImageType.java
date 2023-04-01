package com.debijenkorf.assignment.data;

import com.debijenkorf.assignment.enums.ScaleTypeEnum;
import com.debijenkorf.assignment.enums.ImageTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ImageType {
    private String name;
    private int height;
    private int width;
    private int quality;
    private String fillColor;
    private ImageTypeEnum type;
    private ScaleTypeEnum scaleType;
}
