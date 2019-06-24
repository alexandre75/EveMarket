package lan.groland.eve.bootstrap;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class TestModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(String.class).annotatedWith(Names.named("mongo.schema")).toInstance("unittest");
  }
}
