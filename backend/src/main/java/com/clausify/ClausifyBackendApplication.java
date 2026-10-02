package com.clausify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ClausifyBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClausifyBackendApplication.class, args);
	}

}
