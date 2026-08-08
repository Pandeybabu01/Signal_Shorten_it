package com.urlshortener.config;

import com.urlshortener.security.JwtProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationPropertiesScan(basePackageClasses = {AppProperties.class, JwtProperties.class})
public class PropertiesConfig {
}
