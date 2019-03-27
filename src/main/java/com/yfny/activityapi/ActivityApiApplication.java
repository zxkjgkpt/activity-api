package com.yfny.activityapi;

import org.activiti.spring.boot.SecurityAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

/**
 * 启动类
 */
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
@EnableEurekaClient
public class ActivityApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ActivityApiApplication.class, args);
    }

}
