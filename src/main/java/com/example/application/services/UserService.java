package com.example.application.services;

import com.example.application.supabase.SupabaseClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class UserService {
    private final SupabaseClient supabaseClient;
    @SuppressWarnings("unused")
    private final ObjectMapper objectMapper;

    @Autowired
    public UserService(SupabaseClient supabaseClient, ObjectMapper objectMapper) {
        this.supabaseClient = supabaseClient;
        this.objectMapper = objectMapper;
    }

    private static final java.util.function.Predicate<HttpStatusCode> IS_ERROR = HttpStatusCode::isError;

    public Mono<List<Map<String, Object>>> getAvailableUsers() {
        String url = supabaseClient.getProjectUrl() + "/rest/v1/profiles?select=id,username,email";

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
                        .flatMap(errorBody -> Mono.error(new RuntimeException("Failed to fetch users: " + errorBody))))
            .bodyToMono(List.class)
            .map(users -> {
                List<Map<String, Object>> userList = new ArrayList<>();
                for (Object user : users) {
                    if (user instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> userMap = (Map<String, Object>) user;
                        userList.add(userMap);
                    }
                }
                return userList;
            })
            .doOnSuccess(users -> System.out.println("Retrieved " + users.size() + " users"));
    }
}