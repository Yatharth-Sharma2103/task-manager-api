package org.example.taskmanager.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Public landing endpoint so hitting the root URL shows helpful info instead of a 401.
 */
@RestController
@Tag(name = "Info", description = "Public service information")
public class RootController {

    @GetMapping("/")
    public Map<String, Object> index() {
        return Map.of(
                "application", "Task Manager API",
                "status", "UP",
                "documentation", "/swagger-ui.html",
                "openApiSpec", "/v3/api-docs",
                "publicEndpoints", List.of("/api/auth/register", "/api/auth/login"),
                "hint", "Register or log in to get a JWT, then send it as 'Authorization: Bearer <token>'."
        );
    }
}
