package lan.groland.eve.application;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.bson.Document;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import io.swagger.client.ApiException;
import io.swagger.client.api.UniverseApi;
import io.swagger.client.model.GetUniverseTypesTypeIdOk;
import lan.groland.eve.adapter.EsiEveDataModule;
import lan.groland.eve.domain.market.BestTrades;
import lan.groland.eve.domain.market.EveData;
import lan.groland.eve.domain.market.Item;
import lan.groland.eve.domain.market.OrderStats;
import lan.groland.eve.domain.market.Sales;
import lan.groland.eve.domain.market.Trade;
import lan.groland.eve.domain.market.Station;
import lan.groland.eve.domain.market.Station.Region;


public class Main {
  private static final Main INSTANCE = new Main();

  private static Logger log = Logger.getLogger("Main");
  
  private static Set<Integer> alreadyBought;

  static UniverseApi univers = new UniverseApi();

  final static int cargo = 320000;
  final static  float fee = .962f; //.95f;
  final static int PLACE = 30; 
  final static double cash = 6e9;
  final static BestTrades trades = new BestTrades(PLACE, cargo); 

  static MongoCollection<Document> itemDescritions;

  @Inject
  private EveData eveData;
  
  public static void main(String[] args) throws IOException, InterruptedException {
    Guice.createInjector(new EsiEveDataModule()).injectMembers(INSTANCE);
    
    Station station = Station.D_P;
    Set<Integer> alreadyBought = alreadyBought(station);
    INSTANCE.main(station, alreadyBought);
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
  public void main(Station station, Set<Integer> alreadyBought) throws InterruptedException {
    try(MongoClient mongoClient = new MongoClient(new MongoClientURI("mongodb://jupiter:27017"))){
      MongoDatabase eve = mongoClient.getDatabase("Eve");
      itemDescritions = eve.getCollection("Items");

      ExecutorService executor = new ThreadPoolExecutor(10, 10, 0, TimeUnit.DAYS, new LinkedBlockingDeque<Runnable>());


      List<Integer> items =  eveData.cheaperThan(cash/10, Station.JITA);
      for (int id : items) {   // Move the cursor to the next row
        OrderStats orders = eveData.regionOrderStats(id, station.getRegion());
        OrderStats jitaP = eveData.stationOrderStats(id, Station.JITA);
        Runnable run = new Command(id, jitaP, orders, station.getRegion());
        executor.execute(run);
      }
      executor.shutdown();
      executor.awaitTermination(5, TimeUnit.HOURS);
      System.out.println(trades.multiBuyString());
      System.out.println(trades.toString());
    }
  }

  class Command implements Runnable {
    private int id;
    private OrderStats orderStats;
    OrderStats orders;
    private Region region;

    public Command(int id, OrderStats os , OrderStats orders, Region region){
      this.id = id;
      this.orderStats = os;
      this.orders = orders;
      this.region = region;
    }

    public void run(){        	

      if (orderStats == null){
        return;
      }
      if (orders == null) return;
      try {
        BasicDBObject query = new BasicDBObject("id", id);
        Document res = itemDescritions.find(query).first();
        if (res == null){
          GetUniverseTypesTypeIdOk info;
          while(true){
            try {
              info = univers.getUniverseTypesTypeId(id, null, null, null, null, null);
              break;
            } catch(ApiException e){
            }
          }

          res = new Document("id", id)
              .append("volume", new Double(info.getVolume()))
              .append("name", info.getName())
              .append("timestamp", new Date());
          itemDescritions.insertOne(res);
        } 

        final double buyPrice = orderStats.getBid();
        final double volume = res.getDouble("volume");
        if (volume > cargo) return; // ne rentre pas dans le cargo
        if (alreadyBought.contains(id)) return;


        Sales sales = eveData.medianPrice(id, region, buyPrice);

        double sellPrice = sales.price;
        double quantitéJounalière = sales.quantity;

        if (quantitéJounalière < 1) return ; // il faut au moins en vendre 1 par jour


        // On n'accepte pas les rentabilités historiques < 20%
        if (sellPrice * .975f /*taxe*/ -buyPrice <= .20 * buyPrice) return; 

        // Si pas de concurence, on tente la culbute
        double prixDeVente = ((orders.getBid() < Float.MAX_VALUE)? orders.getBid() : buyPrice*1.5);
        prixDeVente = Math.min(1.5*sellPrice, prixDeVente); // on ne vendra probablement pas à n'importe quel prix
        double margeUnitaire =  prixDeVente * fee /*taxe*/ -buyPrice;

        if (prixDeVente/buyPrice < 1.40f) return; // pas de rentabilité < 20% sinon on pourrait être en perte

        double quantiteAAcheter = Math.ceil(quantitéJounalière/(double)(orders.nbSellOrders() +1));
        //if (orders.nbSellOrders() <= 1) quantiteAAcheter *= 2;

        double margeProbable = Math.min(quantitéJounalière/(double)(orders.nbSellOrders() +1), quantiteAAcheter)*margeUnitaire;

        final String name = res.getString("name");
        Trade trade = new Trade(new Item(id, name, volume), margeProbable, quantiteAAcheter, prixDeVente, buyPrice);
        if (margeProbable < 1e6) return; // on ne se baisse plus pour 3 fois rien
        if (trade.ajust(cash/PLACE)){
          // System.out.println(name + "\t : " + cash.format(margeParTrader) + "\t" + volume);
          trades.add(trade);
        }
      } catch(Exception e){
        log.severe(e.getLocalizedMessage());
        e.printStackTrace(System.out);
        System.exit(1);
      }
    }
  };

}
