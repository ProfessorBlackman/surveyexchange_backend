package com.tlc.surveyexchange_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SurveyexchangeBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SurveyexchangeBackendApplication.class, args);
    }

}
