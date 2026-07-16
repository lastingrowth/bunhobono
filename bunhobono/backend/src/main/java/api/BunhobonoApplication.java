package api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class BunhobonoApplication {

	public static void main(String[] args) {
		SpringApplication.run(BunhobonoApplication.class, args);
	}

}