package com.example.application.views;

import com.example.application.security.SecurityService;
import com.example.application.services.ChatMessageService;
import com.example.application.services.UserService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Aside;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility.Background;
import com.vaadin.flow.theme.lumo.LumoUtility.Display;
import com.vaadin.flow.theme.lumo.LumoUtility.Flex;
import com.vaadin.flow.theme.lumo.LumoUtility.FlexDirection;
import com.vaadin.flow.component.DetachEvent;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "private-chat/:username?", layout = MainLayout.class)
@PageTitle("Private Chat")
@AnonymousAllowed
@CssImport("themes/chatappgroupproject/views/private-chat-view.css")
public class PrivateChatView extends HorizontalLayout {
    private final Aside userListAsideLayout = new Aside();
    private final VerticalLayout chatContainer = new VerticalLayout();
    private final VerticalLayout messageContainer = new VerticalLayout();
    private final HorizontalLayout chatHeader = new HorizontalLayout();
    private final TextField messageField = new TextField();
    private final Button sendButton = new Button(new Icon(VaadinIcon.PAPERPLANE));
    private final HorizontalLayout inputArea = new HorizontalLayout();
    private final Map<String, List<ChatMessageService.ChatMessage>> userMessages = new HashMap<>();
    private final Map<String, Integer> unreadCounts = new HashMap<>();
    private final Grid<User> userGrid = new Grid<>(User.class);
    private String currentRecipient;

    private final UserService userService;
    private final ChatMessageService chatMessageService;
    private final SecurityService securityService;
    private Registration messageRegistration;

    @Autowired
    public PrivateChatView(UserService userService, ChatMessageService chatMessageService,
            SecurityService securityService) {
        this.userService = userService;
        this.chatMessageService = chatMessageService;
        this.securityService = securityService;
        setSizeFull();
        initializeView();
        setupMessageListener();
    }

    private void initializeView() {
        addClassName("private-chat-view");

        // User List Section
        setupUserGrid();
        userListAsideLayout.add(userGrid);
        userListAsideLayout.addClassNames(Display.FLEX, FlexDirection.COLUMN, Flex.GROW_NONE, Flex.SHRINK_NONE,
                Background.CONTRAST_5);
        userListAsideLayout.setWidth("20rem");
        userListAsideLayout.addClassName("private-user-list");

        // Chat Section
        VerticalLayout chatContent = new VerticalLayout();
        chatContent.addClassName("private-chat-content");
        chatContent.setSizeFull();
        chatContent.setSpacing(true);
        chatContent.setPadding(true);
        chatContainer.addClassName("private-chat-container");
        chatContainer.setHeight("100%");
        chatContainer.setWidth("100%");

        // Setup chat header
        setupChatHeader();

        // Setup message container
        messageContainer.addClassName("private-chat-content");
        messageContainer.getStyle().set("flex-grow", "1");
        messageContainer.getStyle().set("overflow-y", "auto");
        messageContainer.setPadding(true);

        // Setup input area
        HorizontalLayout inputLayout = new HorizontalLayout();
        inputLayout.setWidthFull();
        inputLayout.setSpacing(true);
        inputLayout.setAlignItems(Alignment.CENTER);
        inputLayout.addClassName("private-input-layout");

        messageField.setPlaceholder("Type a message...");
        messageField.addClassName("private-message-input");
        messageField.addKeyPressListener(Key.ENTER, event -> sendMessage(messageField.getValue()));
        sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        sendButton.addClickShortcut(Key.ENTER);
        sendButton.addClassName("private-send-button");
        sendButton.addClickListener(event -> sendMessage(messageField.getValue()));

        inputArea.add(messageField, sendButton);
        inputArea.addClassName("private-input-area");
        inputArea.setWidth("100%");

        add(chatContainer, userListAsideLayout);
    }

    private void setupUserGrid() {
        userGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
        userGrid.removeAllColumns();
        userGrid.addClassName("private-user-grid");
        HorizontalLayout usersListsHeader=new HorizontalLayout();
        usersListsHeader.add(new Icon(VaadinIcon.USERS));
        usersListsHeader.add("Users List");
        usersListsHeader.addClassName("shadow-usersListsHeader");
        userGrid.addComponentColumn(user -> {
            HorizontalLayout userLayout = new HorizontalLayout();
            userLayout.addClassName("user-item"); // Added class
            Avatar avatar = new Avatar(user.getUsername());
            avatar.addClassName("user-avatar"); // Added class
            Span username = new Span(user.getUsername());
            username.addClassName("user-name"); // Added class

            // Add notification badge if there are unread messages
            if (unreadCounts.containsKey(user.getUsername()) && unreadCounts.get(user.getUsername()) > 0) {
                Span badge = new Span(String.valueOf(unreadCounts.get(user.getUsername())));
                badge.addClassName("unread-badge"); // Replaced inline styles with class
                userLayout.add(avatar, username, badge);
            } else {
                userLayout.add(avatar, username);
            }
            return userLayout;
        }).setHeader(usersListsHeader).addClassName("user-grid-header"); // Added class to header
        
        
        userGrid.addItemClickListener(event -> {
            String recipientUsername = event.getItem().getUsername();
            openChatWithUser(recipientUsername);
        });

        loadUsers();
    }

    private void loadUsers() {
        userService.getAvailableUsers()
                .subscribe(users -> {
                    List<User> userList = users.stream()
                            .map(userMap -> new User(
                                    (String) userMap.get("username"),
                                    "https://ui-avatars.com/api/?name=" + userMap.get("username")))
                            .toList();
                    getUI().ifPresent(ui -> ui.access(() -> userGrid.setItems(userList)));
                }, error -> {
                    System.err.println("Error loading users: " + error.getMessage());
                });
    }

