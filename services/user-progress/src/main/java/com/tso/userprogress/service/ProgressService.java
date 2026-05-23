package com.tso.userprogress.service;

import com.tso.userprogress.entity.Progress;
import com.tso.userprogress.repository.ProgressRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProgressService {

  private final ProgressRepository repository;

  public ProgressService(ProgressRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public List<Progress> listForUser(UUID userId) {
    return repository.findByUserId(userId);
  }

  @Transactional
  public Progress upsert(UUID userId, UUID seriesId, int episodeIndex) {
    Progress entry =
        repository
            .findByUserIdAndSeriesId(userId, seriesId)
            .orElseGet(() -> new Progress(userId, seriesId, episodeIndex));
    entry.setEpisodeIndex(episodeIndex);
    return repository.save(entry);
  }
}
