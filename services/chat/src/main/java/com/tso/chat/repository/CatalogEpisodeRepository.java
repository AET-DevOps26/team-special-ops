package com.tso.chat.repository;

import com.tso.chat.entity.CatalogEpisode;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogEpisodeRepository extends JpaRepository<CatalogEpisode, UUID> {

  List<CatalogEpisode> findBySeriesIdAndEpisodeIndexLessThanEqualOrderByEpisodeIndexAsc(
      UUID seriesId, int episodeIndex);
}
