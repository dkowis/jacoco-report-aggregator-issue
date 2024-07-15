package example.autoconfigure;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureAfter(ExampleCorrelationIdConfigAutoConfiguration.class)
public class ExampleAutoConfigureReactiveCorrelationID {

  @Bean
  public ExampleCorrelationIdWebClientCustomizer exampleCorrelationIdWebClientCustomizer(
      ExampleCorrelationIdConfiguration config,
      ObservationRegistry observationRegistry) {
    return new ExampleCorrelationIdWebClientCustomizer(config, observationRegistry);
  }
}
