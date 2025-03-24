package com.example.application.views;

import com.example.application.services.GeminiService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;

@PermitAll
@Route(value = "chatAI", layout = MainLayout.class)
@PageTitle("ChatYAI | Chat App")
public class ChatWithAIView extends VerticalLayout {

    @SuppressWarnings("unused")
    private final GeminiService geminiService;

    @Autowired
    public ChatWithAIView(GeminiService geminiService) {
        this.geminiService = geminiService;
        setSizeFull();
        setPadding(false);
        addClassNames("ai-chat-view"); // Overall view style

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setSpacing(false);
        layout.setPadding(false);
        layout.setJustifyContentMode(JustifyContentMode.CENTER);
        layout.setAlignItems(Alignment.CENTER);
        layout.addClassNames("layout-ai-chat"); // Layout style

        Div mainContent = new Div();
        mainContent.setWidthFull();
        mainContent.addClassNames("main-content", LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);

        Div welcomeArea = new Div();
        welcomeArea.addClassNames("welcome-area");

        H4 welcomeTitle = new H4("Welcome To ChatYAI");
        welcomeTitle.addClassName("welcome-title");

        Paragraph welcomeText = new Paragraph("Your AI-powered assistant is ready to help you. Ask any question to get started!");
        welcomeText.addClassName("welcome-text");

        welcomeArea.add(welcomeTitle, welcomeText);

        Div messagesArea = new Div();
        messagesArea.addClassNames("messages-area");

        HorizontalLayout inputLayout = new HorizontalLayout();
        inputLayout.setWidthFull();
        inputLayout.setSpacing(true);
        inputLayout.setAlignItems(Alignment.CENTER);
        inputLayout.addClassName("input-layout");

        TextField messageInput = new TextField();
        messageInput.setPlaceholder("Ask ChatYAI something...");
        messageInput.setClearButtonVisible(true);
        messageInput.setWidthFull();
        messageInput.addClassName("ai-chat-input");

        Button sendButton = new Button(new Icon(VaadinIcon.PAPERPLANE));
        sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        sendButton.addClickShortcut(Key.ENTER);
        sendButton.addClassName("ai-chat-send-button");

        sendButton.addClickListener(e -> {
            String message = messageInput.getValue().trim();
            if (!message.isEmpty()) {
                addMessage(messagesArea, "You", message, false);
                UI ui = UI.getCurrent();

                Div loading = new Div(new Span("Thinking..."));
                messagesArea.add(loading);

                geminiService.getGeminiResponse(message)
                        .subscribe(response -> {
                            System.out.println("Response received: " + response);
                            ui.access(() -> {
                                messagesArea.remove(loading);
                                addMessage(messagesArea, "ChatYAI", response, true);
                                ui.push(); // Force UI update
                            });
                        }, error -> {
                            System.out.println("Error: " + error.getMessage());
                            ui.access(() -> {
                                messagesArea.remove(loading);
                                addMessage(messagesArea, "System", "Error: " + error.getMessage(), true);
                                ui.push(); // Force UI update
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
        add(layout);
    }

    private void addMessage(Div messagesArea, String sender, String message, boolean isAI) {
    Div messageDiv = new Div();
    messageDiv.addClassNames("message-bubble", LumoUtility.Width.FULL);

    Div senderDiv = new Div();
    senderDiv.setText(sender);
    senderDiv.addClassName("sender-div");

    Div contentDiv = new Div();
    contentDiv.setText(message);
    contentDiv.addClassName("content-div");

    if (isAI) {
        messageDiv.addClassName("ai");
    } else {
        messageDiv.addClassName("user");
    }
        messageDiv.add(senderDiv, contentDiv);
        messagesArea.add(messageDiv);

        messagesArea.getElement().executeJs("this.scrollIntoView({ behavior: 'smooth', block: 'end' })");
    }
}
