package com.example.application.views;

import com.example.application.services.FileService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.component.html.Anchor;

import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@PermitAll
@Route(value = "upload", layout = MainLayout.class)
@PageTitle("File Upload | Chat App")
public class FileUploadView extends VerticalLayout {

    private final FileService fileService;
    private Grid<String> fileGrid;

    @Autowired
    public FileUploadView(FileService fileService) {
        this.fileService = fileService;

        // Set page layout
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("file-upload-view");

        H2 title = new H2("File Upload");
        title.addClassName("title-header");

        // File upload component
        Div uploadContainer = createUploadComponent();
        uploadContainer.addClassName("upload-container");

        // File listing component
        fileGrid = new Grid<>();
        fileGrid.addColumn(fileName -> fileName).setHeader("File Name");
        fileGrid.addClassName("file-grid");

        fileGrid.addComponentColumn(fileName -> {
            Anchor downloadLink = createDownloadLink(fileName);
            downloadLink.addClassName("download-button");

            Button deleteButton = new Button("Delete", new Icon(VaadinIcon.TRASH));
            deleteButton.addClassName("delete-button");

            deleteButton.addClickListener(e -> {
                boolean deleted = fileService.deleteFile(fileName);
                if (deleted) {
                    Notification.show("File deleted: " + fileName,
                            3000, Notification.Position.BOTTOM_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    refreshFileList();
                } else {
                    Notification.show("Failed to delete file: " + fileName,
                            3000, Notification.Position.BOTTOM_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });

            HorizontalLayout actions = new HorizontalLayout(downloadLink, deleteButton);
            actions.addClassName("file-actions");
            return actions;
        }).setHeader("Actions");

        refreshFileList();
        add(title, uploadContainer, new H2("Uploaded Files"), fileGrid);
    }

    private Div createUploadComponent() {
        Div uploadContainer = new Div();
        uploadContainer.setClassName("upload-container");

        MemoryBuffer memoryBuffer = new MemoryBuffer();
        Upload upload = new Upload(memoryBuffer);
        upload.setMaxFiles(1);
        upload.setDropLabel(new Paragraph("Drop file here or click to upload"));
        upload.setAcceptedFileTypes("image/*", ".pdf", ".txt", ".doc", ".docx");
        upload.setMaxFileSize(10 * 1024 * 1024);

        upload.addSucceededListener(event -> {
            String fileName = event.getFileName();
            try {
                Path savedPath = fileService.saveFile(fileName, memoryBuffer.getInputStream());
                Notification.show("File uploaded successfully: " + fileName,
                        3000, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                refreshFileList();
            } catch (IOException e) {
                Notification.show("Failed to save file: " + e.getMessage(),
                        5000, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        upload.addFailedListener(event -> {
            Notification.show("Upload failed: " + event.getReason(),
                    5000, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        });

        uploadContainer.add(upload);
        return uploadContainer;
    }

    private void refreshFileList() {
        List<String> files = fileService.getAllFiles();
        fileGrid.setItems(files);
    }

    private Anchor createDownloadLink(String fileName) {
        Path filePath = fileService.getFilePath(fileName);
        StreamResource resource = new StreamResource(fileName, () -> {
            try {
                return new ByteArrayInputStream(Files.readAllBytes(filePath));
            } catch (IOException e) {
                e.printStackTrace();
                return new ByteArrayInputStream(new byte[0]);
            }
        });
        Anchor downloadLink = new Anchor(resource, "Download");
        downloadLink.getElement().setAttribute("download", true);
        
        // Apply inline CSS to make it look like the "Delete" button
        downloadLink.getStyle()
            .set("background-color", "#f0f0f0")
            .set("border", "1px solid #dcdcdc")
            .set("border-radius", "5px")
            .set("padding", "5px 10px")
            .set("color", "#007bff")
            .set("font-weight", "bold")
            .set("text-decoration", "none")
            .set("display", "inline-flex")
            .set("align-items", "center")
            .set("cursor", "pointer")
            .set("transition", "background-color 0.3s");
        
        return downloadLink;
    }
} 