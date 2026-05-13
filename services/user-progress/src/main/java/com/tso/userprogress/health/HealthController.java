package com.tso.userprogress.health;

import com.tso.userprogress.api.UserProgressApi;
import com.tso.userprogress.model.HealthStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
class HealthController implements UserProgressApi {

  @Override
  public ResponseEntity<HealthStatus> userProgressHealth() {
    HealthStatus body =
        new HealthStatus().status(HealthStatus.StatusEnum.OK).service("user-progress");
    return ResponseEntity.ok(body);
  }
}
