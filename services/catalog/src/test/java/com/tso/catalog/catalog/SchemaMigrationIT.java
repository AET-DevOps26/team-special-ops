package com.tso.catalog.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.tso.catalog.support.PostgresIT;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(initializers = PostgresIT.Initializer.class)
class SchemaMigrationIT extends PostgresIT {

  @Autowired DataSource dataSource;

  @Test
  void flywayCreatesShowAndEpisodeTables() {
    var jdbc = new JdbcTemplate(dataSource);
    var tables =
        jdbc.queryForList(
            "SELECT table_name FROM information_schema.tables "
                + "WHERE table_schema = 'public' "
                + "AND table_name IN ('show', 'episode') "
                + "ORDER BY table_name",
            String.class);
    assertThat(tables).containsExactly("episode", "show");
  }

  @Test
  void episodeHasIndexOnShowIdEpisodeIndex() {
    var jdbc = new JdbcTemplate(dataSource);
    var indexes =
        jdbc.queryForList(
            "SELECT indexname FROM pg_indexes "
                + "WHERE schemaname = 'public' AND tablename = 'episode'",
            String.class);
    assertThat(indexes).contains("idx_episode_show_index");
  }
}
