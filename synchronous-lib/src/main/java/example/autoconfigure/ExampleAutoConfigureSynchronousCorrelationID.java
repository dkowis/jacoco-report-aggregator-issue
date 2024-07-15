package example.autoconfigure;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@AutoConfigureAfter(ExampleCorrelationIdConfigAutoConfiguration.class)
public class ExampleAutoConfigureSynchronousCorrelationID {

  @Bean
  public ExampleRestTemplateCorrelationIdCustomizer exampleRestTemplateCorrelationIdCustomizer(
      ExampleCorrelationIdConfiguration configuration) {
    return new ExampleRestTemplateCorrelationIdCustomizer(configuration);
  }

  @Bean
  public ExampleRestClientCorrelationIdCustomizer exampleRestClientCorrelationIdCustomizer(
      ExampleCorrelationIdConfiguration configuration,
      ObservationRegistry observationRegistry) {
    return new ExampleRestClientCorrelationIdCustomizer(configuration, observationRegistry);
  }
}
