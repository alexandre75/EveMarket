package lan.groland.eve.adapter.port.messaging;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class RabbitModule extends AbstractModule {
  

  @Provides
  public ConnectionFactory providesCOnnectionFactory(@Named("rabbit.host") String host, 
                                                     @Named("rabbit.port") int port) {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(host);
    factory.setPort(port);
    return factory;
  }
  
  @Provides
  public Connection providesRabbitConnection(ConnectionFactory factory) throws TimeoutException, IOException {
    return factory.newConnection();
  }
}
