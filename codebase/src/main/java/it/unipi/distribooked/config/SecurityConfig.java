package it.unipi.distribooked.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import it.unipi.distribooked.service.UserService;
import it.unipi.distribooked.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Security configuration class for Spring Security.
 *
 * This class defines how the application handles authentication and authorization,
 * including the use of JWT for securing APIs and disabling session-based authentication.
 */
@Configuration
@EnableWebSecurity // enables Spring Security for the application
@EnableMethodSecurity // for method-level security annotations like @PreAuthorize
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Autowired
    private UserService userService;

    @Value("${spring.security.oauth2.resourceserver.jwt.secret-key}")
    private String configuredSecretKey;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    private boolean isTestMode() {
        return activeProfile.contains("test");
    }

    /**
     * Configures the SecurityFilterChain for the application.
     *
     * This method defines:
     * - Disabling CSRF protection (not needed for stateless APIs).
     * - Stateless session management.
     * - Publicly accessible endpoints (e.g., /auth/** and /public/**).
     * - Requiring authentication for all other endpoints.
     * - Integration of Spring Security's OAuth2 Resource Server for JWT validation.
     *
     * @param http The HttpSecurity object to configure security rules.
     * @return The configured SecurityFilterChain.
     * @throws Exception if there is an error in the configuration.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // Create a request matcher for public endpoints that should ignore JWT
        RequestMatcher publicEndpoints = new OrRequestMatcher(
                new AntPathRequestMatcher("/api/v1/auth/login"),
                new AntPathRequestMatcher("/api/v1/auth/register"),
                new AntPathRequestMatcher("/v3/api-docs/**"),
                new AntPathRequestMatcher("/swagger-ui/**"),
                new AntPathRequestMatcher("/swagger-ui.html"),
                new AntPathRequestMatcher("/webjars/**"),
                new AntPathRequestMatcher("/api/v1/authors/**"),
                new AntPathRequestMatcher("/api/v1/books/**"),
                new AntPathRequestMatcher("/api/v1/libraries/**")
        );

        if (isTestMode()) {
            return http
                    .csrf(csrf -> csrf.disable())
                    .sessionManagement(session ->
                            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    )
                    .authorizeHttpRequests(auth -> auth
                            .anyRequest().permitAll()
                    )
                    .build();
        }

        return http
                .csrf(csrf -> csrf.disable()) // CSRF protection is disabled since the application is stateless
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Ensures no HTTP session is created or used
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/swagger-ui/index.html", "/webjars/**").permitAll()
                        .requestMatchers("/api/v1/auth/login").permitAll() // login endpoint is publicly accessible
                        .requestMatchers("/api/v1/auth/register").permitAll() // login endpoint is publicly accessible
                        .requestMatchers("/api/v1/auth/logout").hasAnyRole("USER", "ADMIN") // logout endpoint requires authentication
                        .requestMatchers("/api/v1/authors/**").permitAll()
                        .requestMatchers("/api/v1/books/**").permitAll()
                        .requestMatchers("/api/v1/libraries/**").permitAll()
                        .requestMatchers("/api/v1/users/**").hasRole("USER")
                        .requestMatchers("/api/v1/reservations/**").hasRole("USER")
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN") // Admin-specific endpoints are restricted to users with the ADMIN role
                        .anyRequest().authenticated() // All other endpoints require authentication
                )
//                .oauth2ResourceServer(oauth2 ->
//                        oauth2.jwt(Customizer.withDefaults()) // Configures the application to validate JWT tokens
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwtConfigurer -> jwtConfigurer
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                                .authenticationManager(authentication -> {
                                    ServletRequestAttributes attributes =
                                            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

                                    if (attributes == null) {
                                        return http.getSharedObject(AuthenticationManager.class)
                                                .authenticate(authentication);
                                    }

                                    HttpServletRequest request = attributes.getRequest();

                                    // Skip JWT processing for public endpoints
                                    if (publicEndpoints.matches(request)) {
                                        return null;
                                    }

                                    // For protected endpoints, validate the JWT
                                    try {
                                        String token = ((BearerTokenAuthenticationToken) authentication).getToken();
                                        Jwt decodedJwt = jwtDecoder().decode(token);
                                        return new JwtAuthenticationToken(
                                                decodedJwt,
                                                jwtAuthenticationConverter().convert(decodedJwt).getAuthorities()
                                        );
                                    } catch (JwtException e) {
                                        throw new InvalidBearerTokenException(e.getMessage(), e);
                                    }
                                }))
                        // OAuth2/JWT handler error configuration
                        .authenticationEntryPoint((request, response, authException) -> {
                            if (authException instanceof InvalidBearerTokenException) {
                                logger.error("Invalid token error for request to {}: {}",
                                        request.getRequestURI(),
                                        authException.getMessage());
                            } else {
                                logger.error("Authentication error for request to {}: {}",
                                        request.getRequestURI(),
                                        authException.getMessage());
                            }

                            response.setContentType("application/json");
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());

                            Map<String, Object> errorDetails = new HashMap<>();
                            errorDetails.put("timestamp", new Date());
                            errorDetails.put("status", HttpStatus.UNAUTHORIZED.value());
                            errorDetails.put("error", "Unauthorized");
                            errorDetails.put("message", determineErrorMessage(authException));
                            errorDetails.put("path", request.getRequestURI());

                            String jsonResponse = new ObjectMapper().writeValueAsString(errorDetails);
                            response.getWriter().write(jsonResponse);
                        })
                )
                // authenticated user with insufficient privileges
                .exceptionHandling(handling -> handling
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            logger.error("Access denied for user {} attempting to access {}",
                                    SecurityContextHolder.getContext().getAuthentication().getName(),
                                    request.getRequestURI());

                            response.setContentType("application/json");
                            response.setStatus(HttpStatus.FORBIDDEN.value());

                            Map<String, Object> errorDetails = new HashMap<>();
                            errorDetails.put("timestamp", new Date());
                            errorDetails.put("status", HttpStatus.FORBIDDEN.value());
                            errorDetails.put("error", "Forbidden");
                            errorDetails.put("message", "Insufficient privileges to access this resource");
                            errorDetails.put("path", request.getRequestURI());

                            String jsonResponse = new ObjectMapper().writeValueAsString(errorDetails);
                            response.getWriter().write(jsonResponse);
                        })
                )
                .userDetailsService(userService) // Set the custom UserDetailsService for loading users
                .build(); // Build the SecurityFilterChain
    }

    /**
     * Determine a more descriptive error message for the given AuthenticationException.
     */
    private String determineErrorMessage(AuthenticationException ex) {
        if (ex instanceof InvalidBearerTokenException) {
            return "Invalid or expired JWT token";
        } else if (ex instanceof BadCredentialsException) {
            return "Invalid credentials";
        } else if (ex instanceof InsufficientAuthenticationException) {
            return "Please login to access this resource";
        }
        return "Authentication required. Please login to continue. ";
    }

    /**
     * Defines the bean for encoding passwords using BCrypt.
     *
     * BCrypt is a strong and secure hashing function, commonly used for password storage.
     *
     * @return A PasswordEncoder instance configured to use BCrypt.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Provides the AuthenticationManager bean.
     *
     * AuthenticationManager is a core interface for handling authentication in Spring Security.
     *
     * @param config AuthenticationConfiguration is used to configure and provide the AuthenticationManager.
     * @return A fully configured AuthenticationManager instance.
     * @throws Exception if there is an error in obtaining the AuthenticationManager.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Registers SecurityUtils as a Spring Bean.
     *
     * @return A SecurityUtils instance.
     */
    @Bean
    public SecurityUtils securityUtils() {
        return new SecurityUtils();
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return parameters -> {
            try {
                // Decode the secret key from Base64
                byte[] keyBytes = Base64.getDecoder().decode(configuredSecretKey);

                // Create a secret key using HmacSHA256 for signing the JWT
                SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");
                MACSigner signer = new MACSigner(key);

                // Retrieve the claims map from the input parameters
                Map<String, Object> claimsMap = parameters.getClaims().getClaims();

                // Extract the "roles" claim from the claims map
                Object roles = claimsMap.get("roles");

                // Extract the "user_id" claim from the claims map
                Object userId = claimsMap.get("user_id");

                // Build the JWT claims set
                JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                        .subject((String) claimsMap.get("sub")) // Set the "sub" (subject/username) claim
                        .issueTime(new Date()) // Set the issue time
                        .expirationTime(Date.from(Instant.now().plus(1, ChronoUnit.HOURS))) // Set the expiration time
                        .claim("roles", roles) // Include roles in the claims
                        .claim("user_id", userId) // Include user_id in the claims
                        .build();

                // Sign the JWT with the secret key
                SignedJWT signedJWT = new SignedJWT(
                        new JWSHeader(JWSAlgorithm.HS256), // Header with HMAC-SHA256 algorithm
                        claimsSet // Payload containing the claims
                );
                signedJWT.sign(signer);

                // Convert the signed JWT to a Spring Security-compatible Jwt object
                return Jwt.withTokenValue(signedJWT.serialize()) // Use the serialized JWT token
                        .headers(h -> h.put("alg", "HS256")) // Add the algorithm to the headers
                        .headers(h -> h.put("typ", "JWT")) // Add the token type to the headers
                        .claims(c -> c.putAll(claimsMap)) // Include the original claims
                        .issuedAt(Instant.now()) // Specify the issue time
                        .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS)) // Specify the expiration time
                        .build();
            } catch (Exception e) {
                // Handle any exception during JWT generation
                throw new RuntimeException("Unable to generate JWT token", e);
            }
        };
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        // Decode the Base64-encoded secret key
        byte[] keyBytes = Base64.getDecoder().decode(configuredSecretKey);

        // Create a SecretKey object for HMAC-SHA256 verification
        SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");

        // Build the NimbusJwtDecoder using the secret key
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(key).build();

        // Configure a default validator for the JWT (validates signature, expiration, etc.)
        jwtDecoder.setJwtValidator(JwtValidators.createDefault());

        return jwtDecoder;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        // Create a JwtAuthenticationConverter instance
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        // Add a custom converter for mapping roles from the JWT claims
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // Extract the "roles" claim as a list of strings
            List<String> roles = jwt.getClaimAsStringList("roles");

            // If no roles are found, return an empty list
            if (roles == null) {
                roles = List.of();
            }

            // Map each role to a Spring Security authority with the "ROLE_" prefix
            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
        });

        return converter;
    }


}
