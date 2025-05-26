package io.muzoo.ssc.project.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@SpringBootApplication
@EnableRedisHttpSession
public class WebApplication {

	public static void main(String[] args) throws Throwable {
		SpringApplication.run(WebApplication.class, args);
	}

}
