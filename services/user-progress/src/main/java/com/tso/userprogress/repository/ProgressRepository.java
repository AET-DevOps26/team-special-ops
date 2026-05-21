package com.tso.userprogress.repository;

import com.tso.userprogress.entity.Progress;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProgressRepository extends JpaRepository<Progress, UUID> {

  List<Progress> findByUserId(UUID userId);

  Optional<Progress> findByUserIdAndSeriesId(UUID userId, UUID seriesId);
}
