package com.example.application.supabase;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class SupabaseClient {

  private final WebClient webClient;

  @Value("${supabase.projectUrl}") // Inject from application.properties
  private String projectUrl;

  @Value("${supabase.apiKey}") // Inject from application.properties
  private String apiKey;

  @Value("${supabase.jwt.secret}")
  private String authSecret;
  
  public SupabaseClient(WebClient.Builder webClientBuilder) {
    this.webClient = webClientBuilder.build();
  }

  public WebClient getWebClient() {
    return webClient;
  }

  public String getProjectUrl() {
    return projectUrl;
  }

  public String getApiKey() {
    return apiKey;
  }
}
