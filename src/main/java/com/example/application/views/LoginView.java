package com.example.application.views;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin.Minus.Horizontal;

@Route("login")
@PageTitle("Login")
@AnonymousAllowed
@StyleSheet("themes/chatappgroupproject/login-layout.css")
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm login = new LoginForm();

    public LoginView() {
        addClassName("login-view");
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        login.addClassName("login-custom-form");
        login.setAction("login");

        add(new H1("Login Form"));
        add(new Span("Username: user, Password: password"));
        add(new Span("Username: admin, Password: password"));
        HorizontalLayout hl=new HorizontalLayout();
        hl.addClassName("container-class-login-image");
        Image img = new Image();
        img.setWidth("500px");
        img.setHeight("500px");
        img.addClassName("login-image");
        hl.add(login,img);
        add(hl);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        // inform the user about an authentication error
        if (beforeEnterEvent.getLocation()
                .getQueryParameters()
                .getParameters()
                .containsKey("error")) {
            login.setError(true);
        }
    }
}