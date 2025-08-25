package coffeandcommit.crema;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class CremaApplication {

    public static void main(String[] args) {
        SpringApplication.run(CremaApplication.class, args);
    }

}