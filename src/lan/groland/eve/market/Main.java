package lan.groland.eve.market;

import io.swagger.client.ApiException;
import io.swagger.client.api.UniverseApi;
import io.swagger.client.model.GetUniverseTypesTypeIdOk;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;


public class Main {

	private static Logger log = Logger.getLogger("Main");

	final public static int DODIXIE = 30002659;
	final public static int AMARR = 30002187;
	final public static int HEK = 30002053;

	public final static int SINQ_SAISON = 10000032;
	final public static int DOMAIN = 10000043;
	final public static int METROPOLIS = 10000042;
	final public static int HEIMATAR = 10000030;
	final public static int CATCH = 10000014;
	final public static int CURSE = 10000012;
	final public static int THE_CITADEL = 10000033;
	final public static int ESOTERIA = 10000039;

	final public static long[] D_P = {1024004680659l};
	final public static long[] DODIXIE_STATION = {60011866};
	final public static long[] AMARR_STATION = {60008494};
	final public static long[] HEK_STATION = {60005686};
	final public static long[] RENS_STATION = {60004588};
	final public static long[] BRAVELAND_STATION = {61000182,1023729674815l};
	final public static long[] TAMA = {60005203};

	private static Set<Integer> alreadyBought;

	static UniverseApi univers = new UniverseApi();

	final static int cargo = 320000;
	final static int region = ESOTERIA;
	final static  float fee = .962f; //.95f;
	final static int PLACE = 20; 
	final static double cash = 2500e6;
	final static BestTrades trades = new BestTrades(PLACE, cargo); 
	
	static MongoCollection<Document> itemDescritions;
	
	/**
	 * @param args
	 * @throws SQLException 
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws ApiException 
	 */
	public static void main(String[] args) throws SQLException, IOException, InterruptedException, ApiException {
		final long[] station = D_P;
		
		MongoClient mongoClient = new MongoClient(new MongoClientURI("mongodb://jupiter:27017"));
		MongoDatabase eve = mongoClient.getDatabase("Eve");
		itemDescritions = eve.getCollection("Items");

		OrdersStatsBuilder ordersStats = OrdersStatsBuilder.newInstance(station, region, false);

		OrdersStatsBuilder jita = OrdersStatsBuilder.newInstance(new long[]{60003760}, 10000002, true);

		alreadyBought = new HashSet<Integer>();
		try (BufferedReader reader = new BufferedReader(new FileReader("orders"))){
			reader.readLine();
			String l;
			while ((l = reader.readLine()) != null){
				String[] words = l.split(",");
				if (Integer.parseInt(words[4]) == region){
					alreadyBought.add(Integer.parseInt(words[1]));
				}
			}
		}

		

		ExecutorService executor = new ThreadPoolExecutor(10, 10, 0, TimeUnit.DAYS, new LinkedBlockingDeque<Runnable>());
		
		
		Iterator<Integer> items =  jita.cheaperThan(cash/10);
		while(items.hasNext()) {   // Move the cursor to the next row
			final int id = items.next();
			OrderStats jitaP = jita.get(id);
			OrderStats orders = ordersStats.get(id);
			Runnable run = new Command(id, jitaP, orders);
			executor.execute(run);
		}
		executor.shutdown();
		executor.awaitTermination(5, TimeUnit.HOURS);
		System.out.println(trades.multiBuyString());
		System.out.println(trades.toString());

	}

	static class Command implements Runnable {
		public Command(int id, OrderStats os , OrderStats orders){
			this.id = id;
			this.orderStats = os;
			this.orders = orders;
		}

		private int id;
		private OrderStats orderStats;
		OrderStats orders;


		public void run(){        	

			if (orderStats == null){
				return;
			}
			if (orders == null) return;
			try {
				BasicDBObject query = new BasicDBObject("id", id);
				Document res = itemDescritions.find(query).first();
				if (res == null){
					GetUniverseTypesTypeIdOk info = univers.getUniverseTypesTypeId(id, null, null, null, null, null);

					
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


				MarketStatistics.Sales sales = MarketStatistics.getInstance().medianPrice(id, region, buyPrice);

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
