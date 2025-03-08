package com.example.application.views.chatapp;

import com.example.application.security.SecurityService;
import com.example.application.views.MainLayout;
import com.vaadin.collaborationengine.CollaborationAvatarGroup;
import com.vaadin.collaborationengine.CollaborationMessageInput;
import com.vaadin.collaborationengine.CollaborationMessageList;
import com.vaadin.collaborationengine.MessageManager;
import com.vaadin.collaborationengine.UserInfo;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Aside;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.Tabs.Orientation;
import com.vaadin.flow.component.textfield.TextField;
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
import com.example.application.services.GeminiService;
import org.springframework.beans.factory.annotation.Autowired;
import com.vaadin.flow.component.UI;
import reactor.core.publisher.Mono;

import java.util.UUID;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

@PermitAll
@PageTitle("Conversation")
@Route(value = "", layout = MainLayout.class)
@Menu(order = 1, icon = LineAwesomeIconUrl.COMMENTS)
public class ChatAppView extends HorizontalLayout {
    @Autowired
    private transient GeminiService geminiService;

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
            new ChatInfo("Casual", 0), new ChatInfo("ChatYAI", 0)
    };
    private ChatInfo currentChat = chats[0];
    private Tabs tabs;

    private CollaborationAvatarGroup avatarGroup;
    private UserInfo userInfo;
    
    private VerticalLayout chatContainer;
    private Component chatYAIComponent;
    private Component currentCollabComponent;
    private Aside side;
    private CollaborationMessageList list;

    public ChatAppView(SecurityService securityService) {
        this.securityService = securityService;
        addClassNames("chat-app-view", Width.FULL, Display.FLEX, Flex.AUTO);
        setSpacing(false);
        String username = "Guest";
        
        if (securityService.getAuthenticatedUser() != null) {
            username = securityService.getAuthenticatedUser().getUsername();
            userInfo = new UserInfo(UUID.randomUUID().toString(), username);
        } else {
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

        list = new CollaborationMessageList(userInfo, currentChat.getCollaborationTopic());
        list.setSizeFull();

        CollaborationMessageInput input = new CollaborationMessageInput(list);
        input.setWidthFull();

        chatContainer = new VerticalLayout();
        chatContainer.addClassNames(Flex.AUTO, Overflow.HIDDEN);
        chatContainer.add(list, input);
        
        chatYAIComponent = createChatYAIInterface();

        currentCollabComponent = chatContainer;

        side = new Aside();
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
            
            removeAll();
            
            if ("ChatYAI".equals(currentChat.name)) {
                add(chatYAIComponent, side);
                expand(chatYAIComponent);
                currentCollabComponent = chatYAIComponent;
            } else {
                add(chatContainer, side);
                expand(chatContainer);
                currentCollabComponent = chatContainer;
                list.setTopic(currentChat.getCollaborationTopic());
            }
        });
    }
    
    private Component createChatYAIInterface() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setSpacing(true);
        layout.setPadding(true);
        layout.setJustifyContentMode(JustifyContentMode.CENTER);
        layout.setAlignItems(Alignment.CENTER);
        
        Div mainContent = new Div();
        mainContent.setWidth("80%");
        mainContent.setMaxWidth("800px");
        mainContent.addClassNames(Display.FLEX, FlexDirection.COLUMN);
        mainContent.getStyle()
            .set("height", "100%")
            .set("background", "#000000")
            .set("color", "#FFFFFF");
        
        Div welcomeArea = new Div();
        welcomeArea.addClassNames(Width.FULL, Padding.LARGE);
        welcomeArea.getStyle()
            .set("background", "#000000")
            .set("border-radius", "var(--lumo-border-radius-l)")
            .set("box-shadow", "var(--lumo-box-shadow-s)")
            .set("margin-bottom", "var(--lumo-space-l)")
            .set("text-align", "center")
            .set("color", "#FFFFFF");
        
        H4 welcomeTitle = new H4("Welcome To ChatYAI 🎉");
        welcomeTitle.getStyle()
            .set("margin-top", "0")
            .set("font-size", "1.5em")
            .set("color", "#FFFFFF");
        
        Paragraph welcomeText = new Paragraph("Your AI-powered assistant is ready to help you. Ask any question to get started!");
        welcomeText.getStyle()
            .set("margin-bottom", "0")
            .set("color", "#FFFFFF");
        
        welcomeArea.add(welcomeTitle, welcomeText);
        
        Div messagesArea = new Div();
        messagesArea.addClassNames(Width.FULL, Flex.GROW);
        messagesArea.getStyle()
            .set("overflow", "auto")
            .set("padding", "var(--lumo-space-m)")
            .set("background", "#000000")
            .set("border-radius", "var(--lumo-border-radius-l)")
            .set("box-shadow", "var(--lumo-box-shadow-xs)")
            .set("flex", "1")
            .set("margin-bottom", "var(--lumo-space-m)")
            .set("min-height", "300px")
            .set("color", "#FFFFFF");
        
        HorizontalLayout inputLayout = new HorizontalLayout();
        inputLayout.setWidthFull();
        inputLayout.setSpacing(true);
        inputLayout.setAlignItems(Alignment.CENTER);
        inputLayout.getStyle()
            .set("background", "#000000")
            .set("border-radius", "var(--lumo-border-radius-l)")
            .set("padding", "var(--lumo-space-xs)")
            .set("box-shadow", "var(--lumo-box-shadow-s)")
            .set("color", "#FFFFFF");
        
        TextField messageInput = new TextField();
        messageInput.setPlaceholder("Ask ChatYAI something...");
        messageInput.setClearButtonVisible(true);
        messageInput.setWidthFull();
        messageInput.getStyle()
            .set("background", "rgba(0, 0, 0, 1)")
            .set("color", "rgb(255, 255, 255)")
            .set("border", "1px solid rgba(255, 255, 255, 0.3)");
        
        Button sendButton = new Button(new Icon(VaadinIcon.PAPERPLANE));
        sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        sendButton.addClickShortcut(Key.ENTER);
        sendButton.getStyle()
            .set("color", "#FFFFFF")
            .set("background", "rgba(255, 255, 255, 0.2)");
        
            sendButton.addClickListener(e -> {
                String message = messageInput.getValue().trim();
                if (!message.isEmpty()) {
                    addMessage(messagesArea, "You", message, false);
                    // Capture the UI instance before entering the async block
                    UI ui = UI.getCurrent();
                    
                    Div loading = new Div(new Span("Thinking..."));
                    messagesArea.add(loading);
                    
                    geminiService.getGeminiResponse(message)
                        .subscribe(response -> {
                            // Use the captured UI instance
                            ui.access(() -> {
                                messagesArea.remove(loading);
                                addMessage(messagesArea, "ChatYAI", response, true);
                            });
                        }, error -> {
                            // Use the captured UI instance
                            ui.access(() -> {
                                messagesArea.remove(loading);
                                addMessage(messagesArea, "System", 
                                    "Error: " + error.getMessage(), true);
                            });
                        });
                    
                    messageInput.clear();
                    messageInput.focus();
                }
            });
        
        inputLayout.add(messageInput, sendButton);
        inputLayout.expand(messageInput);
        
        mainContent.add(welcomeArea, messagesArea, inputLayout);
        
        layout.add(mainContent);
        layout.expand(mainContent);
        
        return layout;
    }
    
    private void addMessage(Div messagesArea, String sender, String message, boolean isAI) {
        Div messageDiv = new Div();
        messageDiv.addClassNames(Width.FULL);
        messageDiv.getStyle()
            .set("margin-bottom", "var(--lumo-space-m)")
            .set("color", "#FFFFFF");
        
        Div senderDiv = new Div();
        senderDiv.setText(sender);
        senderDiv.getStyle()
            .set("font-weight", "bold")
            .set("margin-bottom", "var(--lumo-space-xs)")
            .set("color", "#FFFFFF");
        
        Div contentDiv = new Div();
        contentDiv.setText(message);
        contentDiv.getStyle()
            .set("padding", "var(--lumo-space-s)")
            .set("border-radius", "var(--lumo-border-radius-m)")
            .set("color", "#FFFFFF");
        
        if (isAI) {
            contentDiv.getStyle()
                .set("background-color", "rgba(255, 255, 255, 0.1)")
                .set("border-left", "3px solid #4CAF50");
        } else {
            contentDiv.getStyle()
                .set("background-color", "rgba(255, 255, 255, 0.1)")
                .set("border-left", "3px solid #2196F3");
        }
        
        messageDiv.add(senderDiv, contentDiv);
        messagesArea.add(messageDiv);
        
        messagesArea.getElement().executeJs("this.scrollTop = this.scrollHeight");
    }

    private ChatTab createTab(ChatInfo chat) {
        ChatTab tab = new ChatTab(chat);
        tab.addClassNames(JustifyContent.BETWEEN);

        Span badge = new Span();
        chat.setUnreadBadge(badge);
        badge.getElement().getThemeList().add("badge small contrast");
        
        VaadinIcon icon;
        switch (chat.name) {
            case "General":
                icon = VaadinIcon.MEGAPHONE;
                break;
            case "Support":
                icon = VaadinIcon.TOOLS;
                break;
            case "Casual":
                icon = VaadinIcon.COMMENT_ELLIPSIS;
                break;
            case "ChatYAI":
                icon = VaadinIcon.CHAT;
                break;
            default:
                icon = VaadinIcon.COMMENT_ELLIPSIS;
        }
        
        tab.add(new Icon(icon), new Span(chat.name), badge);
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