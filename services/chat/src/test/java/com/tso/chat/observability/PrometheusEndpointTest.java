package com.tso.chat.observability;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tso.chat.support.ChatPostgresIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = ChatPostgresIT.Initializer.class)
class PrometheusEndpointTest extends ChatPostgresIT {

  @Autowired private MockMvc mvc;

  @Test
  void prometheusEndpointExposesMetrics() throws Exception {
    mvc.perform(get("/chat/health")).andExpect(status().isOk());

    mvc.perform(get("/actuator/prometheus"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("http_server_requests")))
        .andExpect(content().string(containsString("application=\"chat\"")));
  }
}
