package lan.groland.eve.bootstrap;

import java.io.IOException;
import java.util.Properties;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

class ConfigModule extends AbstractModule {
  @Override
  protected void configure() {
    try {
      Properties properties = new Properties();
      properties.load(ClassLoader.getSystemResourceAsStream("application.properties"));
      Names.bindProperties(binder(), properties);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}