package com.example.application.views;

import com.example.application.security.SecurityService;
import com.example.application.services.ChatMessageService;
import com.example.application.services.UserService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
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
import com.vaadin.flow.component.DetachEvent;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "private-chat/:username?", layout = MainLayout.class)
@PageTitle("Private Chat")
@AnonymousAllowed
@CssImport("./themes/chatappgroupproject/private-chat.css")
public class PrivateChatView extends HorizontalLayout {

    private final VerticalLayout userListLayout = new VerticalLayout();
    private final VerticalLayout chatContainer = new VerticalLayout();
    private final VerticalLayout messageContainer = new VerticalLayout();
    private final HorizontalLayout chatHeader = new HorizontalLayout();
    private final TextField messageField = new TextField();
    private final Button sendButton = new Button(new Icon(VaadinIcon.PAPERPLANE));

    private final Map<String, List<ChatMessageService.ChatMessage>> userMessages = new HashMap<>();
 private final Map<String, Integer> unreadCounts = new HashMap<>();
    private final Grid<User> userGrid = new Grid<>(User.class);
    private String currentRecipient;

    private final UserService userService;
    private final ChatMessageService chatMessageService;
    private final SecurityService securityService;
    private Registration messageRegistration;

    @Autowired
    public PrivateChatView(UserService userService, ChatMessageService chatMessageService, SecurityService securityService) {
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
        userListLayout.add(userGrid);
        userListLayout.addClassName("user-list");

        // Chat Section
        VerticalLayout chatContent = new VerticalLayout();
        chatContent.addClassName("chat-content");
        chatContent.setSizeFull();
        chatContent.setSpacing(true);
        chatContent.setPadding(true);

        chatContainer.addClassName("chat-container");
        chatContainer.setHeight("100%");
        chatContainer.setVisible(false);

        // Setup chat header
        setupChatHeader();

        // Setup message container
        messageContainer.addClassName("chat-content");
        messageContainer.getStyle().set("flex-grow", "1");
        messageContainer.getStyle().set("overflow-y", "auto");
        messageContainer.setPadding(true);

        // Setup input area
        messageField.setPlaceholder("Type a message...");
        messageField.addClassName("message-input");
        messageField.addKeyPressListener(Key.ENTER, event -> sendMessage(messageField.getValue()));
        sendButton.addClassName("send-button");
        sendButton.addClickListener(event -> sendMessage(messageField.getValue()));

        HorizontalLayout inputArea = new HorizontalLayout(messageField, sendButton);
        inputArea.addClassName("input-area");
        inputArea.setWidth("100%");

        chatContainer.add(chatHeader, messageContainer, inputArea);

        add(userListLayout, chatContainer);
    }

    private void setupUserGrid() {
        userGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
        userGrid.removeAllColumns();
        userGrid.addComponentColumn(user -> {
            HorizontalLayout userLayout = new HorizontalLayout();
            Avatar avatar = new Avatar(user.getUsername());
            Span username = new Span(user.getUsername());

            // Add notification badge if there are unread messages
            if (unreadCounts.containsKey(user.getUsername()) && unreadCounts.get(user.getUsername()) > 0) {
                Span badge = new Span(String.valueOf(unreadCounts.get(user.getUsername())));
                badge.getStyle().set("background-color", "var(--lumo-error-color)")
                    .set("color", "white")
                    .set("border-radius", "50%")
                    .set("padding", "2px 6px")
                    .set("font-size", "0.8em");
                userLayout.add(avatar, username, badge);
            } else {
                userLayout.add(avatar, username);
            }
            return userLayout;
        }).setHeader("Users");

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
        chatHeader.addClassName("chat-header");
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
            chatContainer.setVisible(true);
            updateChatHeader(recipientUsername);

            // Load chat history
            List<ChatMessageService.ChatMessage> history = chatMessageService.getChatHistory(getUsername(), recipientUsername);
            userMessages.put(recipientUsername, new ArrayList<>(history));
            displayMessages();
        }
    }

    private void sendMessage(String text) {
        if (!text.trim().isEmpty() && currentRecipient != null) {
            chatMessageService.sendMessage(
                getUsername(),
                currentRecipient,
                text
            );
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
                messageWrapper.addClassName("message-bubble");

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
                            Notification.show("New message from " + message.getFrom(), 3000, Notification.Position.TOP_CENTER);
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
        bubble.addClassName(isUser ? "user-message" : "receiver-message");
        
        // Create message content
        Div messageContent = new Div();
        messageContent.setText(message);
        messageContent.getStyle()
            .set("word-break", "break-word")
            .set("white-space", "pre-wrap")
            .set("padding", "8px 12px");

        // Add timestamp below message
        String formattedTime = timestamp.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        Span timeSpan = new Span(formattedTime);
        timeSpan.getStyle()
            .set("font-size", "0.7em")
            .set("color", isUser ? "rgba(255,255,255,0.7)" : "var(--lumo-secondary-text-color)")
            .set("margin-top", "2px")
            .set("display", "block")
            .set("text-align", isUser ? "right" : "left");

        // Add message status for user messages
        if (isUser) {
            Icon statusIcon = new Icon(VaadinIcon.CHECK);
            statusIcon.getStyle()
                .set("font-size", "0.7em")
                .set("color", "rgba(255,255,255,0.7)")
                .set("margin-left", "4px");
            timeSpan.add(statusIcon);
        }

        VerticalLayout messageLayout = new VerticalLayout(messageContent, timeSpan);
        messageLayout.setSpacing(false);
        messageLayout.setPadding(false);
        messageLayout.getStyle().set("width", "100%");
        
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
