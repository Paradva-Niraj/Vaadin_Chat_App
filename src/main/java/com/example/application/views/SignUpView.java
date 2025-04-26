package com.example.application.views;

import com.example.application.services.AuthenticationService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.Map;
import com.vaadin.flow.component.dependency.CssImport;
@Route("signup")
@PageTitle("Sign Up")
@AnonymousAllowed
@CssImport("themes/chatappgroupproject/signup-layout.css")
public class SignUpView extends VerticalLayout {

    private final AuthenticationService authenticationService;
    private final EmailField emailField = new EmailField("Email");
    private final TextField usernameField = new TextField("Username");
    private final PasswordField passwordField = new PasswordField("Password");
    private final IntegerField ageField = new IntegerField("Age");
    private final ComboBox<String> genderComboBox = new ComboBox<>("Gender");
    private final Button signUpButton = new Button("Sign Up");
    private final Button loginRedirectButton = new Button("Already have an account? Log In");

    public SignUpView(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;

        genderComboBox.setItems("Male", "Female", "Other");
        genderComboBox.addClassName("gender_at_sign_up");
        ageField.setMin(1);
        ageField.setMax(150);
        loginRedirectButton.addClassName("already_have_account");

        // Add click listener to redirect to login page
        loginRedirectButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("login")));
        signUpButton.addClickListener(event -> signUp());

        addClassName("signup-view");
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        HorizontalLayout hl = new HorizontalLayout();
        hl.addClassName("container-class-signup-image");

        // Signup image on the left
        Div signupImage = new Div();
        signupImage.setWidth("1250px");
        signupImage.setHeight("450px");
        signupImage.addClassName("signup-image");

        // Form layout on the right
        VerticalLayout signupLayout = new VerticalLayout(
                new H1("Sign Up"),
                new HorizontalLayout(emailField, usernameField),
                passwordField,
                new HorizontalLayout(ageField, genderComboBox),
                signUpButton,
                loginRedirectButton);
        signupLayout.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        signupLayout.addClassName("inputs-container-signup");
        hl.add(signupImage, signupLayout);
        add(hl);
    }

    private void signUp() {
        String email = emailField.getValue();
        String username = usernameField.getValue();
        String password = passwordField.getValue();
        Integer age = ageField.getValue();
        String gender = genderComboBox.getValue();

        if (email == null || email.isEmpty() || username == null || username.isEmpty()
                || password == null || password.isEmpty() || age == null || gender == null || gender.isEmpty()) {
            Notification.show("Please fill all the fields",
                            3000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            @SuppressWarnings("rawtypes")
            Map response = authenticationService.signUp(email, password, username, age, gender).block();
            if (response != null && response.containsKey("user")) {
                @SuppressWarnings("rawtypes")
                Map user = (Map) response.get("user");
                if (user != null && user.containsKey("id")) {
                    Notification.show("Sign up successful! Please log in.",
                    3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    getUI().ifPresent(ui -> ui.navigate("login"));
                } else {
                    Notification.show("Sign up failed: User ID missing in response.",
                    3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            } else {
                Notification.show("Sign up failed: Invalid response from server.",
                3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        } catch (Exception e) {
            if (e.getMessage().equals("Username is already taken")) {
                Notification.show("Username '" + username + "' is already taken. Please choose a different one.",
                3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            } 
            else if (e.getMessage().contains("User already registered")) {
                Notification.show("Email is already registered | Try Login",
                3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
            else if (e.getMessage().contains("Unable to validate email address: invalid format")) {
                Notification.show("Wrong Email Format",
                3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);   
            }
            else {
                Notification.show("Something Unexpected happened: ",
                3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        }
    }
}
