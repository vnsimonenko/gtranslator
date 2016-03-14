package com.gtranslator;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import javax.validation.Validator;
import java.nio.file.Paths;

@Configuration
public class ApplicationConfiguration {

    @Bean
    public Validator validator() {
        return new LocalValidatorFactoryBean();
    }

    @Bean
    @DependsOn({"validator"})
    public MethodValidationPostProcessor methodValidationPostProcessor(Validator validator) {
        MethodValidationPostProcessor methodValidationPostProcessor = new MethodValidationPostProcessor();
        methodValidationPostProcessor.setValidator(validator);
        return methodValidationPostProcessor;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Autowired
    private ConfigurableEnvironment env;

    @PostConstruct
    public void postConstruct() {
        setDefaultPropertySource("workspace", Paths.get(System.getProperty("user.home"), "gtranslator").toFile().getAbsolutePath());
    }

    private void setDefaultPropertySource(String propName, Object value) {
        if (StringUtils.isBlank(env.getProperty(propName))) {
            env.getPropertySources()
                    .addFirst(new MapPropertySource(propName,
                            ImmutableMap.of(propName, value)));
        }
    }
}
