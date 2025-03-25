package com.example.application.security;

import com.example.application.services.AuthenticationService;
import com.example.application.views.LoginView;
import com.fasterxml.jackson.databind.JsonNode;
import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import reactor.core.publisher.Mono;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@EnableWebSecurity
@Configuration
public class SecurityConfig extends VaadinWebSecurity {

    private final AuthenticationService authenticationService;

    @Value("${supabase.jwt.secret}") // Load from application.properties or environment variable
    private String authSecret;

    @Autowired
    public SecurityConfig(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // Permit static resources
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers(new AntPathRequestMatcher("/images/*.png")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/line-awesome/**/*.svg")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/login")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/signup")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/chatAI")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/upload")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/Conversation")).permitAll()
        );

        // Apply Vaadin's default configuration
        super.configure(http);

        // Configure stateless JWT authentication
        setStatelessAuthentication(http, new SecretKeySpec(Base64.getDecoder().decode(authSecret), JwsAlgorithms.HS256), "com.example.application");

        // Set login view
        setLoginView(http, LoginView.class);
    }

    @Bean
    public UserDetailsService users() {
        return new InMemoryUserDetailsManager() {
            @Override
            public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
                // Extract JWT from the Authorization header
                String jwt = SecurityContextHolder.getContext().getAuthentication() != null
                    ? (String) SecurityContextHolder.getContext().getAuthentication().getCredentials()
                    : null;
                System.out.println("loadUserByUsername called for: " + username + ", JWT: " + (jwt != null ? "present" : "null"));

                if (jwt != null) {
                    Mono<JsonNode> userResult = authenticationService.validateJWT(jwt);
                    JsonNode userNode = userResult.block();
                    if (userNode != null) {
                        String userUsername = userNode.has("user_metadata") && userNode.get("user_metadata").has("username")
                            ? userNode.get("user_metadata").get("username").asText()
                            : userNode.get("email").asText();
                        System.out.println("User validated: " + userUsername);
                        return User.builder()
                            .username(userUsername)
                            .password("") // No password needed with JWT
                            .roles("USER")
                            .build();
                    } else {
                        System.out.println("Invalid JWT response");
                        throw new UsernameNotFoundException("Invalid JWT");
                    }
                } else {
                    System.out.println("No JWT found in request");
                    throw new UsernameNotFoundException("User not authenticated");
                }
            }
        };
    }
}