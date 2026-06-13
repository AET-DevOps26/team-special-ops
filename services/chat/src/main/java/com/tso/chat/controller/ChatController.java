package com.tso.chat.controller;

import com.tso.chat.api.ChatApi;
import com.tso.chat.model.ChatAnswerResponse;
import com.tso.chat.model.ChatQuestionRequest;
import com.tso.chat.model.HealthStatus;
import com.tso.chat.service.ChatService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController implements ChatApi {

  private final ChatService chatService;

  public ChatController(ChatService chatService) {
    this.chatService = chatService;
  }

  @Override
  public ResponseEntity<HealthStatus> chatHealth() {
    HealthStatus body = new HealthStatus().status(HealthStatus.StatusEnum.OK).service("chat");
    return ResponseEntity.ok(body);
  }

  @Override
  public ResponseEntity<ChatAnswerResponse> askQuestion(ChatQuestionRequest chatQuestionRequest) {
    UUID userId = currentUserId();
    ChatAnswerResponse response = chatService.askQuestion(userId, chatQuestionRequest);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  private UUID currentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return UUID.fromString(auth.getName());
  }
}
