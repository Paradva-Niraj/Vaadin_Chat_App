package com.example.application.services;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GeminiService {
    
   
    private static final String API_KEY = "AIzaSyBEdn5NyxjXPXUUa5tn3ho88cMu9vOSm0s";
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
            .baseUrl("https://generativelanguage.googleapis.com/v1beta/models")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    public Mono<String> getGeminiResponse(String prompt) {
        String requestBody = String.format("""
            {
                "contents": [{
                    "parts": [{
                        "text": "%s"
                    }]
                }]
            }
            """, prompt.replace("\"", "\\\""));

        return webClient.post()
            .uri("/gemini-2.0-flash:generateContent?key=" + API_KEY)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .flatMap(response -> {
                try {
                    JsonNode candidates = response.path("candidates");
                    if (candidates.isEmpty()) {
                        return Mono.error(new RuntimeException("No candidates in response"));
                    }
                    
                    JsonNode content = candidates.get(0).path("content");
                    JsonNode parts = content.path("parts");
                    if (parts.isEmpty()) {
                        return Mono.error(new RuntimeException("No parts in response"));
                    }
                    
                    String text = parts.get(0).path("text").asText();
                    return Mono.just(text);
                } catch (Exception e) {
                    return Mono.error(e);
                }
            })
            .onErrorResume(e -> Mono.just("Error processing request: " + e.getMessage()));
    }
}