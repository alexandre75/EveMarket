package lan.groland.eve.market;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class MarketStatistics {

	public static class Sales {
		public double quantity;
		public double price;
	}

	static private volatile MarketStatistics instance;

	protected MarketStatistics(){
//		try {
//			conn = DriverManager.getConnection(
//					"jdbc:mysql://192.168.45.112:3306/eve", "alex", "chHfn8Zc");
//		} catch (SQLException e) {
//			throw new RuntimeException("",e);
//		}
	}

	static public MarketStatistics getInstance(){
		return new APIStats();
//		if (instance == null){
//			synchronized(MarketStatistics.class){
//				if (instance == null){
//					instance = new APIStats();
//					//instance = new MarketStatistics();
//				}
//			}
//		}
//		return instance;
	}
	private Connection conn;

	private PreparedStatement stmt2;
	public Sales medianPrice(int itemId, int regionId, double jitaPrice) throws SQLException{
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

class APIStats extends MarketStatistics {
	private DocumentBuilder builder;

	protected APIStats(){
		try {
			builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Sales medianPrice(int itemId, int regionId, double jitaPrice) throws SQLException{
		/*
		 * Computes and returns :
		 * - Daily sold volume : a transaction is considered a "sale" if price > JitaPrice
		 * - Trading price : actually the median of 30days high price (TODO could be pondered with quantities?)
		 */
		int remainingRetry = 3;
		while (true){
			try {
				URL url = new URL("https://api.eve-marketdata.com/api/item_history2.xml?char_name=Khamsila&region_ids="+regionId+"&type_ids="+itemId);
				HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
				con.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:221.0) Gecko/20100101 Firefox/31.0");
				Document dom = builder.parse(con.getInputStream());
				NodeList nodes = dom.getFirstChild().getChildNodes();
				Node result = null;
				for (int i = 0; i < nodes.getLength(); i++){
					Node tmp = nodes.item(i);
					if (tmp.getNodeName().equals("result")){
						result = tmp.getFirstChild();
						break;
					}
				}

				List<Double> prices = new ArrayList<Double>();
				List<Long> quantities = new ArrayList<>();

				for (Node day = result.getFirstChild(); day != null ; day = day.getNextSibling()){
					NamedNodeMap atts = day.getAttributes();
					double priceHigh=0, priceLow=0,priceAverage=0;
					long qty=0;
					for (int i = 0; i < atts.getLength(); i++){
						Node att = atts.item(i);
						if (atts.item(i).getNodeName().equals("highPrice")){
							priceHigh = Double.parseDouble(att.getNodeValue());
						} else if (atts.item(i).getNodeName().equals("lowPrice")){
							priceLow = Double.parseDouble(att.getNodeValue());
						} else if (atts.item(i).getNodeName().equals("avgPrice")){
							priceAverage = Double.parseDouble(att.getNodeValue());
						} else if (atts.item(i).getNodeName().equals("volume")){
							qty = Long.parseLong(att.getNodeValue());
						}
					}
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

				Collections.sort(prices);
				Sales res = new Sales();
				res.quantity = quantities.get(quantities.size()/2)*2;
				if (prices.size() > 0){
					res.price = prices.get(prices.size()/2);
				}
				return res;
			} catch(SAXException | IOException e){
				if (--remainingRetry == 0){
					throw new RuntimeException(e);
				}
			} catch(NullPointerException e){
				System.out.println("failed " + itemId);
				throw e;
			}
		}
	}
}
