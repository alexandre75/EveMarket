package lan.groland.eve.application;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.google.inject.Guice;
import com.google.inject.Inject;

import io.swagger.client.ApiException;
import lan.groland.eve.adapter.port.EsiEveDataModule;
import lan.groland.eve.domain.market.BestTrades;
import lan.groland.eve.domain.market.EveData;
import lan.groland.eve.domain.market.Item;
import lan.groland.eve.domain.market.ItemRepository;
import lan.groland.eve.domain.market.OrderBookEmptyException;
import lan.groland.eve.domain.market.OrderStats;
import lan.groland.eve.domain.market.Sales;
import lan.groland.eve.domain.market.Station;
import lan.groland.eve.domain.market.Trade;


public class Main {
  private static final Main INSTANCE = new Main();

  private static Logger log = Logger.getLogger(Main.class);

  final static int cargo = 320000;
  final static  float fee = .962f; //.95f;
  final static int PLACE = 30; 
  final static double cash = 6e9;

  @Inject
  private EveData eveData;

  @Inject
  private ItemRepository itemRepository;

  public static void main(String[] args) throws IOException, InterruptedException {
    Guice.createInjector(new EsiEveDataModule()).injectMembers(INSTANCE);

    Station station = Station.D_P;
    Set<Integer> alreadyBought = alreadyBought(station);
    BestTrades trades = INSTANCE.main(station, alreadyBought);
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
  public BestTrades main(Station station, Set<Integer> alreadyBought) throws InterruptedException {

    BestTrades trades = new BestTrades(PLACE, cargo); 
    List<Integer> items =  eveData.cheaperThan(cash/10, Station.JITA);
    for (int id : items) {   // Move the cursor to the next row
      try {
        OrderStats orders = eveData.regionOrderStats(id, station.getRegion());
        OrderStats jitaP = eveData.stationOrderStats(id, Station.JITA);

        try {
          final double buyPrice = jitaP.getBid();
          Item item = itemRepository.find(id);            
          if (item.getVolume() > cargo) continue; // ne rentre pas dans le cargo
          if (alreadyBought.contains(id)) continue;


          Sales sales = eveData.medianPrice(id, station.getRegion(), buyPrice);

          double sellPrice = sales.price;
          double quantitéJounalière = sales.quantity;

          if (quantitéJounalière < 1) continue ; // il faut au moins en vendre 1 par jour


          // On n'accepte pas les rentabilités historiques < 20%
          if (sellPrice * .975f /*taxe*/ -buyPrice <= .20 * buyPrice) continue; 

          // Si pas de concurence, on tente la culbute
          double prixDeVente = ((orders.getBid() < Float.MAX_VALUE)? orders.getBid() : buyPrice*1.5);
          prixDeVente = Math.min(1.5*sellPrice, prixDeVente); // on ne vendra probablement pas à n'importe quel prix
          double margeUnitaire =  prixDeVente * fee /*taxe*/ -buyPrice;

          if (prixDeVente/buyPrice < 1.40f) continue; // pas de rentabilité < 20% sinon on pourrait être en perte

          double quantiteAAcheter = Math.ceil(quantitéJounalière/(double)(orders.nbSellOrders() +1));
          //if (orders.nbSellOrders() <= 1) quantiteAAcheter *= 2;

          double margeProbable = Math.min(quantitéJounalière/(double)(orders.nbSellOrders() +1), quantiteAAcheter)*margeUnitaire;

          Trade trade = new Trade(item, margeProbable, quantiteAAcheter, prixDeVente, buyPrice);
          if (margeProbable < 1e6) continue; // on ne se baisse plus pour 3 fois rien
          if (trade.ajust(cash/PLACE)){
            // System.out.println(name + "\t : " + cash.format(margeParTrader) + "\t" + volume);
            trades.add(trade);
          }
        } catch(Exception e){
          log.error(e.getLocalizedMessage());
          e.printStackTrace(System.out);
          System.exit(1);
        }
      } catch(OrderBookEmptyException e) {
        log.info(e.getMessage());
      }
    }
    return trades;
  }
}
