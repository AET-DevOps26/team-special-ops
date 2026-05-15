package com.tso.catalog.repo;

import com.tso.catalog.domain.Show;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

public interface ShowRepository extends Repository<Show, UUID> {

  List<Show> findAllByOrderByTitleAsc();

  Optional<Show> findById(UUID id);

  boolean existsById(UUID id);
}
