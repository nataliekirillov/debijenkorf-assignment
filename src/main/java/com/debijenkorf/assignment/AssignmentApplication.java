package com.debijenkorf.assignment;

import com.debijenkorf.assignment.app.configuration.LogDBProperties;
import com.debijenkorf.assignment.app.configuration.S3Properties;
import com.debijenkorf.assignment.app.configuration.SourceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({LogDBProperties.class, S3Properties.class, SourceProperties.class})
public class AssignmentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssignmentApplication.class, args);
    }

}
