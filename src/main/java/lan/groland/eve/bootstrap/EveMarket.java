package lan.groland.eve.bootstrap;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.util.concurrent.Service;
import com.google.inject.Guice;
import com.google.inject.Injector;

import lan.groland.eve.adapter.port.messaging.RabbitModule;
import lan.groland.eve.adapter.port.messaging.RabbitService;
import lan.groland.eve.adapter.port.ws.EsiEveDataModule;

public class EveMarket {

  private static Logger logger = Logger.getLogger("bootstrap");

  public static void main(String[] args) throws IOException {
    Injector injector = Guice.createInjector(new ConfigModule(), new RabbitModule(), new EsiEveDataModule());
    RabbitService service = injector.getInstance(RabbitService.class);
    service.startAsync();
    if (service.state() == Service.State.FAILED){
      logger.severe(service.failureCause().getMessage());
      System.exit(1);
    }
  }
}




