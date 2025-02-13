package it.unipi.distribooked;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableScheduling;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO) // otherwise, pagination might not work as expected
@SpringBootApplication
@EnableScheduling
public class DistribookedApplication {

	public static void main(String[] args) {
		SpringApplication.run(DistribookedApplication.class, args);
	}

}
