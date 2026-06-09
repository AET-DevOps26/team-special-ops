package com.tso.chat.repository;

import com.tso.chat.entity.CatalogSeries;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogSeriesRepository extends JpaRepository<CatalogSeries, UUID> {}
