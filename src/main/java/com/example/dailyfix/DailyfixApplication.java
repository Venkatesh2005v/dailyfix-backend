package com.example.dailyfix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DailyfixApplication {

	public static void main(String[] args) {
		SpringApplication.run(DailyfixApplication.class, args);
	}

}
