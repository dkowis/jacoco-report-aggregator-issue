package example.autoconfigure;

import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.web.client.RestClient;

@Slf4j
public class ExampleRestClientCorrelationIdCustomizer implements RestClientCustomizer {

  private final ExampleCorrelationIdConfiguration configuration;
  private final ObservationRegistry observationRegistry;

  public ExampleRestClientCorrelationIdCustomizer(ExampleCorrelationIdConfiguration config,
      ObservationRegistry observationRegistry) {
    this.configuration = config;
    this.observationRegistry = observationRegistry;
  }

  @Override
  public void customize(RestClient.Builder restClientBuilder) {
    restClientBuilder
        .requestInterceptor(new ExampleCorrelationIdInterceptor(configuration))
        .observationRegistry(observationRegistry);
  }
}
