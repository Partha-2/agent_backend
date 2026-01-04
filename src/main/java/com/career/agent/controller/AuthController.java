package com.career.agent.controller;

import com.career.agent.config.JwtUtil;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtUtil jwtUtil;

    @Value("${google.client-id}")
    private String clientId;

    public AuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/google")
    public Map<String, Object> googleLogin(@RequestBody Map<String, String> body, HttpServletResponse response) {
        try {
            String tokenString = body.get("token");
            if (tokenString == null || tokenString.isEmpty()) {
                return Map.of("success", false, "error", "Token missing");
            }

            // Verify token
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance()).setAudience(List.of(clientId)).build();

            GoogleIdToken idToken = verifier.verify(tokenString);
            if (idToken == null) {
                return Map.of("success", false, "error", "Invalid Google Token");
            }

            String email = idToken.getPayload().getEmail();
            String jwt = jwtUtil.generateToken(email);

            // Set cookie from backend - NO SPACES, NO DECODING ISSUES
            Cookie jwtCookie = new Cookie("jwt_token", jwt);
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(86400);
            jwtCookie.setHttpOnly(false); // Let frontend see it if needed
            response.addCookie(jwtCookie);

            return Map.of(
                    "success", true,
                    "email", email,
                    "jwt", jwt);

        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }
}
