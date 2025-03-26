package com.example.application.views.chatapp;

import com.example.application.security.SecurityService;
import com.example.application.views.MainLayout;
import com.vaadin.collaborationengine.CollaborationAvatarGroup;
import com.vaadin.collaborationengine.CollaborationMessageList;
import com.vaadin.collaborationengine.MessageManager;
import com.vaadin.collaborationengine.UserInfo;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import com.vaadin.flow.component.textfield.TextField;
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

@PermitAll
@PageTitle("Conversation")
@Route(value = "", layout = MainLayout.class)
// @Menu(order = 1, icon = LineAwesomeIconUrl.COMMENTS) // Uncomment if LineAwesomeIconUrl is available
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
        private MessageManager messageManager;

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

        public void setMessageManager(MessageManager messageManager) {
            this.messageManager = messageManager;
        }

        public MessageManager getMessageManager() {
            return messageManager;
        }
    }

    private ChatInfo[] chats = new ChatInfo[] {
            new ChatInfo("General", 0), new ChatInfo("Support", 0),
            new ChatInfo("Casual", 0)
    };
    private ChatInfo currentChat = chats[0];
    private Tabs tabs;

    private CollaborationAvatarGroup avatarGroup;
    private UserInfo userInfo;
    private CollaborationMessageList list;
    @SuppressWarnings("unused")
    private transient MessageManager mm;

    public ChatAppView(SecurityService securityService) {
        this.securityService = securityService;
        addClassNames("chat-app-view", Width.FULL, Display.FLEX, Flex.AUTO);
        setSpacing(false);
        setPadding(false);

        // Initialize user info with JWT-based username
        String username = securityService.getAuthenticatedUsername(); // Default to "Anonymous" if null
        String jwt = securityService.getAuthenticatedUsername();
        System.out.println(jwt);
        if (jwt != null) {
            getUI().ifPresent(ui -> ui.navigate("login"));
        }
        userInfo = new UserInfo(UUID.randomUUID().toString(), username);

        tabs = new Tabs();
        for (ChatInfo chat : chats) {
            MessageManager mm = new MessageManager(this, userInfo, chat.getCollaborationTopic());
            chat.setMessageManager(mm);
            mm.setMessageHandler(context -> {
                if (currentChat != chat) {
                    chat.incrementUnread();
                }
            });
            tabs.add(createTab(chat));
        }
        tabs.setOrientation(Orientation.VERTICAL);
        tabs.addClassNames(Flex.GROW, Flex.SHRINK, Overflow.HIDDEN);

        list = new CollaborationMessageList(userInfo, currentChat.getCollaborationTopic());
        list.setSizeFull();

        VerticalLayout chatContainer = new VerticalLayout();
        chatContainer.addClassNames(Flex.AUTO, Overflow.HIDDEN);
        chatContainer.setPadding(false);
        chatContainer.setSpacing(false);

        HorizontalLayout inputLayout = new HorizontalLayout();
        inputLayout.setWidthFull();
        inputLayout.setSpacing(true);
        inputLayout.addClassName("input-layout");

        TextField messageInput = new TextField();
        messageInput.setPlaceholder("Type a message...");
        messageInput.setClearButtonVisible(true);
        messageInput.setWidthFull();
        messageInput.addClassName("chat-input");

        Button sendButton = new Button(new Icon(VaadinIcon.PAPERPLANE));
        sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        sendButton.addClickShortcut(com.vaadin.flow.component.Key.ENTER);
        sendButton.addClassName("chat-send-button");

        sendButton.addClickListener(e -> {
            String message = messageInput.getValue().trim();
            if (!message.isEmpty()) {
                currentChat.getMessageManager().submit(message);
                messageInput.clear();
                messageInput.focus();
            }
        });

        inputLayout.add(messageInput, sendButton);
        inputLayout.expand(messageInput);

        chatContainer.add(list, inputLayout);

        Aside side = new Aside();
        side.addClassNames(Display.FLEX, FlexDirection.COLUMN, Flex.GROW_NONE, Flex.SHRINK_NONE, Background.CONTRAST_5);
        side.setWidth("18rem");
        Header header = new Header();
        header.addClassNames(Display.FLEX, FlexDirection.ROW, Width.FULL, AlignItems.CENTER, Padding.MEDIUM,
                BoxSizing.BORDER);
        H3 channels = new H3("Channels");
        channels.addClassNames(Flex.GROW, Margin.NONE);

        if (avatarGroup == null) {
            avatarGroup = new CollaborationAvatarGroup(userInfo, "chat");
            avatarGroup.setMaxItemsVisible(4);
            avatarGroup.addClassNames(Width.AUTO);
        }

        header.add(channels, avatarGroup);

        side.add(header, tabs);

        add(chatContainer, side);
        setSizeFull();
        expand(chatContainer);

        tabs.addSelectedChangeListener(event -> {
            currentChat = ((ChatTab) event.getSelectedTab()).getChatInfo();
            currentChat.resetUnread();
            list.setTopic(currentChat.getCollaborationTopic());
        });
    }

    private ChatTab createTab(ChatInfo chat) {
        ChatTab tab = new ChatTab(chat);
        tab.addClassNames(JustifyContent.BETWEEN);

        Span badge = new Span();
        chat.setUnreadBadge(badge);
        badge.getElement().getThemeList().add("badge small contrast");
        tab.add(
            new Icon(chat.name.equals("General") ? VaadinIcon.MEGAPHONE :
                     chat.name.equals("Support") ? VaadinIcon.TOOLS :
                     chat.name.equals("Casual") ? VaadinIcon.COMMENT_ELLIPSIS : VaadinIcon.COMMENT_ELLIPSIS),
            new Span(chat.name),
            badge
        );
        return tab;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        Page page = attachEvent.getUI().getPage();
        page.retrieveExtendedClientDetails(details -> setMobile(details.getWindowInnerWidth() < 740));
        page.addBrowserWindowResizeListener(e -> setMobile(e.getWidth() < 740));
    }

    private void setMobile(boolean mobile) {
        tabs.setOrientation(mobile ? Orientation.HORIZONTAL : Orientation.VERTICAL);
    }
}