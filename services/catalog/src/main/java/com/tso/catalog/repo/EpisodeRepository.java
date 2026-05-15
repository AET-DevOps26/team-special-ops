package com.tso.catalog.repo;

import com.tso.catalog.domain.Episode;
import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.Repository;

public interface EpisodeRepository extends Repository<Episode, UUID> {

  List<Episode> findByShow_IdOrderByEpisodeIndexAsc(UUID showId);
}
