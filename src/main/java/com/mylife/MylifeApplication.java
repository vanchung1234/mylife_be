package com.mylife;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Lớp khởi động chính của ứng dụng Spring Boot.
 * 
 * @SpringBootApplication bao gồm:
 *                        - @Configuration: đánh dấu lớp này có chứa các @Bean
 *                        - @EnableAutoConfiguration: tự động cấu hình Spring
 *                        dựa trên dependencies
 *                        - @ComponentScan: quét các component trong package
 *                        hiện tại và sub-packages
 */
@SpringBootApplication
@EnableJpaAuditing // Bật auditing cho JPA (sử dụng @CreatedDate, @LastModifiedDate)
@EnableScheduling // Bật khả năng chạy các tác vụ định kỳ (@Scheduled)
@EnableAsync // Bật xử lý bất đồng bộ (@Async)
public class MylifeApplication {

	public static void main(String[] args) {
		// SpringApplication.run sẽ khởi động Spring container và embedded server
		SpringApplication.run(MylifeApplication.class, args);
	}
}