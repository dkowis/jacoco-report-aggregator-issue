package example.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.PropertySource;

@AutoConfiguration
@PropertySource("classpath:/example/autoconfigure/example_baggage.properties")
@EnableConfigurationProperties(ExampleCorrelationIdConfiguration.class)
public class ExampleCorrelationIdConfigAutoConfiguration {

}
