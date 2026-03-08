package org.processing.system.smsconsumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SmsConsumerApplication {
    public static void main(String[] args) {

        SpringApplication.run(SmsConsumerApplication.class, args);
    }
}