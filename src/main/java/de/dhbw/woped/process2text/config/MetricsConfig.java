package de.dhbw.woped.process2text.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

  @Bean(name = "httpRequestsTotal")
  public Counter httpRequestsTotal(MeterRegistry registry) {
    return Counter.builder("http_requests_total")
        .description("Total number of HTTP requests")
        .register(registry);
  }

  @Bean(name = "httpRequestDuration")
  public Timer httpRequestDuration(MeterRegistry registry) {
    return Timer.builder("http_request_duration_seconds")
        .description("HTTP request duration in seconds")
        .register(registry);
  }

  @Bean(name = "apiCallDuration")
  public Timer apiCallDuration(MeterRegistry registry) {
    return Timer.builder("api_call_duration_seconds")
        .description("API call processing duration")
        .tag("application", "p2t-service")
        .register(registry);
  }
}
