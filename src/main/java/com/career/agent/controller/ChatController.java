package com.career.agent.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api")
public class ChatController {

    @Value("${groq.api.key}")
    private String groqApiKey;
    
    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody Map<String, String> body) {

        try {
            String msg = body.get("message");
            if (msg == null || msg.isBlank()) {
                return Map.of("reply", "Ask a career-related question.");
            }

            Map<String, Object> payload = Map.of(
                "model", "llama-3.1-8b-instant",
                "messages", List.of(
                    Map.of("role", "system", "content",
                            "You are a career assistant. Answer only about jobs, careers, skills, resumes, and interviews."),
                    Map.of("role", "user", "content", msg)
                ),
                "temperature", 0.7
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(groqApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://api.groq.com/openai/v1/chat/completions",
                    entity,
                    Map.class
            );

            List choices = (List) response.getBody().get("choices");
            Map choice = (Map) choices.get(0);
            Map message = (Map) choice.get("message");

            return Map.of("reply", message.get("content").toString());

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("reply", "Groq error. Check server logs.");
        }
    }

}
