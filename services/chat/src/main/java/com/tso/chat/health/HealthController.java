package com.tso.chat.health;

import com.tso.chat.api.ChatApi;
import com.tso.chat.model.HealthStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
class HealthController implements ChatApi {

  @Override
  public ResponseEntity<HealthStatus> chatHealth() {
    HealthStatus body = new HealthStatus().status(HealthStatus.StatusEnum.OK).service("chat");
    return ResponseEntity.ok(body);
  }
}
