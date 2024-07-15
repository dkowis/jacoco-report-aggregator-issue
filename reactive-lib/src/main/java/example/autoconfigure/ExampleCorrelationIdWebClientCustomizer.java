package example.autoconfigure;

import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
public class ExampleCorrelationIdWebClientCustomizer implements WebClientCustomizer {

  private final ExampleCorrelationIdConfiguration config;
  private final ObservationRegistry observationRegistry;

  public ExampleCorrelationIdWebClientCustomizer(ExampleCorrelationIdConfiguration config,
      ObservationRegistry observationRegistry) {
    this.config = config;
    this.observationRegistry = observationRegistry;
  }

  @Override
  public void customize(WebClient.Builder webClientBuilder) {

    //Create the filter and make sure it's in here.
    ExchangeFilterFunction filterFunction = (clientRequest, nextFilter) -> {

      //I could call filter on the "next" one, would that run at the end?!?
//      nextFilter.filter()

      if (log.isTraceEnabled()) {
        clientRequest.headers().forEach((header, values) -> {
          log.trace("HEADER: {} - {}", header, values);
        });
      }
      if (!config.headerNames().isEmpty()) {
        //If I have any of the desired header names set, just use that, ignoring the traceparent stuff
        var existingHeaderName = clientRequest.headers().keySet().stream().filter( name -> config.headerNames().contains(name)).findFirst();

        final String correlationId = existingHeaderName.map( name -> {
          return clientRequest.headers().getFirst(name);
        }).orElseGet(() -> {
          var traceparent = clientRequest.headers().getFirst("traceparent");
          return traceparent.split("-")[1];
        });

        //This is outgoing things, do I just get them from MDC?
        //The tracing thing includes a traceparent, so we can just populate it in correlation-id
        // does this also add it to MDC and baggage? Probably not
        var headersToAdd = new HttpHeaders();
        //Need to figure out if any of the correlation IDs are set, and then propagate them, not just blindly use tracparent's ID
        config.headerNames().forEach(name -> {
          if (!clientRequest.headers().containsKey(name)) {
            //add a header for it
            headersToAdd.add(name, correlationId);
          }

        });
        return nextFilter.exchange(ClientRequest.from(clientRequest)
            .headers(httpHeaders -> {
              httpHeaders.addAll(headersToAdd);
            })
            .build()
        );
      }
      return nextFilter.exchange(clientRequest);
    };

    //TODO: why don't I also set up the observation thing here?

    webClientBuilder
        .filter(filterFunction)
        .observationRegistry(observationRegistry);
  }
}
