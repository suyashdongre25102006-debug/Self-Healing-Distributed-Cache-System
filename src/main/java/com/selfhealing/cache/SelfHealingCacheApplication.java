
package com.selfhealing.cache;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SelfHealingCacheApplication {

	public static void main(String[] args) {

		SpringApplication.run(
				SelfHealingCacheApplication.class,
				args
		);
	}
}
