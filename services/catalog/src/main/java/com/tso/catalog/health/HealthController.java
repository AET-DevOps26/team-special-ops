package com.tso.catalog.health;

import com.tso.catalog.api.CatalogApi;
import com.tso.catalog.model.HealthStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
class HealthController implements CatalogApi {

  @Override
  public ResponseEntity<HealthStatus> catalogHealth() {
    HealthStatus body = new HealthStatus().status(HealthStatus.StatusEnum.OK).service("catalog");
    return ResponseEntity.ok(body);
  }
}
