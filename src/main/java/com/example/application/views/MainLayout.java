package com.example.application.views;

import com.example.application.security.SecurityService;
import com.example.application.views.chatapp.ChatAppView;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.flow.theme.lumo.LumoUtility.Width;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

public class MainLayout extends AppLayout {
    private final SecurityService securityService;

    public MainLayout(SecurityService securityService) {
        this.securityService = securityService;
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        H1 logo = new H1("Conversation");
        logo.addClassNames(
                LumoUtility.FontSize.LARGE,
                LumoUtility.Margin.MEDIUM);

        var header = new HorizontalLayout(new DrawerToggle(), logo);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(logo); // Expands the logo to fill the space
        header.setWidthFull();
        header.addClassNames(
                LumoUtility.Padding.Vertical.NONE,
                LumoUtility.Padding.Horizontal.MEDIUM);

        addToNavbar(header); // This stays as is

    }

    private void createDrawer() {
 
        // Ensure user is authenticated and handle null values gracefully
        String username = "Guest";
        if (securityService.getAuthenticatedUser() != null) {
            username = securityService.getAuthenticatedUser().getUsername();
        }

        Button logout = new Button("Log out ", e -> securityService.logout());
        logout.addClassName("logout-Button");
        HorizontalLayout user = new HorizontalLayout(new Icon(VaadinIcon.HEADSET),new H3(username));
        user.addClassName("User-Name");
        VerticalLayout avatarAndName = new VerticalLayout(logout);
        avatarAndName.addClassNames(LumoUtility.Margin.Top.AUTO, Width.FULL);
        // Span appName = new Span("Fun Hub");
        
        // appName.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.FontSize.LARGE);
        Header header = new Header(user);
        // Chat Link
        HorizontalLayout chatLink = new HorizontalLayout(new Icon(VaadinIcon.CHAT),
                new RouterLink("Chat", ChatAppView.class));
        chatLink.addClassName("Chat-Link-Hz");

        // File Upload Link
        HorizontalLayout uploadLink = new HorizontalLayout(new Icon(VaadinIcon.UPLOAD),
                new RouterLink("File Upload", FileUploadView.class));
        uploadLink.addClassName("Chat-Link-Hz");

        // File Upload Link
        HorizontalLayout AIChatLink = new HorizontalLayout(new Icon(VaadinIcon.UPLOAD),
                new RouterLink("Chat AI", ChatWithAIView.class));
        AIChatLink.addClassName("Chat-Link-Hz");
        // Combine both links
        VerticalLayout drawerContent = new VerticalLayout(chatLink, uploadLink,AIChatLink, avatarAndName);
        drawerContent.addClassName("linkUsernameLogout");
        drawerContent.setSpacing(false); // Prevent spacing between the navigation and logout
        drawerContent.setSizeFull(); // Ensure it fills the available space
        addToDrawer(header, new Scroller(drawerContent));
    }
}