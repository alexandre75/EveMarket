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
	final public static int HEK = 30002053;
	
	public final static int SINQ_SAISON = 10000032;
	final public static int DOMAIN = 10000043;
	final public static int METROPOLIS = 10000042;
	final public static int HEIMATAR = 10000030;
	final public static int CATCH = 10000014;
	
	final public static int DODIXIE_STATION = 60011866;
	final public static int AMARR_STATION = 60008494;
	final public static int HEK_STATION = 60005686;
	final public static int RENS_STATION = 60004588;
	final public static int BRAVELAND_STATION = 61000182;
	
	
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
		int station = BRAVELAND_STATION;
		int region = CATCH;
		double cash = 1700e6;
		final int PLACE = 25;
		final int cargo = 12000;
		
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
         String strSelect = "select eve_inv_types.type_id, eve_inv_types.name, eve_inv_types.jita_price_sell, volume " +
         		"from eve_inv_types " +
         		"where eve_inv_types.jita_price_sell <> 0 and eve_inv_types.jita_price_sell < "+(cash/(float)30)//+" and eve_inv_types.type_id = 22553 "
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
            
            Sales sales = medianPrice(conn, id, region, buyPrice);
            
            double sellPrice = sales.price;
            double quantitéJounalière = sales.quantity;
            
            if (quantitéJounalière < 1) continue; // il faut au moins en vendre 1 par jour
            
            
            // On n'accepte pas les rentabilités historiques < 20%
            if (sellPrice * .975f /*taxe*/ -buyPrice <= .20 * buyPrice) continue; 
            
            OrderStats orders = OrdersStatsBuilder.build(id, station, region);
            
            // Si pas de concurence, on tente la culbute
            double prixDeVente = ((orders.getBid() < Float.MAX_VALUE)? orders.getBid() : buyPrice*1.5);
            prixDeVente = Math.min(1.5*sellPrice, prixDeVente); // on ne vendra probablement pas à n'importe quel prix
            double margeUnitaire =  prixDeVente * .983f /*taxe*/ -buyPrice;
           
            if (prixDeVente/buyPrice < 1.20f) continue; // pas de rentabilité < 20% sinon on pourrait être en perte
      
            double quantiteAAcheter = Math.ceil(quantitéJounalière/(double)(orders.nbSellOrders() +1));
            //if (orders.nbSellOrders() <= 1) quantiteAAcheter *= 2;
           
			double margeProbable = Math.min(quantitéJounalière/(double)(orders.nbSellOrders() +1), quantiteAAcheter)*margeUnitaire;
			Trade trade = new Trade(new Item(id, name, volume), margeProbable, quantiteAAcheter, prixDeVente, buyPrice);
            if (margeProbable < 1e6) continue; // on ne se baisse plus pour 3 fois rien
			if (trade.ajust(cash/30f)){
            	// System.out.println(name + "\t : " + cash.format(margeParTrader) + "\t" + volume);
            	trades.add(trade);
            }
         }
         System.out.println(trades.toString());
	}
	
	static PreparedStatement stmt2;
	private static Sales medianPrice(Connection conn, int itemId, int regionId, double jitaPrice) throws SQLException{
		/*
		 * Computes and returns :
		 * - Daily sold volume : a transaction is considered a "sale" if price > JitaPrice
		 * - Trading price : actually the median of 30days high price (TODO could be pondered with quantities?)
		 */
		if (stmt2 == null){
			stmt2 = conn.prepareStatement("select price_high, price_low, price_average, quantity from items_history where type_id = ? and region_id = ?;");
		}
		stmt2.setInt(1, itemId);
        stmt2.setInt(2, regionId);
        ResultSet rset2 = stmt2.executeQuery();
        List<Double> prices = new ArrayList<Double>();
        List<Long> quantities = new ArrayList<>();
       
        while (rset2.next()){
        	double priceHigh = rset2.getDouble("price_high");
            double priceLow = rset2.getDouble("price_low");
            double priceAverage = rset2.getDouble("price_average");
            long qty = rset2.getLong("quantity");
        	if (priceLow >= jitaPrice){
        		prices.add(priceAverage);
        		quantities.add(qty);
        	} else if (priceHigh >= jitaPrice){
        		prices.add(priceHigh);
        		quantities.add(Math.round((priceLow - priceAverage)/(priceLow - priceHigh)*qty));
        	}
        }
        
        /* 0 trades for days where records are missing */
        for (int i = quantities.size(); i < 27 ; i++){
        	quantities.add(0l);
        }
        Collections.sort(quantities);
        
        rset2.close();
        Collections.sort(prices);
        Sales res = new Sales();
        res.quantity = quantities.get(quantities.size()/2)*2;
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
