package com.rentdi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RentdiAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(RentdiAppApplication.class, args);
	}

}
