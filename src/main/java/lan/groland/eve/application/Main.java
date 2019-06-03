package lan.groland.eve.application;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import com.google.inject.Guice;
import com.google.inject.Inject;

import io.swagger.client.ApiException;
import lan.groland.eve.adapter.port.EsiEveDataModule;
import lan.groland.eve.domain.market.Cargo;
import lan.groland.eve.domain.market.ShipmentService;
import lan.groland.eve.domain.market.ShipmentSpecification;
import lan.groland.eve.domain.market.Station;


public class Main {
  private static final Main INSTANCE = new Main();

  @SuppressWarnings("unused")
  private static Logger logger = Logger.getLogger(Main.class);
  
  static final double cash = 6e9;
  
  @Inject ShipmentService shipmentService;

  public static void main(String[] args) throws IOException, InterruptedException {
    Guice.createInjector(new EsiEveDataModule()).injectMembers(INSTANCE);

    Station station = Station.D_P;
    Set<Integer> alreadyBought = alreadyBought(station);
    Cargo trades = INSTANCE.main(station, alreadyBought);
    System.out.println(trades.multiBuyString());
    System.out.println(trades.toString());
  }

  private static Set<Integer> alreadyBought(Station station) throws IOException {
    Set<Integer> alreadyBought = new HashSet<Integer>();
    try (BufferedReader reader = new BufferedReader(new FileReader("orders"))){
      reader.readLine();
      String l;
      while ((l = reader.readLine()) != null){
        String[] words = l.split(",");
        if (Integer.parseInt(words[4]) == station.getRegionId()){
          alreadyBought.add(Integer.parseInt(words[1]));
        }
      }
    }
    return alreadyBought;
  }

  /**
   * @param args
   * @throws SQLException 
   * @throws IOException 
   * @throws InterruptedException 
   * @throws ApiException 
   */
  public Cargo main(Station station, Set<Integer> alreadyBought) throws InterruptedException {
    ShipmentSpecification spec = new ShipmentSpecification.Builder(station, cash)
                                                          .alreadyBought(alreadyBought)
                                                          .build();
    return shipmentService.optimizeCargo(spec);
  }
}
