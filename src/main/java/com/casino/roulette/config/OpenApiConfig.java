package com.casino.roulette.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.examples.Example;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI configuration for the Free Roulette Spin System API
 * 
 * This configuration provides comprehensive API documentation including:
 * - Authentication schemes
 * - Common response schemas
 * - Error response examples
 * - Server configurations for different environments
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Free Roulette Spin System API")
                        .version("1.0.0")
                        .description("""
                            ## Overview
                            
                            The Free Roulette Spin System API provides endpoints for managing user rewards, 
                            roulette gameplay, and letter collection mechanics in a casino platform.
                            
                            ## Features
                            
                            - **Mission Management**: Earn and claim free spins through deposit-based missions
                            - **Roulette Gameplay**: Spin the roulette wheel to win cash and letters
                            - **Letter Collection**: Collect letters to form words and claim collection bonuses
                            - **Deposit Processing**: Process deposits and evaluate mission eligibility
                            - **Transaction Tracking**: Complete audit trail of all balance changes
                            
                            ## Authentication
                            
                            All endpoints require a valid User ID in the `X-User-Id` header. This ID should 
                            correspond to a user in the main casino system.
                            
                            ## Error Handling
                            
                            The API uses standard HTTP status codes and returns structured error responses 
                            with detailed error information including error codes and user-friendly messages.
                            
                            ## Rate Limiting
                            
                            Some endpoints may be rate-limited to prevent abuse. Rate limit information 
                            is included in response headers when applicable.
                            """)
                        .contact(new Contact()
                                .name("Casino Development Team")
                                .email("dev@casino.com")
                                .url("https://casino.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://casino.com/license")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080/api")
                                .description("Development server - Local development environment"),
                        new Server()
                                .url("https://staging-api.casino.com/roulette")
                                .description("Staging server - Testing environment"),
                        new Server()
                                .url("https://api.casino.com/roulette")
                                .description("Production server - Live environment")))
                .components(new Components()
                        // Security schemes
                        .addSecuritySchemes("UserIdHeader", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-User-Id")
                                .description("User ID header for authentication. Must be a valid positive integer."))
                        
                        // Common response schemas
                        .addSchemas("ErrorResponse", new Schema<>()
                                .type("object")
                                .description("Standard error response format")
                                .addProperty("success", new Schema<>().type("boolean").example(false))
                                .addProperty("error", new Schema<>().type("string").example("INSUFFICIENT_SPINS"))
                                .addProperty("message", new Schema<>().type("string").example("You don't have enough spins available"))
                                .addProperty("timestamp", new Schema<>().type("string").format("date-time")))
                        
                        .addSchemas("SuccessResponse", new Schema<>()
                                .type("object")
                                .description("Standard success response format")
                                .addProperty("success", new Schema<>().type("boolean").example(true))
                                .addProperty("message", new Schema<>().type("string").example("Operation completed successfully")))
                        
                        // Common API responses
                        .addResponses("BadRequest", new ApiResponse()
                                .description("Bad Request - Invalid input parameters")
                                .content(new Content()
                                        .addMediaType("application/json", new MediaType()
                                                .schema(new Schema<>().$ref("#/components/schemas/ErrorResponse"))
                                                .addExamples("InvalidUserId", new Example()
                                                        .summary("Invalid User ID")
                                                        .value("""
                                                        {
                                                          "success": false,
                                                          "error": "INVALID_USER_ID",
                                                          "message": "User ID must be a positive integer",
                                                          "timestamp": "2024-01-15T10:30:00Z"
                                                        }
                                                        """)))))
                        
                        .addResponses("Unauthorized", new ApiResponse()
                                .description("Unauthorized - Missing or invalid authentication")
                                .content(new Content()
                                        .addMediaType("application/json", new MediaType()
                                                .schema(new Schema<>().$ref("#/components/schemas/ErrorResponse"))
                                                .addExamples("MissingHeader", new Example()
                                                        .summary("Missing X-User-Id header")
                                                        .value("""
                                                        {
                                                          "success": false,
                                                          "error": "MISSING_USER_ID",
                                                          "message": "X-User-Id header is required",
                                                          "timestamp": "2024-01-15T10:30:00Z"
                                                        }
                                                        """)))))
                        
                        .addResponses("InternalServerError", new ApiResponse()
                                .description("Internal Server Error - Unexpected server error")
                                .content(new Content()
                                        .addMediaType("application/json", new MediaType()
                                                .schema(new Schema<>().$ref("#/components/schemas/ErrorResponse"))
                                                .addExamples("ServerError", new Example()
                                                        .summary("Internal server error")
                                                        .value("""
                                                        {
                                                          "success": false,
                                                          "error": "INTERNAL_ERROR",
                                                          "message": "An unexpected error occurred. Please try again later.",
                                                          "timestamp": "2024-01-15T10:30:00Z"
                                                        }
                                                        """))))))
                .addSecurityItem(new SecurityRequirement().addList("UserIdHeader"));
    }
}