package com.example.application.views;

import com.example.application.services.AuthenticationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

@Route("login")
@PageTitle("Login")
@AnonymousAllowed
@StyleSheet("themes/chatappgroupproject/login-layout.css")

public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final AuthenticationService authenticationService;
    private final EmailField emailField = new EmailField("Email");
    private final PasswordField passwordField = new PasswordField("Password");
    private final Button loginButton = new Button("Login");
    private final Button signUpButton = new Button("Don't Have an Account?");

    public LoginView(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
        addClassName("login-view");
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        loginButton.addClickListener(event -> login(emailField.getValue(), passwordField.getValue()));
        signUpButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("signup")));
        
        HorizontalLayout hl = new HorizontalLayout();
        hl.addClassName("container-class-login-image");
        Div div = new Div("");
        div.setWidth("500px");
        div.setHeight("500px");
        div.addClassName("login-image");

        VerticalLayout loginLayout = new VerticalLayout(new H1("Login"),emailField, passwordField, loginButton, signUpButton);
        loginLayout.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        hl.add(loginLayout, div);
        add(hl);
    }

    private void login(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            Notification.show("Please fill all the fields",
                            3000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            JsonNode response = authenticationService.signInWithEmail(email, password).block();
            if (response != null && response.has("access_token")) {
                String accessToken = response.get("access_token").asText();
                VaadinSession.getCurrent().setAttribute("supabase.jwt", accessToken);
                System.out.println("JWT set in session: " + accessToken);

                JsonNode userNode = authenticationService.validateJWT(accessToken).block();
                if (userNode != null) {
                    String username = userNode.has("user_metadata") && userNode.get("user_metadata").has("username") 
                        ? userNode.get("user_metadata").get("username").asText() 
                        : email;
                        // ✅ Store username in VaadinSession
                    VaadinSession.getCurrent().setAttribute("username", username);
                    System.out.println("Username set in session: " + username);

                    UserDetails userDetails = User.builder()
                        .username(username)
                        .password("")
                        .roles("USER")
                        .build();
                    PreAuthenticatedAuthenticationToken auth = new PreAuthenticatedAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                    auth.setAuthenticated(true);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    System.out.println("Authentication set: " + SecurityContextHolder.getContext().getAuthentication().getName());
                    Notification.show("Login successful!",
                            3000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    getUI().ifPresent(ui -> ui.navigate("chatAI"));
                } else {
                    Notification.show("Authentication failed: Unable to validate user",
                            3000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            } else {
                Notification.show("Session Expired as Access Token Not Found",
                            3000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        } catch (Exception e) {
            if (e.getMessage().contains("Invalid login credentials")) {
                Notification.show("Authentication failed: Wrong Email/Password",
                3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
            else{
                Notification.show("Something went wrong!!", 3000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if (beforeEnterEvent.getLocation().getQueryParameters().getParameters().containsKey("error")) {
            Notification.show("Authentication error occurred", 3000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}