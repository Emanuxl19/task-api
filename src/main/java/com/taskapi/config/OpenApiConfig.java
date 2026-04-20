package com.taskapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Task Management API")
                .version("1.0.0")
                .description("RESTful API for task management — portfolio project")
                .contact(new Contact()
                    .name("Your Name")
                    .email("your@email.com")
                    .url("https://github.com/your-username")));
    }
}
