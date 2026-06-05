package com.tso.catalog.observability;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tso.catalog.support.PostgresIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = PostgresIT.Initializer.class)
class PrometheusEndpointIT extends PostgresIT {

  @Autowired MockMvc mvc;

  @Test
  void prometheusEndpointExposesMetrics() throws Exception {
    // Drive one request so the http_server_requests metrics are emitted.
    mvc.perform(get("/catalog/series")).andExpect(status().isOk());

    mvc.perform(get("/actuator/prometheus"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("http_server_requests")))
        .andExpect(content().string(containsString("application=\"catalog\"")));
  }
}
