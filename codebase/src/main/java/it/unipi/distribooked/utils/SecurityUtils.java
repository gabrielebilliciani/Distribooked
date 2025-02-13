package it.unipi.distribooked.utils;

import it.unipi.distribooked.model.security.CustomUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    Logger logger = LoggerFactory.getLogger(SecurityUtils.class);

    /**
     * Retrieves the current user's ID from the JWT claims in the security context.
     *
     * @return The user ID of the currently authenticated user.
     * @throws IllegalStateException if the user ID cannot be retrieved.
     */
    public String getCurrentUserId() {
        logger.debug("Retrieving user ID from the security context.");

        // Retrieve the current authentication object
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            logger.error("Authentication object is null.");
            throw new IllegalStateException("Authentication object is missing in the security context.");
        }

        logger.debug("Authentication class: {}", authentication.getClass().getName());
        logger.debug("Authentication principal: {}", authentication.getPrincipal());

        // Ensure authentication is valid and contains a Jwt token
        if (authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();

            // Log the claims in the JWT
            logger.debug("JWT claims: {}", jwt.getClaims());

            // Extract the user ID from the "user_id" claim in the JWT
            String userId = jwt.getClaimAsString("user_id");
            if (userId != null) {
                return userId;
            } else {
                logger.error("The 'user_id' claim is missing in the JWT.");
            }
        } else {
            logger.error("Authentication principal is not an instance of Jwt. Principal: {}", authentication.getPrincipal());
        }

        // Throw an exception if the user ID cannot be retrieved
        throw new IllegalStateException("Unable to retrieve user ID from the security context.");
    }


}
