package com.free.easyLearn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
public class EasyLearnApplication {

	public static void main(String[] args) {
		SpringApplication.run(EasyLearnApplication.class, args);
		System.out.println("EasyLearn Application Started Successfully!");
	}

}
