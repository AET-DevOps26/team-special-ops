package com.tso.userprogress.controller;

import com.tso.userprogress.api.ProgressApi;
import com.tso.userprogress.model.ProgressEntry;
import com.tso.userprogress.model.UpdateProgressRequest;
import com.tso.userprogress.service.ProgressService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProgressController implements ProgressApi {

  private final ProgressService progressService;

  public ProgressController(ProgressService progressService) {
    this.progressService = progressService;
  }

  @Override
  public ResponseEntity<List<ProgressEntry>> getProgress() {
    UUID userId = currentUserId();
    List<ProgressEntry> body =
        progressService.listForUser(userId).stream().map(ProgressEntryMapper::toDto).toList();
    return ResponseEntity.ok(body);
  }

  @Override
  public ResponseEntity<ProgressEntry> updateProgress(UpdateProgressRequest updateProgressRequest) {
    UUID userId = currentUserId();
    var saved =
        progressService.upsert(
            userId, updateProgressRequest.getSeriesId(), updateProgressRequest.getEpisodeIndex());
    return ResponseEntity.ok(ProgressEntryMapper.toDto(saved));
  }

  private UUID currentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return UUID.fromString((String) authentication.getPrincipal());
  }
}
