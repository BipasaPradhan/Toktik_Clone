package io.muzoo.scalable.vms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EntityScan("io.muzoo.scalable.vms")
public class VmsApplication {

	public static void main(String[] args) {
		SpringApplication.run(VmsApplication.class, args);
	}



}
