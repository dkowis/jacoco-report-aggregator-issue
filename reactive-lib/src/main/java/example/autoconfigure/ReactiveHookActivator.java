package example.autoconfigure;

import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.context.ApplicationListener;
import reactor.core.publisher.Hooks;

public class ReactiveHookActivator implements ApplicationListener<ApplicationStartingEvent> {

  @Override
  public void onApplicationEvent(ApplicationStartingEvent event) {
    //Enable the automatic context propagation *always*
    Hooks.enableAutomaticContextPropagation();
  }
}
