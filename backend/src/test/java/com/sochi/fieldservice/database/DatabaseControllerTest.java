
package com.sochi.fieldservice.database;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import com.sochi.fieldservice.security.SecurityConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = DatabaseController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class)
)
class DatabaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldReturnDatabaseConnectionStatus() throws Exception {
        // Arrange
        when(jdbcTemplate.queryForObject(
                "SELECT current_database()",
                String.class
        )).thenReturn("fieldservice");

        // Act + Assert
        mockMvc.perform(get("/api/v1/database-check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.database").value("fieldservice"))
                .andExpect(jsonPath("$.connected").value(true));

        // Verify
        verify(jdbcTemplate).queryForObject(
                "SELECT current_database()",
                String.class
        );
    }
}
