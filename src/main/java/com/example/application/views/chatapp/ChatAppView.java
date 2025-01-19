package com.example.application.views.chatapp;

import com.example.application.security.SecurityService;
import com.example.application.views.MainLayout;
import com.vaadin.collaborationengine.CollaborationAvatarGroup;
import com.vaadin.collaborationengine.CollaborationMessageInput;
import com.vaadin.collaborationengine.CollaborationMessageList;
import com.vaadin.collaborationengine.MessageManager;
import com.vaadin.collaborationengine.UserInfo;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Aside;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.Tabs.Orientation;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility.AlignItems;
import com.vaadin.flow.theme.lumo.LumoUtility.Background;
import com.vaadin.flow.theme.lumo.LumoUtility.BoxSizing;
import com.vaadin.flow.theme.lumo.LumoUtility.Display;
import com.vaadin.flow.theme.lumo.LumoUtility.Flex;
import com.vaadin.flow.theme.lumo.LumoUtility.FlexDirection;
import com.vaadin.flow.theme.lumo.LumoUtility.JustifyContent;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin;
import com.vaadin.flow.theme.lumo.LumoUtility.Overflow;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;
import com.vaadin.flow.theme.lumo.LumoUtility.Width;
import jakarta.annotation.security.PermitAll;

import java.util.UUID;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

@PermitAll
@PageTitle("Conversation")
@Route(value = "", layout = MainLayout.class) // <1>
@Menu(order = 1, icon = LineAwesomeIconUrl.COMMENTS)
public class ChatAppView extends HorizontalLayout {
    @SuppressWarnings("unused")
    private final SecurityService securityService;
    
    public static class ChatTab extends Tab {
        private final ChatInfo chatInfo;

        public ChatTab(ChatInfo chatInfo) {
            this.chatInfo = chatInfo;
        }

        public ChatInfo getChatInfo() {
            return chatInfo;
        }
    }

    public static class ChatInfo {
        private String name;
        private int unread;
        private Span unreadBadge;

        private ChatInfo(String name, int unread) {
            this.name = name;
            this.unread = unread;
        }

        public void resetUnread() {
            unread = 0;
            updateBadge();
        }

        public void incrementUnread() {
            unread++;
            updateBadge();
        }

        private void updateBadge() {
            unreadBadge.setText(unread + "");
            unreadBadge.setVisible(unread != 0);
        }

        public void setUnreadBadge(Span unreadBadge) {
            this.unreadBadge = unreadBadge;
            updateBadge();
        }

        public String getCollaborationTopic() {
            return "chat/" + name;
        }
    }

    private ChatInfo[] chats = new ChatInfo[] {
            new ChatInfo("General", 0), new ChatInfo("Support", 0),
            new ChatInfo("Casual", 0)
    };
    private ChatInfo currentChat = chats[0];
    private Tabs tabs;

    private CollaborationAvatarGroup avatarGroup; // Only create once
    private UserInfo userInfo; // Hold the user info

    public ChatAppView(SecurityService securityService) {
        this.securityService = securityService;
        addClassNames("chat-app-view", Width.FULL, Display.FLEX, Flex.AUTO);
        setSpacing(false);
        String username = "Guest";
        
        // Initialize the user info only once
        if (securityService.getAuthenticatedUser() != null) {
            username = securityService.getAuthenticatedUser().getUsername();
            userInfo = new UserInfo(UUID.randomUUID().toString(), username);
        }

        tabs = new Tabs();
        for (ChatInfo chat : chats) {
            MessageManager mm = new MessageManager(this, userInfo, chat.getCollaborationTopic());
            mm.setMessageHandler(context -> {
                if (currentChat != chat) {
                    chat.incrementUnread();
                }
            });
            tabs.add(createTab(chat));
        }
        tabs.setOrientation(Orientation.VERTICAL);
        tabs.addClassNames(Flex.GROW, Flex.SHRINK, Overflow.HIDDEN);

        CollaborationMessageList list = new CollaborationMessageList(userInfo, currentChat.getCollaborationTopic());
        list.setSizeFull();

        CollaborationMessageInput input = new CollaborationMessageInput(list);
        input.setWidthFull();

        VerticalLayout chatContainer = new VerticalLayout();
        chatContainer.addClassNames(Flex.AUTO, Overflow.HIDDEN);

        Aside side = new Aside();
        side.addClassNames(Display.FLEX, FlexDirection.COLUMN, Flex.GROW_NONE, Flex.SHRINK_NONE, Background.CONTRAST_5);
        side.setWidth("18rem");
        Header header = new Header();
        header.addClassNames(Display.FLEX, FlexDirection.ROW, Width.FULL, AlignItems.CENTER, Padding.MEDIUM,
                BoxSizing.BORDER);
        H3 channels = new H3("Channels");
        channels.addClassNames(Flex.GROW, Margin.NONE);

        // Create the avatar group only once to avoid double entries
        if (avatarGroup == null) {
            avatarGroup = new CollaborationAvatarGroup(userInfo, "chat");
            avatarGroup.setMaxItemsVisible(4);
            avatarGroup.addClassNames(Width.AUTO);
        }

        header.add(channels, avatarGroup);

        side.add(header, tabs);

        chatContainer.add(list, input);
        add(chatContainer, side);
        setSizeFull();
        expand(list);

        // Change the topic id of the chat when a new tab is selected
        tabs.addSelectedChangeListener(event -> {
            currentChat = ((ChatTab) event.getSelectedTab()).getChatInfo();
            currentChat.resetUnread();
            list.setTopic(currentChat.getCollaborationTopic());
        });
    }

    private ChatTab createTab(ChatInfo chat) {
        ChatTab tab = new ChatTab(chat);
        tab.addClassNames(JustifyContent.BETWEEN);

        // Apply unique styling for each chat tab
        switch (chat.name) {
            case "General":
                tab.addClassName("tab-general");
                break;
            case "Support":
                tab.addClassName("tab-support");
                break;
            case "Casual":
                tab.addClassName("tab-casual");
                break;
        }

        Span badge = new Span();
        chat.setUnreadBadge(badge);
        badge.getElement().getThemeList().add("badge small contrast");
        tab.add(new Icon(chat.name=="General"?VaadinIcon.MEGAPHONE:chat.name=="Support"?VaadinIcon.TOOLS:chat.name=="Casual"?VaadinIcon.COMMENT_ELLIPSIS:VaadinIcon.COMMENT_ELLIPSIS),new Span(chat.name), badge);
        return tab;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        Page page = attachEvent.getUI().getPage();
        page.retrieveExtendedClientDetails(details -> {
            setMobile(details.getWindowInnerWidth() < 740);
        });
        page.addBrowserWindowResizeListener(e -> {
            setMobile(e.getWidth() < 740);
        });
    }

    private void setMobile(boolean mobile) {
        tabs.setOrientation(mobile ? Orientation.HORIZONTAL : Orientation.VERTICAL);
    }
}
