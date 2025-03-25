package com.example.application.security;

import com.vaadin.flow.spring.security.AuthenticationContext;
import org.springframework.stereotype.Component;

@Component
public class SecurityService {

    private final AuthenticationContext authenticationContext;

    public SecurityService(AuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;
    }

    public String getAuthenticatedUsername() {
        return authenticationContext.getPrincipalName().orElse("Anonymous");
    }

    public void logout() {
        authenticationContext.logout();
    }
}