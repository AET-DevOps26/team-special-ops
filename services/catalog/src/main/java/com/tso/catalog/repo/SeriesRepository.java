package com.tso.catalog.repo;

import com.tso.catalog.domain.Series;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

public interface SeriesRepository extends Repository<Series, UUID> {

  List<Series> findAllByOrderByTitleAsc();

  Optional<Series> findById(UUID id);

  boolean existsById(UUID id);
}
