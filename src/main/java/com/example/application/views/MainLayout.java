package com.example.application.views;

import com.example.application.security.SecurityService;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.flow.theme.lumo.LumoUtility.Width;

import java.util.List;
import java.util.UUID;

import com.vaadin.collaborationengine.CollaborationAvatarGroup;
import com.vaadin.collaborationengine.UserInfo;

@Layout
@AnonymousAllowed
public class MainLayout extends AppLayout {

    private final SecurityService securityService;
    private H1 viewTitle = new H1();

    public MainLayout(SecurityService securityService) {
        this.securityService = securityService;
        setPrimarySection(Section.DRAWER);
        addDrawerContent();
        addHeaderContent();
    }

    private void addHeaderContent() {
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

    private void addDrawerContent() {
        Span appName = new Span("My App");
        appName.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.FontSize.LARGE);
        Header header = new Header(appName);

        // Ensure user is authenticated and handle null values gracefully
        String username = "Guest";
        if (securityService.getAuthenticatedUser() != null) {
            username = securityService.getAuthenticatedUser().getUsername();
        }

        Button logout = new Button("Log out ", e -> securityService.logout());
        logout.addClassName("logout-Button");
        H3 user=new H3(username);
        user.addClassName("User-Name");
        HorizontalLayout avatarAndName = new HorizontalLayout(user, logout);
        avatarAndName.addClassNames(LumoUtility.Margin.Top.AUTO,Width.FULL);
        VerticalLayout drawerContent = new VerticalLayout(createNavigation(), avatarAndName, logout);
        drawerContent.setSpacing(false); // Prevent spacing between the navigation and logout
        drawerContent.setSizeFull(); // Ensure it fills the available space

        addToDrawer(header, new Scroller(drawerContent), createFooter());
    }

    private SideNav createNavigation() {
        SideNav nav = new SideNav();

        List<MenuEntry> menuEntries = MenuConfiguration.getMenuEntries();
        menuEntries.forEach(entry -> {
            if (entry.icon() != null) {
                nav.addItem(new SideNavItem(entry.title(), entry.path(), new SvgIcon(entry.icon())));
            } else {
                nav.addItem(new SideNavItem(entry.title(), entry.path()));
            }
        });

        return nav;
    }

    private Footer createFooter() {
        Footer layout = new Footer();
        return layout;
    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        viewTitle.setText(getCurrentPageTitle());
    }

    private String getCurrentPageTitle() {
        return MenuConfiguration.getPageHeader(getContent()).orElse("Default Title");
    }
}
