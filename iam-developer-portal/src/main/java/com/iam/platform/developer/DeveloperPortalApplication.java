package com.iam.platform.developer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DeveloperPortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeveloperPortalApplication.class, args);
    }
}