    private void setupChatHeader() {
        chatHeader.addClassName("private-chat-header");
        chatHeader.setWidth("100%");
        chatHeader.setPadding(true);
        chatHeader.setSpacing(true);
        chatHeader.setAlignItems(Alignment.CENTER);

        // Initially empty, will be populated when a user is selected
        chatHeader.setVisible(false);
    }

    private void openChatWithUser(String recipientUsername) {
        if (recipientUsername != null) {
            currentRecipient = recipientUsername;
            chatContainer.add(chatHeader, messageContainer, inputArea);
            updateChatHeader(recipientUsername);

            // Load chat history
            List<ChatMessageService.ChatMessage> history = chatMessageService.getChatHistory(getUsername(),
                    recipientUsername);
            userMessages.put(recipientUsername, new ArrayList<>(history));
            displayMessages();
        }
    }

    private void sendMessage(String text) {
        if (!text.trim().isEmpty() && currentRecipient != null) {
            chatMessageService.sendMessage(
                    getUsername(),
                    currentRecipient,
                    text);
            messageField.clear();
        }
    }

    private void displayMessages() {
        messageContainer.removeAll();
        if (currentRecipient != null && userMessages.containsKey(currentRecipient)) {
            // Reset unread count when opening chat
            unreadCounts.put(currentRecipient, 0);
            List<ChatMessageService.ChatMessage> messages = userMessages.get(currentRecipient);
            String currentUser = getUsername();

            for (ChatMessageService.ChatMessage message : messages) {
                boolean isUser = message.getFrom().equals(currentUser);
                VerticalLayout messageWrapper = new VerticalLayout();
                messageWrapper.setSpacing(false);
                messageWrapper.setPadding(false);
                messageWrapper.addClassName("private-message-bubble");

                Div bubble = createMessageBubble(message.getContent(), isUser, message.getTimestamp());
                messageWrapper.add(bubble);
                messageContainer.add(messageWrapper);

                if (isUser) {
                    // Add message status for user messages
                    Span status = new Span();
                    status.getStyle()
                            .set("font-size", "0.8em")
                            .set("color", "var(--lumo-secondary-text-color)")
                            .set("margin-left", "4px");
                    status.addClassName("message-read-status");
                    Icon statusIcon = switch (message.getStatus()) {
                        case SENT -> VaadinIcon.CHECK.create();
                        case DELIVERED -> VaadinIcon.CHECK_CIRCLE.create();
                        case READ -> VaadinIcon.CHECK_CIRCLE_O.create();
                    };
                    status.add(statusIcon);
                    bubble.add(status);
                }
            }
            // Scroll to bottom
            messageContainer.getElement().executeJs("this.scrollTop = this.scrollHeight");
        }
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (messageRegistration != null) {
            messageRegistration.remove();
            messageRegistration = null;
        }
        super.onDetach(detachEvent);
    }

    private void setupMessageListener() {
        String username = getUsername();
        if (username != null && messageRegistration == null) {
            messageRegistration = chatMessageService.registerUser(username, message -> {
                getUI().ifPresent(ui -> ui.access(() -> {
                    // Store message
                    String otherUser = message.getFrom().equals(username) ? message.getTo() : message.getFrom();
                    userMessages.computeIfAbsent(otherUser, k -> new ArrayList<>()).add(message);

                    // Update unread count if message is not from current user
                    if (!message.getFrom().equals(username)) {
                        unreadCounts.put(otherUser, unreadCounts.getOrDefault(otherUser, 0) + 1);
                    }

                    // Show notification to receiver when they're not viewing the chat
                    if (currentRecipient == null || !currentRecipient.equals(message.getFrom())) {
                        if (message.getTo().equals(getUsername())) {
                            Notification.show("New message from " + message.getFrom(), 3000,
                                    Notification.Position.TOP_CENTER);
                        }
                    }

                    // Update UI if this is the current chat
                    if (currentRecipient != null &&
                            (currentRecipient.equals(message.getFrom()) || currentRecipient.equals(message.getTo()))) {
                        displayMessages();
                    }
                }));
            });
        }
    }

    private String getUsername() {
        return securityService.getAuthenticatedUsername();
    }

    private Div createMessageBubble(String message, boolean isUser, java.time.LocalDateTime timestamp) {
        Div bubble = new Div();
        bubble.addClassName(isUser ? "private-user-message" : "private-receiver-message");

        // Create message content
        Div messageContent = new Div();
        messageContent.setText(message);
        messageContent.addClassName("private-inner-message-content");

        // Add timestamp below message
        String formattedTime = timestamp.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        Span timeSpan = new Span(formattedTime);
        timeSpan.addClassName("message-timestamp");

        // Add message status for user messages
        // if (isUser) {
        // Icon statusIcon = new Icon(VaadinIcon.CHECK);
        // statusIcon.addClassName("message-status-icon");
        // timeSpan.add(statusIcon);
        // }

        VerticalLayout messageLayout = new VerticalLayout(messageContent, timeSpan);
        messageLayout.addClassName("message-layout");

        bubble.add(messageLayout);
        return bubble;
    }

    public static class User {
        private final String username;
        private final String avatarUrl;

        public User(String username, String avatarUrl) {
            this.username = username;
            this.avatarUrl = avatarUrl;
        }

        public String getUsername() {
            return username;
        }

        public String getAvatarUrl() {
            return avatarUrl;
        }
    }

    private void updateChatHeader(String recipientUsername) {
        chatHeader.removeAll();
        chatHeader.setVisible(true);

        Avatar recipientAvatar = new Avatar(recipientUsername);
        Span recipientName = new Span(recipientUsername);
        recipientName.getStyle().set("font-weight", "bold");

        chatHeader.add(recipientAvatar, recipientName);
    }
}
