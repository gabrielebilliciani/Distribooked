package it.unipi.distribooked.utils;

import org.springframework.http.ResponseEntity;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

    public class ApiResponseUtil {
        public static <T> ResponseEntity<Map<String, Object>> created(String message, T data, String resourcePath) {
            Map<String, Object> response = new HashMap<>();
            response.put("timestamp", LocalDateTime.now());
            response.put("status", 201);
            response.put("message", message);
            response.put("data", data);
            response.put("path", resourcePath);

            return ResponseEntity.status(201).body(response);
        }

        public static <T> ResponseEntity<Map<String, Object>> ok(String message, T data) {
            Map<String, Object> response = new HashMap<>();
            response.put("timestamp", LocalDateTime.now());
            response.put("status", 200);
            response.put("message", message);
            response.put("data", data);

            return ResponseEntity.ok(response);
        }

    }
