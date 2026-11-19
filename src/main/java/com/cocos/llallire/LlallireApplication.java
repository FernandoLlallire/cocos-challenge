package com.cocos.llallire;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the portfolio and orders API.
 */
@SpringBootApplication
public class LlallireApplication {

    /**
     * Starts the Spring Boot application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(LlallireApplication.class, args);
    }
}
