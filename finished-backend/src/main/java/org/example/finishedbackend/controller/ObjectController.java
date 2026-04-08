package org.example.finishedbackend.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.example.finishedbackend.entity.RestBean;
import org.example.finishedbackend.service.ImageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.FileNotFoundException;

@Controller
@Slf4j
public class ObjectController {

    @Resource
    ImageService imageService;

    @GetMapping("/images/**")
    public void imageFetch(HttpServletRequest request,
                           HttpServletResponse response) throws Exception {
        String imagePath = request.getServletPath().substring(7);
        ServletOutputStream outputStream = response.getOutputStream();
        if (imagePath.length() < 13) {
            response.setStatus(404);
            outputStream.println(RestBean.failure(404, "404 Not Found").asJSONString());
            return;
        }
        try {
            imageService.fetchImage(outputStream, imagePath);
            response.setHeader("Cache-Control", "max-age=2592000");
            response.setContentType("image/jpeg");
        } catch (FileNotFoundException e) {
            response.setStatus(404);
            outputStream.println(RestBean.failure(404, "404 Not Found").asJSONString());
        } catch (Exception e) {
            log.error("获取图片异常：{}", e.getMessage());
            response.setStatus(500);
        }
    }
}
