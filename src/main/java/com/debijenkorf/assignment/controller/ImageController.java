package com.debijenkorf.assignment.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.debijenkorf.assignment.service.ImageService;

@Controller
public class ImageController {
    private ImageService imageService;

    @GetMapping(value="hello", produces="image/jpeg")
    @ResponseBody
    public byte[] getImage() {
        return imageService.getImage();
    }

    @Autowired
    public void setImageService(ImageService imageService) {
        this.imageService = imageService;
    }
}
