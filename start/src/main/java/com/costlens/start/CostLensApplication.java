package com.costlens.start;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CostLens 启动入口。
 */
@SpringBootApplication(scanBasePackages = "com.costlens")
public class CostLensApplication {

    public static void main(String[] args) {
        SpringApplication.run(CostLensApplication.class, args);
    }
}