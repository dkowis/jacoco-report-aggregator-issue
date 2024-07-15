package example.autoconfigure;

import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

@Slf4j
public class ExampleRestTemplateCorrelationIdCustomizer implements RestTemplateCustomizer {

  private final ExampleCorrelationIdConfiguration configuration;

  public ExampleRestTemplateCorrelationIdCustomizer(ExampleCorrelationIdConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public void customize(RestTemplate restTemplate) {
    //Here we need to do the cool stuff that tracing also does.
    var interceptorList = restTemplate.getInterceptors();
    //Do I need to make sure it's not already in the interceptors list?
    if(CollectionUtils.isEmpty(interceptorList)) {
      interceptorList = new ArrayList<>();
    }
    interceptorList.add(new ExampleCorrelationIdInterceptor(configuration));
    restTemplate.setInterceptors(interceptorList);
  }

}
