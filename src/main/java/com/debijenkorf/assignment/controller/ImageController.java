package com.debijenkorf.assignment.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.debijenkorf.assignment.service.ImageService;

@Controller
public class ImageController {
    private ImageService imageService;

    @GetMapping(value="image/show/{type}/{dummySeo}/", produces="image/jpeg")
    @ResponseBody
    public byte[] getImage(@PathVariable("type") String type, @PathVariable("dummySeo") String dummySeo,
                           @RequestParam("reference") String filename) {
        return imageService.getImage(type, filename);
    }

    @DeleteMapping(value="image/flush/{type}/")
    public void flushImage(@PathVariable("type") String type, @RequestParam("reference") String filename) {
        imageService.flushImage(type, filename);
    }

    @Autowired
    public void setImageService(ImageService imageService) {
        this.imageService = imageService;
    }
}
