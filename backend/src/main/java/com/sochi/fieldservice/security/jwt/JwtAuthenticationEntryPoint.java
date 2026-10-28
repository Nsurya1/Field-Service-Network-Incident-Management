package com.sochi.fieldservice.security.jwt;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        String message = authException != null && authException.getMessage() != null
                ? authException.getMessage().replace("\"", "\\\"")
                : "Full authentication is required to access this resource";

        String json = String.format(
                "{\"status\":401,\"message\":\"Unauthorized: %s\",\"timestamp\":\"%s\"}",
                message,
                LocalDateTime.now()
        );

        response.getWriter().write(json);
    }
}
