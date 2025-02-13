package it.unipi.distribooked.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for JWT response containing the authentication token.
 *
 * This class leverages Lombok annotations to reduce boilerplate code.
 * The primary purpose of this class is to encapsulate the JSON Web Token (JWT)
 * used for authentication in the response of an API call.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponseDTO {

    /**
     * The JWT token string used for authentication and authorization.
     * This token is expected to be used in the Authorization header
     * of subsequent API requests.
     */
    @Schema(
            description = "The JWT token string used for authentication and authorization.",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huX2RvZSIsImlhdCI6MTYyNzM3NDQ0NSwiZXhwIjoxNjI3Mzc4MDQ1fQ.sflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
    )
    private String token; // The JWT token string
}
