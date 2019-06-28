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
                                                     @Named("rabbit.port") int port,
                                                     @Named("rabbit.user") String username,
                                                     @Named("rabbit.password") String password) {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(host);
    factory.setPort(port);
    factory.setUsername(username);
    factory.setPassword(password);
    return factory;
  }
  
  @Provides
  public Connection providesRabbitConnection(ConnectionFactory factory) throws TimeoutException, IOException {
    return factory.newConnection();
  }
}
