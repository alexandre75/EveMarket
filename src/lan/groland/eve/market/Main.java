package lan.groland.eve.market;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class Main {

	private static Logger log = Logger.getLogger("Main");
	
	final public static int DODIXIE = 30002659;
	final public static int AMARR = 30002187;
	public final static int SINQ_SAISON = 10000032;
	final public static int DOMAIN = 10000043;
	
	final public static int METROPOLIS = 10000042;
	final public static int HEK = 30002053;
	
	final public static int DODIXIE_STATION = 60011866;
	final public static int AMARR_STATION = 60008494;
	
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
		int station = AMARR_STATION;
		int region = DOMAIN;
		int cash = 20000000;
		final int PLACE = 12;
		final int cargo = 9552;
		
		Set<Integer> alreadyBought = new HashSet<Integer>();
		try (BufferedReader reader = new BufferedReader(new FileReader("orders"))){
			reader.readLine();
			String l;
			while ((l = reader.readLine()) != null){
				String[] words = l.split(",");
				alreadyBought.add(Integer.parseInt(words[1]));
			}
		}
		 Connection conn = DriverManager.getConnection(
	               "jdbc:mysql://192.168.1.133:3306/eve", "alex", "chHfn8Zc");
	     //  is returned in a "ResultSet" object.
         String strSelect = "select eve_inv_types.type_id, eve_inv_types.name, eve_inv_types.jita_price_sell, sum((price_low - price_average)/(price_low-price_high)*items_history.quantity)/27.0 as quantJour, avg(items_history.price_high) as median, volume " +
         		"from eve_inv_types, items_history " +
         		"where eve_inv_types.jita_price_sell <> 0 and eve_inv_types.jita_price_sell < "+(2*cash/(float)PLACE)+" and eve_inv_types.type_id = items_history.type_id and items_history.region_id = "+region+
         				" group by eve_inv_types.type_id ";
         //		+" having count(items_history.quantity) / 27.0 > 0.6; ";
         log.fine("The SQL query is: " + strSelect); // Echo For debugging
         System.out.println(strSelect);
 
         Statement stmt = conn.createStatement();
         ResultSet rset = stmt.executeQuery(strSelect);
         
         BestTrades trades = new BestTrades(PLACE, cargo);
         
         System.out.println("Preselect done");
 
         while(rset.next()) {   // Move the cursor to the next row
            String name = rset.getString("name");
            double price = rset.getDouble("jita_price_sell");
            int    id   = rset.getInt("type_id");
            double quantitéJounalière = rset.getDouble("quantJour");
            double volume = rset.getDouble("volume");
            log.fine(name + ", " + price + ", " + id);
                   
            if (volume > cargo) continue; // ne rentre pas dans le cargo
            if (quantitéJounalière < 1) continue; // il faut au moins en vendre 1 par jour
            if (alreadyBought.contains(id)) continue;
            
            Sales sales = medianPrice(conn, id, region, price);
            
            double sellPrice = sales.price;
            quantitéJounalière = sales.quantity;
            
            double margeUnitaire = sellPrice * .975f /*taxe*/ -price;
            
            // On n'accepte pas les rentabilités historiques < 20%
            if (margeUnitaire <= .20 * price) continue; 
            
            double marge = margeUnitaire * quantitéJounalière;
            
            OrderStats orders = OrdersStatsBuilder.build(id, station, region);
            double margeParTrader = marge / (orders.nbSellOrders() +1);
            double prixDeVente = (orders.getBid() < Float.MAX_VALUE)? orders.getBid() : sellPrice;
            if (prixDeVente/price < 1.20f) continue; // pas de rentabilité < 20% sinon on pourrait être en perte
            Trade trade = new Trade(new Item(id, name, volume), margeParTrader, quantitéJounalière, prixDeVente, price);
            if (trade.ajust(2*cash/(float)PLACE)){
            	// System.out.println(name + "\t : " + cash.format(margeParTrader) + "\t" + volume);
            	trades.add(trade);
            }
         }
         System.out.println(trades.toString());
	}
	
	static PreparedStatement stmt2;
	private static Sales medianPrice(Connection conn, int itemId, int regionId, double jitaPrice) throws SQLException{
		if (stmt2 == null){
			stmt2 = conn.prepareStatement("select price_high, price_low, price_average, quantity from items_history where type_id = ? and region_id = ?;");
		}
		stmt2.setInt(1, itemId);
        stmt2.setInt(2, regionId);
        ResultSet rset2 = stmt2.executeQuery();
        List<Double> prices = new ArrayList<Double>();
        int quantity = 0;
        while (rset2.next()){
        	double priceHigh = rset2.getDouble("price_high");
            double priceLow = rset2.getDouble("price_low");
            double priceAverage = rset2.getDouble("price_average");
            double qty = rset2.getDouble("quantity");
        	if (priceLow >= jitaPrice){
        		prices.add(priceAverage);
        		quantity += qty;
        	} else if (priceHigh >= jitaPrice){
        		prices.add(priceHigh);
        		quantity += (priceLow - priceAverage)/(priceLow - priceHigh)*qty;
        	}
        }
        rset2.close();
        Collections.sort(prices);
        Sales res = new Sales();
        res.quantity = quantity / 27.0;
        if (prices.size() > 0){
        	res.price = prices.get(prices.size()/2);
        }
        return res;
	}

}

class Sales {
	double quantity;
	double price;
}
