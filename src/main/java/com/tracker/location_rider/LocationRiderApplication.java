package com.tracker.location_rider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class LocationRiderApplication {

	public static void main(String[] args) {
		SpringApplication.run(LocationRiderApplication.class, args);
	}

}
