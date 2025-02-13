package it.unipi.distribooked.controller;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.distribooked.dto.*;
import it.unipi.distribooked.dto.swagger.CreatedResponse;
import it.unipi.distribooked.dto.swagger.ErrorResponse;
import it.unipi.distribooked.dto.swagger.SuccessResponse;
import it.unipi.distribooked.service.AuthService;
import it.unipi.distribooked.utils.ApiResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.util.Map;

/**
 * The AuthController class manages authentication-related API endpoints.
 * It provides functionalities for user registration, login, and logout.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Operations users/admins authentication.")
@Validated
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    /**
     * Registers a new user.
     *
     * @param request The details of the user to register.
     * @return The details of the newly registered user.
     */
    @Operation(summary = "Register a new user", description = "Creates a new user account with the specified details.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                    content = @Content(
                            schema = @Schema(
                                    implementation = CreatedResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = "{\"timestamp\":\"2025-01-31T12:00:00.000Z\",\"status\":201,\"message\":\"User registered successfully\",\"data\":{\"id\":\"889a770c426571df71448d64\",\"username\":\"johndoe\"},\"path\":\"/api/v1/auth/register\"}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid registration details",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "User already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegistrationRequestDTO request) {
        logger.info("Received registration request for username: {}", request.getUsername());

        RegistrationResponseDTO response = authService.registerUser(
                request.getUsername(),
                request.getPassword(),
                request.getEmail(),
                request.getAddress(),
                request.getName(),
                request.getSurname(),
                request.getDateOfBirth()
        );

        logger.info("User '{}' successfully registered", request.getUsername());

        return ApiResponseUtil.created(
                "User registered successfully",
                response,
                "/api/v1/auth/register"
        );

    }

    /**
     * Logs in a user.
     *
     * @param request The login credentials of the user.
     * @return A JWT response containing the access and refresh tokens.
     */
    @Operation(summary = "Log in a user", description = "Authenticates a user and returns JWT tokens.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(
                            schema = @Schema(
                                    implementation = SuccessResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = "{\n" +
                                            "  \"data\": {\n" +
                                            "    \"token\": \"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huX2RvZSIsImV4cCI6MTczOTEyMDUyMSwidXNlcl9pZCI6IjY3YTUyNjllNjhiNzhkNzQ3OWQzOTFhNiIsImlhdCI6MTczOTExNjkyMSwicm9sZXMiOlsiVVNFUiJdfQ.E4kui5Mwzyrd7_WNTrxWb0z8i6_rqXFsINEXnzTXyqM\"\n" +
                                            "  },\n" +
                                            "  \"message\": \"Login successful\",\n" +
                                            "  \"timestamp\": \"2025-02-09T17:02:01.888945\",\n" +
                                            "  \"status\": 200\n" +
                                            "}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid input data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequestDTO request) {
        logger.info("Received login request for user: {}", request.getUsername());
        String token = authService.authenticateUser(
                request.getUsername(),
                request.getPassword()
        );
        logger.info("Login successful for user: {}", request.getUsername());

        return ApiResponseUtil.ok(
                "Login successful",
                new JwtResponseDTO(token)
        );

    }

    /**
     * Logs out a user.
     *
     * This endpoint is stateless; the server does not manage token invalidation.
     * We are keeping the endpoint for future further development, so that clients can call it to log out.
     *
     * @return A confirmation response for the logout.
     */
    @Operation(
            summary = "Log out a user",
            description = "Logs out the currently authenticated user."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logout successful"),
            @ApiResponse(responseCode = "400", description = "Logout failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Please login to access this resource.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Invalid or expired token.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        logger.info("Received logout request");

        // We are not doing anything here, as the server does not manage token invalidation.
        // We are keeping the endpoint for future further development, so that clients can call it to log out.

        logger.info("Logout processed successfully");

        return ApiResponseUtil.ok(
                "Logout successful",
                null // no extra data to return
        );
    }
}
