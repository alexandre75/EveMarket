package lan.groland.eve.market;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

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
	
	
	final public static long[] DODIXIE_STATION = {60011866};
	final public static long[] AMARR_STATION = {60008494};
	final public static long[] HEK_STATION = {60005686};
	final public static long[] RENS_STATION = {60004588};
	final public static long[] BRAVELAND_STATION = {61000182,1023729674815l};
	final public static long[] TAMA = {60005203};
	
	final private static NumberFormat cash = NumberFormat.getIntegerInstance();
	{
		cash.setGroupingUsed(true);
	}
	
	/**
	 * @param args
	 * @throws SQLException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws SQLException, IOException {
		long[] station = AMARR_STATION;
		int region = DOMAIN;
		double cash = 2e9;
		final int PLACE = 30; 
		final int cargo = 845000;
		final float fee = .962f; //.95f;
		
		Set<Integer> alreadyBought = new HashSet<Integer>();
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
		 Connection conn = DriverManager.getConnection(
	               "jdbc:mysql://192.168.45.112:3306/eve", "alex", "chHfn8Zc");
	     //  is returned in a "ResultSet" object.
         String strSelect = "select eve_inv_types.type_id, eve_inv_types.name, eve_inv_types.jita_price_sell, volume " +
         		"from eve_inv_types " +
         		"where eve_inv_types.jita_price_sell <> 0 and eve_inv_types.jita_price_sell < "+(cash/(float)10)//+" and eve_inv_types.type_id = 22553 "
         				;
         //		+" having count(items_history.quantity) / 27.0 > 0.6; ";
         log.fine("The SQL query is: " + strSelect); // Echo For debugging
         System.out.println(strSelect);
 
         Statement stmt = conn.createStatement();
         ResultSet rset = stmt.executeQuery(strSelect);
         
         BestTrades trades = new BestTrades(PLACE, cargo); 
         
         System.out.println("Preselect done");
 
         while(rset.next()) {   // Move the cursor to the next row
            String name = rset.getString("name");
            double buyPrice = rset.getDouble("jita_price_sell");
            int    id   = rset.getInt("type_id");
            double volume = rset.getDouble("volume");
            log.fine(name + ", " + buyPrice + ", " + id);
                   
            if (volume > cargo) continue; // ne rentre pas dans le cargo
            if (alreadyBought.contains(id)) continue;
            
            MarketStatistics.Sales sales = MarketStatistics.getInstance().medianPrice(id, region, buyPrice);
            
            double sellPrice = sales.price;
            double quantitéJounalière = sales.quantity;
            
            if (quantitéJounalière < 1) continue; // il faut au moins en vendre 1 par jour
            
            
            // On n'accepte pas les rentabilités historiques < 20%
            if (sellPrice * .975f /*taxe*/ -buyPrice <= .20 * buyPrice) continue; 
            
            OrderStats orders = OrdersStatsBuilder.build(id, station, region);
            
            // Si pas de concurence, on tente la culbute
            double prixDeVente = ((orders.getBid() < Float.MAX_VALUE)? orders.getBid() : buyPrice*1.5);
            prixDeVente = Math.min(1.5*sellPrice, prixDeVente); // on ne vendra probablement pas à n'importe quel prix
            double margeUnitaire =  prixDeVente * fee /*taxe*/ -buyPrice;
           
            if (prixDeVente/buyPrice < 1.40f) continue; // pas de rentabilité < 20% sinon on pourrait être en perte
      
            double quantiteAAcheter = Math.ceil(quantitéJounalière/(double)(orders.nbSellOrders() +1));
            //if (orders.nbSellOrders() <= 1) quantiteAAcheter *= 2;
           
			double margeProbable = Math.min(quantitéJounalière/(double)(orders.nbSellOrders() +1), quantiteAAcheter)*margeUnitaire;
			
			
			Trade trade = new Trade(new Item(id, name, volume), margeProbable, quantiteAAcheter, prixDeVente, buyPrice);
            if (margeProbable < 1e6) continue; // on ne se baisse plus pour 3 fois rien
			if (trade.ajust(cash/PLACE)){
            	// System.out.println(name + "\t : " + cash.format(margeParTrader) + "\t" + volume);
            	trades.add(trade);
            }
         }
         System.out.println(trades.multiBuyString());
         System.out.println(trades.toString());
         
	}

}
