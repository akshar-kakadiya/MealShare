package com.mealshare.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mealshare.common.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                401, "UNAUTHORIZED", "Authentication required", request.getRequestURI());

        // We need to write timestamp as string since ObjectMapper needs configuration for Instant
        String json = String.format(
                "{\"timestamp\":\"%s\",\"status\":401,\"error\":\"UNAUTHORIZED\",\"message\":\"Authentication required\",\"path\":\"%s\"}",
                java.time.Instant.now().toString(), request.getRequestURI());

        response.getWriter().write(json);
    }
}
