package example.autoconfigure;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("example.correlation-id")
public record ExampleCorrelationIdConfiguration(
    @DefaultValue({"x-example-correlation-id"})
    List<String> headerNames
) {

}
