package com.vigza.markweave;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.vigza.markweave.infrastructure.persistence.mapper")
@SpringBootApplication
public class MarkweaveApplication {

	public static void main(String[] args) {
		SpringApplication.run(MarkweaveApplication.class, args);
	}

}
