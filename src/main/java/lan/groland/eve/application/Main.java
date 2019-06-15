package lan.groland.eve.application;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.name.Names;

import io.swagger.client.ApiException;
import lan.groland.eve.adapter.port.EsiEveDataModule;
import lan.groland.eve.domain.market.ShipmentService;
import lan.groland.eve.domain.market.ShipmentSpecification;
import lan.groland.eve.domain.market.Station;
import lan.groland.eve.domain.market.Trade;

class Config extends AbstractModule {
  @Override
  protected void configure() {
    bind(String.class).annotatedWith(Names.named("mongo.schema")).toInstance("EveMarket");
  }
}

public class Main {
  private static final Main INSTANCE = new Main();
  private static final Comparator<Trade> TRADE_COMPARATOR = Comparator.comparing(Trade::getBenefParJour);
  private static NumberFormat intFormat = NumberFormat.getIntegerInstance();
  
  @SuppressWarnings("unused")
  private static Logger logger = Logger.getLogger(Main.class);
  
  static final double cash = 6e9;
  
  @Inject ShipmentService shipmentService;

  public static void main(String[] args) throws IOException, InterruptedException {
    Guice.createInjector(new Config(), new EsiEveDataModule()).injectMembers(INSTANCE);

    Station station = Station.AMARR_STATION;
    Set<Integer> alreadyBought = alreadyBought(station);
    Collection<Trade> trades = INSTANCE.main(station, alreadyBought);
    System.out.println(multiBuyString(trades));
    System.out.println(toString(trades));
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
  
  public static String toString(Collection<Trade> trades){
    List<Trade> sortedTrade = new ArrayList<>(trades);
    Collections.sort(sortedTrade, TRADE_COMPARATOR);
    float benef = 0, invest = 0;
    StringBuilder b = new StringBuilder("=============================================\n");
    for (Trade t : sortedTrade){
      b.append(t + "\n");
      benef += t.getBenefParJour();
      invest += t.capital();
    }
    b.append("Benefice potentiel :" + intFormat.format(benef) + " / " + intFormat.format(invest) + ": " +NumberFormat.getPercentInstance().format(benef/invest)) ;
    return b.toString();
  }

  public static String multiBuyString(Collection<Trade> trades){
    List<Trade> sortedTrade = new ArrayList<>(trades);
    Collections.sort(sortedTrade, TRADE_COMPARATOR);
    StringBuilder b = new StringBuilder("=============================================\n");
    for (Trade t : sortedTrade){
      b.append(t.multiBuyString() + "\n");
    }
    return b.toString();
  }

  /**
   * @param args
   * @throws SQLException 
   * @throws IOException 
   * @throws InterruptedException 
   * @throws ApiException 
   */
  public Collection<Trade> main(Station station, Set<Integer> alreadyBought) throws InterruptedException {
    ShipmentSpecification spec = new ShipmentSpecification.Builder(station, cash)
                                                          .alreadyBought(alreadyBought)
                                                          .build();
    return shipmentService.optimizeCargo(spec);
  }
}
