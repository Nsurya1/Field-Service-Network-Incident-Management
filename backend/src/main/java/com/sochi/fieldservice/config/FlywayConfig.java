package com.sochi.fieldservice.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    @ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true", matchIfMissing = true)
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load();
    }

    @Bean
    public static BeanFactoryPostProcessor flywayDependsOnPostProcessor() {
        return beanFactory -> {
            String[] jpaBeans = new String[]{"entityManagerFactory"};
            for (String beanName : jpaBeans) {
                if (beanFactory.containsBeanDefinition(beanName)) {
                    BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
                    String[] dependsOn = bd.getDependsOn();
                    List<String> list = dependsOn != null ? new ArrayList<>(Arrays.asList(dependsOn)) : new ArrayList<>();
                    if (!list.contains("flyway")) {
                        list.add("flyway");
                        bd.setDependsOn(list.toArray(new String[0]));
                    }
                }
            }
        };
    }
}
