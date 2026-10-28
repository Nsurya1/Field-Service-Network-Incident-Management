package com.sochi.fieldservice.config;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import javax.sql.DataSource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FlywayConfigTest {

    @Test
    @DisplayName("Verify Flyway bean instantiation and configuration")
    void shouldConfigureFlywayBean() {
        DataSource mockDataSource = mock(DataSource.class);
        FlywayConfig config = new FlywayConfig();

        Flyway flyway = config.flyway(mockDataSource);
        assertNotNull(flyway);
        assertNotNull(flyway.getConfiguration());
        assertEquals(mockDataSource, flyway.getConfiguration().getDataSource());
    }

    @Test
    @DisplayName("Verify flywayDependsOnPostProcessor attaches flyway dependency to entityManagerFactory")
    void shouldAttachFlywayDependencyToEntityManagerFactory() {
        ConfigurableListableBeanFactory beanFactory = mock(ConfigurableListableBeanFactory.class);
        BeanDefinition beanDefinition = mock(BeanDefinition.class);

        when(beanFactory.containsBeanDefinition("entityManagerFactory")).thenReturn(true);
        when(beanFactory.getBeanDefinition("entityManagerFactory")).thenReturn(beanDefinition);
        when(beanDefinition.getDependsOn()).thenReturn(new String[]{"dataSource"});

        FlywayConfig.flywayDependsOnPostProcessor().postProcessBeanFactory(beanFactory);

        ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
        verify(beanDefinition).setDependsOn(captor.capture());

        List<String> result = List.of(captor.getValue());
        assertTrue(result.contains("dataSource"));
        assertTrue(result.contains("flyway"));
    }

    @Test
    @DisplayName("Verify flywayDependsOnPostProcessor handles null dependsOn and missing bean")
    void shouldHandleNullDependsOnAndMissingBean() {
        ConfigurableListableBeanFactory beanFactory = mock(ConfigurableListableBeanFactory.class);
        BeanDefinition beanDefinition = mock(BeanDefinition.class);

        when(beanFactory.containsBeanDefinition("entityManagerFactory")).thenReturn(true);
        when(beanFactory.getBeanDefinition("entityManagerFactory")).thenReturn(beanDefinition);
        when(beanDefinition.getDependsOn()).thenReturn(null);

        FlywayConfig.flywayDependsOnPostProcessor().postProcessBeanFactory(beanFactory);

        ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
        verify(beanDefinition).setDependsOn(captor.capture());

        List<String> result = List.of(captor.getValue());
        assertEquals(1, result.size());
        assertEquals("flyway", result.get(0));

        // When bean is absent
        ConfigurableListableBeanFactory emptyFactory = mock(ConfigurableListableBeanFactory.class);
        when(emptyFactory.containsBeanDefinition("entityManagerFactory")).thenReturn(false);
        assertDoesNotThrow(() -> FlywayConfig.flywayDependsOnPostProcessor().postProcessBeanFactory(emptyFactory));
    }
}
