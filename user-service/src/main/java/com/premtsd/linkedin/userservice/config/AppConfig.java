package com.premtsd.linkedin.userservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
//import io.swagger.v3.oas.models.info.Info;
//import io.swagger.v3.oas.models.OpenAPI;
import feign.Capability;
import feign.micrometer.MicrometerCapability;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;


@Configuration
@Slf4j
//@OpenAPIDefinition
public class AppConfig {

    @Bean
    public ModelMapper modelMapper() {
        log.debug("Creating ModelMapper bean");
        return new ModelMapper();
    }

@Bean
public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return mapper;
}




@Bean
public OpenAPI customOpenAPI() {
    return new OpenAPI()
            .info(new Info()
                    .title("User Service API")
                    .version("1.0")
                    .description("API for managing users"))
            .servers(List.of(new Server().url("http://localhost:8080/api/v1/users")));
}


@Bean
public Capability capability(final MeterRegistry registry) {
    return new MicrometerCapability(registry);
}

}
