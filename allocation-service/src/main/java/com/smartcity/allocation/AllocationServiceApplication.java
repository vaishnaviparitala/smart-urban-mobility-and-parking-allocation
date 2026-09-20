package com.smartcity.allocation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class AllocationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AllocationServiceApplication.class, args);
    }
}
