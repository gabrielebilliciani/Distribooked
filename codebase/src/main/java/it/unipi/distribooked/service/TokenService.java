package it.unipi.distribooked.service;

import it.unipi.distribooked.config.SecurityConfig;
import it.unipi.distribooked.model.security.CustomUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for generating JWT tokens.
 *
 * This service generates signed tokens for authenticated users, including claims such as
 * the issuer, expiration time, username, and roles.
 */
@Service
public class TokenService {

    private static final Logger logger = LoggerFactory.getLogger(TokenService.class);

    private final JwtEncoder encoder; // Encoder for signing and generating JWTs

    /**
     * Constructor that injects the JwtEncoder.
     *
     * @param encoder The JwtEncoder for signing tokens.
     */
    public TokenService(JwtEncoder encoder) {
        this.encoder = encoder;
        logger.info("Encoder received: {}", encoder.getClass().getName());

    }

    /**
     * Generates a JWT token for an authenticated user.
     *
     * @param authentication The Authentication object containing user details.
     * @return The generated JWT token as a string.
     */
    public String generateToken(Authentication authentication) {
        Instant now = Instant.now();

        // Extract user ID and roles
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String userId = userDetails.getUserId();
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(role -> role.replace("ROLE_", ""))
                .collect(Collectors.toList());

        // Build the claims for the JWT token
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("self")
                .issuedAt(now)
                .expiresAt(now.plus(1, ChronoUnit.HOURS))
                .subject(authentication.getName())
                .claim("user_id", userId)
                .claim("roles", roles)
                .build();

        logger.debug("Attempting to encode claims: {}", claims);
        logger.debug("Encoder instance: {}", encoder);

        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
