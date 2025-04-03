package com.example.application.services;

import com.vaadin.flow.shared.Registration;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatMessageService {
    private final Map<String, Sinks.Many<ChatMessage>> userSinks = new ConcurrentHashMap<>();
    private final Map<String, List<ChatMessage>> messageHistory = new ConcurrentHashMap<>();

    private String getChatKey(String user1, String user2) {
        return user1.compareTo(user2) < 0 ? user1 + "_" + user2 : user2 + "_" + user1;
    }

    public void sendMessage(String from, String to, String message) {
        // Prevent self-messaging
        if (from.equals(to)) {
            throw new IllegalArgumentException("Cannot send message to yourself");
        }

        ChatMessage chatMessage = new ChatMessage(
            from,
            to,
            message,
            LocalDateTime.now(),
            MessageStatus.SENT
        );

        // Store in message history
        String chatKey = getChatKey(from, to);
        messageHistory.computeIfAbsent(chatKey, k -> new ArrayList<>()).add(chatMessage);

        // Send to recipient
        Sinks.Many<ChatMessage> recipientSink = userSinks.get(to);
        if (recipientSink != null) {
            recipientSink.tryEmitNext(chatMessage);
            chatMessage.setStatus(MessageStatus.DELIVERED);
        }

        // Send back to sender with updated status
        Sinks.Many<ChatMessage> senderSink = userSinks.get(from);
        if (senderSink != null) {
            senderSink.tryEmitNext(chatMessage);
        }
    }

    public Registration registerUser(String username, java.util.function.Consumer<ChatMessage> messageConsumer) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        Sinks.Many<ChatMessage> sink = userSinks.computeIfAbsent(username,
            key -> Sinks.many().multicast().directBestEffort());

        var subscription = sink.asFlux()
            // Ensure users only receive messages they're involved in
            .filter(message -> {
                boolean isParticipant = message.getTo().equals(username) || message.getFrom().equals(username);
                if (isParticipant && message.getTo().equals(username)) {
                    message.setStatus(MessageStatus.READ);
                }
                return isParticipant;
            })
            .subscribe(messageConsumer);

        return () -> {
            subscription.dispose();
            userSinks.remove(username);
        };
    }

    public List<ChatMessage> getChatHistory(String currentUser, String otherUser) {
        // Ensure the requesting user is part of the conversation
        if (!currentUser.equals(otherUser)) {
            String chatKey = getChatKey(currentUser, otherUser);
            List<ChatMessage> messages = messageHistory.getOrDefault(chatKey, new ArrayList<>());
            
            // Filter messages to ensure user can only see messages they're involved in
            return messages.stream()
                .filter(msg -> msg.getFrom().equals(currentUser) || msg.getTo().equals(currentUser))
                .toList();
        }
        return new ArrayList<>(); // Return empty list for self-chat attempts
    }

    public static class ChatMessage {
        private final String from;
        private final String to;
        private final String content;
        private final LocalDateTime timestamp;
        private MessageStatus status;

        public ChatMessage(String from, String to, String content, LocalDateTime timestamp, MessageStatus status) {
            this.from = from;
            this.to = to;
            this.content = content;
            this.timestamp = timestamp;
            this.status = status;
        }

        public String getFrom() { return from; }
        public String getTo() { return to; }
        public String getContent() { return content; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public MessageStatus getStatus() { return status; }
        public void setStatus(MessageStatus status) { this.status = status; }
    }

    public enum MessageStatus {
        SENT,
        DELIVERED,
        READ
    }
}