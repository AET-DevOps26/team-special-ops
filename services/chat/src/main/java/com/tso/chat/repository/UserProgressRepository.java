package com.tso.chat.repository;

import com.tso.chat.entity.UserProgress;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProgressRepository extends JpaRepository<UserProgress, UUID> {

  Optional<UserProgress> findByUserIdAndSeriesId(UUID userId, UUID seriesId);
}
