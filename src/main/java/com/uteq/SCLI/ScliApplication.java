
package com.uteq.SCLI;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling; // ⭐ importa esto

@SpringBootApplication
@EntityScan(basePackages = "com.uteq.SCLI.model")
@EnableJpaRepositories(basePackages = "com.uteq.SCLI.repository")
@EnableScheduling   // ⭐ habilita tareas programadas
public class ScliApplication {

	public static void main(String[] args) {
		SpringApplication.run(ScliApplication.class, args);
	}

}

/*package com.uteq.SCLI;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.uteq.SCLI.model")
@EnableJpaRepositories(basePackages = "com.uteq.SCLI.repository")
public class ScliApplication {

	 

	public static void main(String[] args) {
		SpringApplication.run(ScliApplication.class, args);
	}



}*/
