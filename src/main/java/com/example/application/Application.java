package com.example.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.Theme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@Push
@SpringBootApplication
@Theme(value = "chatappgroupproject")
public class Application implements AppShellConfigurator {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }
}
