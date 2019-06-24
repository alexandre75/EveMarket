package lan.groland.eve.bootstrap;

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

import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.name.Names;

import lan.groland.eve.adapter.port.ws.EsiEveDataModule;
import lan.groland.eve.application.TradeFormat;
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
  private static final Comparator<Trade> TRADE_COMPARATOR = Comparator.comparing(Trade::dailyBenefit);
  private static NumberFormat intFormat = NumberFormat.getIntegerInstance();
  
  static final double cash = 6e9;
  
  private static TradeFormat tradeFormat = new TradeFormat();
  
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
    float dailyBenef = 0, invest = 0;
    double benef = 0;
    StringBuilder b = new StringBuilder("=============================================\n");
    for (Trade t : sortedTrade){
      b.append(t + "\n");
      dailyBenef += t.dailyBenefit();
      invest += t.capital();
      benef += t.capital() * t.expectedMargin();
    }
    b.append("Benefice potentiel :" + intFormat.format(benef) + " / " + intFormat.format(invest) + ": " +NumberFormat.getPercentInstance().format(benef/invest)) ;
    b.append("\nDaily benefit      :" + intFormat.format(dailyBenef));
    return b.toString();
  }

  public static String multiBuyString(Collection<Trade> trades){
    List<Trade> sortedTrade = new ArrayList<>(trades);
    Collections.sort(sortedTrade, TRADE_COMPARATOR);
    StringBuffer sb = new StringBuffer("=============================================\n");
    for (Trade t : sortedTrade){
      tradeFormat.format(t, sb, null);
      sb.append("\n");
    }
    return sb.toString();
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
    Gson json = new Gson();
    System.out.println(json.toJson(spec));
    return shipmentService.optimizeCargo(spec);
  }
}
