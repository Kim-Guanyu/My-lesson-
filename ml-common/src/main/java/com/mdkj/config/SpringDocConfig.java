package com.mdkj.config;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@RefreshScope
public class SpringDocConfig {

    @Value("${springdoc.author}")
    private String author;
    @Value("${springdoc.url}")
    private String url;
    @Value("${springdoc.title}")
    private String title;
    @Value("${springdoc.description}")
    private String description;
    @Value("${springdoc.version}")
    private String version;

    /** 通用信息Bean */
    @Bean
    public OpenAPI commonInfo() {
        return new OpenAPI().info(new Info()
                .title(title)
                .description(description)
                .version(version)
                .contact(new Contact().name(author).url(url)));
    }
}
