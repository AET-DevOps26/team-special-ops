package com.tso.chat.health;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tso.chat.controller.ChatController;
import com.tso.chat.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = ChatController.class,
    excludeAutoConfiguration = SecurityAutoConfiguration.class)
class HealthControllerTest {

  @Autowired private MockMvc mvc;

  @MockBean private ChatService chatService;

  @Test
  void healthReturnsOkAndServiceName() throws Exception {
    mvc.perform(get("/chat/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ok"))
        .andExpect(jsonPath("$.service").value("chat"));
  }
}
