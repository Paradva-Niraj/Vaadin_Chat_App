package com.example.application.services;

import com.example.application.supabase.SupabaseClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.function.Predicate;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class AuthenticationService {

    private final SupabaseClient supabaseClient;
    @SuppressWarnings("unused")
    private final ObjectMapper objectMapper;

    @Autowired
    public AuthenticationService(SupabaseClient supabaseClient, ObjectMapper objectMapper) {
        this.supabaseClient = supabaseClient;
        this.objectMapper = objectMapper;
    }

    private static final Predicate<HttpStatusCode> IS_ERROR = HttpStatusCode::isError;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Mono<Map> signUp(String email, String password, String username, Integer age, String gender) {
        // First, check if username is available
        return checkUsernameAvailability(username)
            .flatMap(isAvailable -> {
                if (!isAvailable) {
                    return Mono.error(new RuntimeException("Username is already taken"));
                }
                String url = supabaseClient.getProjectUrl() + "/auth/v1/signup";

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("email", email);
                requestBody.put("password", password);

                Map<String, Object> userMetadata = new HashMap<>();
                userMetadata.put("username", username);
                userMetadata.put("age", age);
                userMetadata.put("gender", gender);
                requestBody.put("data", userMetadata);

                return supabaseClient
                    .getWebClient()
                    .post()
                    .uri(url)
                    .header("apikey", supabaseClient.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(
                        IS_ERROR,
                        response ->
                            response.bodyToMono(String.class)
                                .doOnNext(errorBody -> System.out.println("Signup error: " + errorBody))
                                .flatMap(errorBody -> Mono.error(new RuntimeException("Sign-up failed: " + errorBody))))
                    .bodyToMono(Map.class)
                    .doOnSuccess(response -> System.out.println("Signup response: " + response))
                    .flatMap(response -> {
                        Map<String, Object> user = (Map<String, Object>) response.get("user");
                        String userId = user != null ? (String) user.get("id") : null;
                        String confirmedEmail = (String) user.get("email");
                        if (userId == null) {
                            return Mono.error(new RuntimeException("User ID not found in signup response"));
                        }
                        return storeProfile(userId, confirmedEmail, username, age, gender).thenReturn(response);
                    });
            });
    }

    private Mono<Boolean> checkUsernameAvailability(String username) {
        String url = supabaseClient.getProjectUrl() + "/rest/v1/profiles?username=eq." + username + "&select=id";
        return supabaseClient
            .getWebClient()
            .get()
            .uri(url)
            .header("apikey", supabaseClient.getApiKey())
            .retrieve()
            .onStatus(
                IS_ERROR,
                response ->
                    response.bodyToMono(String.class)
                        .doOnNext(errorBody -> System.out.println("Username check error: " + errorBody))
                        .flatMap(errorBody -> Mono.error(new RuntimeException("Username check failed: " + errorBody))))
            .bodyToMono(JsonNode.class)
            .map(node -> node.size() == 0) // If no rows, username is available
            .doOnSuccess(isAvailable -> System.out.println("Username " + username + " available: " + isAvailable));
    }

    private Mono<Void> storeProfile(String userId, String email, String username, Integer age, String gender) {
        String url = supabaseClient.getProjectUrl() + "/rest/v1/profiles";
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", userId);
        profile.put("email", email);
        profile.put("username", username);
        profile.put("age", age);
        profile.put("gender", gender);

        return supabaseClient
            .getWebClient()
            .post()
            .uri(url)
            .header("apikey", supabaseClient.getApiKey())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(profile)
            .retrieve()
            .onStatus(
                IS_ERROR,
                response ->
                    response.bodyToMono(String.class)
                        .doOnNext(errorBody -> System.out.println("Profile error: " + errorBody))
                        .flatMap(errorBody -> Mono.error(new RuntimeException("Profile creation failed: " + errorBody))))
            .bodyToMono(Void.class)
            .doOnSuccess(v -> System.out.println("Profile stored for user: " + username + " with email: " + email));
    }

    public Mono<JsonNode> signInWithEmail(String email, String password) {
        String url = supabaseClient.getProjectUrl() + "/auth/v1/token?grant_type=password";
    
        return supabaseClient
            .getWebClient()
            .post()
            .uri(url)
            .header("apikey", supabaseClient.getApiKey())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("email", email, "password", password))
            .retrieve()
            .onStatus(
                IS_ERROR,
                response ->
                    response.bodyToMono(String.class)
                        .doOnNext(errorBody -> System.out.println("Signin error: " + errorBody))
                        .flatMap(errorBody -> Mono.error(new RuntimeException("Sign-in failed: " + errorBody))))
            .bodyToMono(JsonNode.class)
            .doOnSuccess(response -> System.out.println("Signin response: " + response));
    }

    public Mono<JsonNode> signInWithUsername(String username, String password) {
        return getEmailByUsername(username)
            .flatMap(
                email -> {
                    if (email == null || email.isEmpty()) {
                        return Mono.error(new RuntimeException("Username not found"));
                    }
                    return signInWithEmail(email, password);
                })
            .switchIfEmpty(Mono.error(new RuntimeException("Username not found")));
    }

    private Mono<String> getEmailByUsername(String username) {
        String url = supabaseClient.getProjectUrl() + "/rest/v1/profiles?username=eq." + username + "&select=email";
        return supabaseClient
            .getWebClient()
            .get()
            .uri(url)
            .header("apikey", supabaseClient.getApiKey())
            .retrieve()
            .onStatus(
                IS_ERROR,
                response ->
                    response.bodyToMono(String.class)
                        .doOnNext(errorBody -> System.out.println("Lookup error: " + errorBody))
                        .flatMap(errorBody -> Mono.error(new RuntimeException("Username lookup failed: " + errorBody))))
            .bodyToMono(JsonNode.class)
            .map(node -> node.size() > 0 ? node.get(0).get("email").asText() : null)
            .doOnSuccess(email -> System.out.println("Retrieved email for " + username + ": " + email));
    }
    @SuppressWarnings("rawtypes")
    public Mono<Map> signIn(String email, String password) {
        String url = supabaseClient.getProjectUrl() + "/auth/v1/token?grant_type=password";

        return supabaseClient
            .getWebClient()
            .post()
            .uri(url)
            .header("apikey", supabaseClient.getApiKey())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("email", email, "password", password))
            .retrieve()
            .onStatus(
                IS_ERROR,
                response ->
                    response.bodyToMono(String.class)
                        .flatMap(errorBody -> Mono.error(new RuntimeException("Sign-in failed: " + errorBody))))
            .bodyToMono(Map.class)
            .doOnSuccess(success -> System.out.println("Sign-in successful: " + success))
            .doOnError(error -> System.err.println("Sign-in error: " + error.getMessage()));
    }

    public Mono<JsonNode> validateJWT(String jwt) {
        String url = supabaseClient.getProjectUrl() + "/auth/v1/user";

        return supabaseClient
            .getWebClient()
            .get()
            .uri(url)
            .header("apikey", supabaseClient.getApiKey())
            .header("Authorization", "Bearer " + jwt)
            .retrieve()
            .onStatus(
                IS_ERROR,
                response ->
                    response.bodyToMono(String.class)
                        .flatMap(errorBody -> Mono.error(new RuntimeException("JWT validation failed: " + errorBody))))
            .bodyToMono(JsonNode.class)
            .doOnSuccess(success -> System.out.println("JWT valid: " + success))
            .doOnError(error -> System.err.println("JWT validation error: " + error.getMessage()));
    }


    // public Mono<JsonNode> validateJWT(String jwt) {
    //     String url = supabaseClient.getProjectUrl() + "/auth/v1/user";

    //     return supabaseClient
    //         .getWebClient()
    //         .get()
    //         .uri(url)
    //         .header("apikey", supabaseClient.getApiKey())
    //         .header("Authorization", "Bearer " + jwt)
    //         .retrieve()
    //         .onStatus(
    //             IS_ERROR,
    //             response ->
    //                 response.bodyToMono(String.class)
    //                     .flatMap(errorBody -> Mono.error(new RuntimeException("JWT validation failed: " + errorBody))))
    //         .bodyToMono(JsonNode.class);
    // }
}