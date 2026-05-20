package com.tso.catalog.health;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tso.catalog.catalog.ShowController;
import com.tso.catalog.repo.EpisodeRepository;
import com.tso.catalog.repo.ShowRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ShowController.class)
class HealthControllerTest {

  @Autowired private MockMvc mvc;

  @MockitoBean ShowRepository showRepository;
  @MockitoBean EpisodeRepository episodeRepository;

  @Test
  void healthReturnsOkAndServiceName() throws Exception {
    mvc.perform(get("/catalog/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ok"))
        .andExpect(jsonPath("$.service").value("catalog"));
  }
}
