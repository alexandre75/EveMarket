package lan.groland.eve.adapter.port.messaging;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import com.google.inject.AbstractModule;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class RabbitModule extends AbstractModule {

  private Properties properties;
  
  public RabbitModule(Properties properties) {
    this.properties = properties;
  }
  
  @Override
  protected void configure() {
    ConnectionFactory factory = new ConnectionFactory().load(properties);
    bind(ConnectionFactory.class).toInstance(factory);
  }
  
  public Connection providesRabbitConnection(ConnectionFactory factory) throws TimeoutException, IOException {
    return factory.newConnection();
  }
}
