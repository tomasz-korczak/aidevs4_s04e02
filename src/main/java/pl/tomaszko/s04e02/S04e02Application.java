package pl.tomaszko.s04e02;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class S04e02Application {

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(S04e02Application.class, args)));
    }
}
