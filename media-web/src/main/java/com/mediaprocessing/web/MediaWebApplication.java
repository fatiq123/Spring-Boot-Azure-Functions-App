package com.mediaprocessing.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.mediaprocessing.web", "com.mediaprocessing.common"})
public class MediaWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(MediaWebApplication.class, args);
    }
}
