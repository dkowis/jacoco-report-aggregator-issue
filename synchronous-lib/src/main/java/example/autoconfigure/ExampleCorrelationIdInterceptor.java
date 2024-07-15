package example.autoconfigure;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

@Slf4j
class ExampleCorrelationIdInterceptor implements ClientHttpRequestInterceptor {

  private final ExampleCorrelationIdConfiguration config;

  public ExampleCorrelationIdInterceptor(ExampleCorrelationIdConfiguration configuration) {
    this.config = configuration;
  }

  @Override
  public ClientHttpResponse intercept(HttpRequest request, byte[] body,
      ClientHttpRequestExecution execution) throws IOException {
    //Somehow the request headers here don't match what is in the correlation ID of the MVC logs.

    if(log.isTraceEnabled()) {
      request.getHeaders().forEach((header, values) -> {
        log.trace("HEADER: {} - {}", header, values);
      });
    }

    log.trace("CONFIGURED HEADERS: {}", config.headerNames());

    if(!config.headerNames().isEmpty()) {
      var existingHeaderName = request.getHeaders().keySet().stream().filter( name -> config.headerNames().contains(name)).findFirst();

      final String correlationId = existingHeaderName.map( name -> {
        return request.getHeaders().getFirst(name);
      }).orElseGet( () -> {
        var traceparent = request.getHeaders().getFirst("traceparent");
        return traceparent.split("-")[1];
      });

      var headersToAdd = new HttpHeaders();
      config.headerNames().forEach(name -> {
        if(!request.getHeaders().containsKey(name)) {
          headersToAdd.add(name, correlationId);
        }
      });

      request.getHeaders().addAll(headersToAdd);
    }

    var response = execution.execute(request,body);

    //I don't think I need to modify the response, just the request

    return response;
  }
}
