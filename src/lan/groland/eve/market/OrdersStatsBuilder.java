package lan.groland.eve.market;

import io.swagger.client.ApiException;
import io.swagger.client.api.MarketApi;
import io.swagger.client.model.GetMarketsRegionIdOrders200Ok;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public class OrdersStatsBuilder {

	private static SAXParserFactory spf = SAXParserFactory.newInstance();
	{
		spf.setNamespaceAware(true);
	}


//	public static OrderStats build(int id, long[] stations, int regionlimit) throws IOException {
//		return build(id, stations, regionlimit, false);
//	}

	public static OrderStats oldbuild(int id, int stationId, int regionlimit, boolean station) throws IOException {
		URL url = new URL("http://api.eve-central.com/api/quicklook?typeid="+id+"&regionlimit="+regionlimit);
		try {
			URLConnection conn = url.openConnection();
			SAXParser saxParser = spf.newSAXParser();
			XMLReader xmlReader = saxParser.getXMLReader();
			Handler handler = new Handler(stationId, station);
			xmlReader.setContentHandler(handler);
			InputSource source = new InputSource(conn.getInputStream());
			xmlReader.parse(source);
			OrderStats res = new OrderStats(handler.getNbActiveTrafers(), handler.getBid(), handler.getNbAsks(), handler.getAsk());
			return res;
		} catch(NumberFormatException e){
			System.out.println(url);
			throw e;
		} catch (MalformedURLException e) {
			throw new RuntimeException("", e);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("", e);
		} catch (SAXException e) {
			throw new RuntimeException("", e);
		}
	}

	enum State {
		START,READING,LOCATION,TYPE
	}
	
	public OrdersStatsBuilder(){
		
	}
	
	private Map<Integer, OrderStats> stats;
	
	public void init(int region, long[] stationId, boolean stationIn) throws ApiException{
		stats = new HashMap<>();
		MarketApi market = new MarketApi();
		int page = 1;
		List<GetMarketsRegionIdOrders200Ok> orders;
		do {
			while (true){
				try {
					orders = market.getMarketsRegionIdOrders("all", (Integer)region, null, null, page, null, null, null);
					page++;
					System.out.println("Page : " + page);
					for (GetMarketsRegionIdOrders200Ok order : orders){
						if (stationIn && -1 == Arrays.binarySearch(stationId, order.getLocationId())) continue;	

						OrderStats stat = stats.get(order.getTypeId());
						if (stat == null){
							stat = new OrderStats(0, Float.MAX_VALUE, 0, Float.MIN_VALUE);
							stats.put(order.getTypeId(), stat);
						}
						if (order.isIsBuyOrder()){
							stat.newBuy(order.getPrice());
						} else {
							stat.newSell(order.getPrice(), order.getIssued().toLocalDateTime());
						}
					}
					break;
				} catch(ApiException e){
					System.out.println(e.getLocalizedMessage());
				}
			}
		} while(!orders.isEmpty());
	}
	
	public OrderStats get(int id){
		return stats.get(id);
	}

	
	public static OrdersStatsBuilder newInstance(long[] stationId, int regionlimit, boolean stationIn) throws ApiException{
		OrdersStatsBuilder o = new OrdersStatsBuilder();
		o.init(regionlimit, stationId, stationIn);
		return o;
	}

	public static OrderStats build(int id, long[] stationId, int regionlimit, boolean stationIn) throws IOException {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DATE, -2);
		Date yesterday = calendar.getTime();
		
		float lowerSell = Float.MAX_VALUE;
		float higherBuy = 0;
		int nbActiveSells = 0, nbActiveBuys = 0; 

		URL url = new URL("https://crest-tq.eveonline.com/market/"+regionlimit+"/orders/buy/?type=https://crest-tq.eveonline.com/inventory/types/"+id+"/");
		//System.out.println(url.toString());
		State state = State.START;
		{
			try (InputStream is = url.openStream();JsonParser jsp = Json.createParser(is)){
				boolean buy = false;
				float price = 0;
				long station = 0;
				while (jsp.hasNext()){
					Event e = jsp.next();
					if (state == State.START && e == Event.KEY_NAME && jsp.getString().equals("buy")){
						if (jsp.next() == Event.VALUE_FALSE){
							buy = false;
						} else {
							buy = true;
						}
						state = State.READING;
						continue;
					}
					if (state == State.READING && e == Event.KEY_NAME && jsp.getString().equals("price")){
						jsp.next();
						price = jsp.getBigDecimal().floatValue();
						continue;
					}
					if (state == State.READING && e == Event.KEY_NAME && jsp.getString().equals("location")){
						state = State.LOCATION;
						continue;
					}
					if (state == State.LOCATION && e == Event.KEY_NAME && jsp.getString().equals("id")){
						jsp.next();
						station = jsp.getLong();
						continue;
					}
					if (state == State.LOCATION && e == Event.END_OBJECT){
						state = State.READING;
						continue;
					}
					if (state == State.READING && e == Event.KEY_NAME && jsp.getString().equals("type")){
						state = State.TYPE;
						continue;
					}
					if (state == State.TYPE && e == Event.END_OBJECT){
						state = State.READING;
						continue;
					}
					if (state == State.READING && e == Event.END_OBJECT){
						if (!stationIn || Arrays.binarySearch(stationId, station)>=0){
							if (buy){
								higherBuy = Math.max(higherBuy, price);
								nbActiveBuys++;
							} else {
								lowerSell = Math.min(lowerSell, price);
								nbActiveSells++;
							}
						}
					}
				}
		//		System.out.println(higherBuy + "\t" + nbActiveBuys);
			}
		}
		url = new URL("https://crest-tq.eveonline.com/market/"+regionlimit+"/orders/sell/?type=https://crest-tq.eveonline.com/inventory/types/"+id+"/");
	//	System.out.println(url.toString());
		state = State.START;


		try (InputStream is = url.openStream();JsonParser jsp = Json.createParser(is)){
			boolean buy = false;
			float price = 0;
			long station = 0;
			Date date = null;
			while (jsp.hasNext()){
				Event e = jsp.next();
				if (state == State.START && e == Event.KEY_NAME && jsp.getString().equals("buy")){
					if (jsp.next() == Event.VALUE_FALSE){
						buy = false;
					} else {
						buy = true;
					}
					state = State.READING;
					continue;
				}
				if (state == State.READING && e == Event.KEY_NAME && jsp.getString().equals("price")){
					jsp.next();
					price = jsp.getBigDecimal().floatValue();
					continue;
				}
				if (state == State.READING && e == Event.KEY_NAME && jsp.getString().equals("issued")){
					jsp.next();
					try {
						date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(jsp.getString());
					} catch (ParseException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					continue;
				}
				if (state == State.READING && e == Event.KEY_NAME && jsp.getString().equals("location")){
					state = State.LOCATION;
					continue;
				}
				if (state == State.LOCATION && e == Event.KEY_NAME && jsp.getString().equals("id")){
					jsp.next();
					station = jsp.getLong();
					continue;
				}
				if (state == State.LOCATION && e == Event.END_OBJECT){
					state = State.READING;
					continue;
				}
				if (state == State.READING && e == Event.KEY_NAME && jsp.getString().equals("type")){
					state = State.TYPE;
					continue;
				}
				if (state == State.TYPE && e == Event.END_OBJECT){
					state = State.READING;
					continue;
				}
				if (state == State.READING && e == Event.END_OBJECT){
					if (!stationIn || Arrays.binarySearch(stationId, station)>=0){
						if (buy){
							higherBuy = Math.max(higherBuy, price);
							nbActiveBuys++;
						} else {
							lowerSell = Math.min(lowerSell, price);
							if (date.after(yesterday)){
								nbActiveSells++;
							}
						}
					}
				}
			}
			//System.out.println(lowerSell + "\t" + nbActiveSells);
		}

		return new OrderStats(nbActiveSells,lowerSell, nbActiveBuys, higherBuy);
	}

	public static void main(String[] args){
		try {
			OrderStats o = build(14343, new long[]{60004588}, 10000030, true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Iterator<Integer> cheaperThan(double d) {
		final Iterator<Entry<Integer, OrderStats>>iter = stats.entrySet().iterator();
		return new Iterator<Integer>(){
			
			private Integer next;
			
			private void skip(){
				if (next != null){
					return;
				}
				next = null;
				while(iter.hasNext()){
					Entry<Integer, OrderStats> entr = iter.next();
					if (entr.getValue().getBid() < d){
						next = entr.getKey();
						break;
					} 
				}
			}
			
			@Override
			synchronized public boolean hasNext() {
				skip();
				return next != null;
			}

			@Override
			synchronized public Integer next() {
				if (next == null){
					skip();
				}
				if (next == null){
					throw new NoSuchElementException();
				}
				int res = next;
				next = null;
				return res;
			}
			
		};
	}
}

class Handler extends DefaultHandler {
	private int nbTraders;
	private boolean inSell = false;
	private StringBuffer chars;
	private float bid = Float.MAX_VALUE;

	private List<Float> prices = new ArrayList<Float>();
	final private int stationId;
	private boolean limitedToStation;
	private long lastStation;
	private List<Float> priceStation = new ArrayList<Float>();
	private boolean inBuy;
	private List<Float> asks = new ArrayList<Float>();
	private List<Float> asksStation = new ArrayList<Float>();
	private float ask = Float.MIN_VALUE;
	private int nbAsks;

	public int getNbAsks(){
		return nbAsks;
	}

	public float getAsk() {
		return ask;
	}

	public Handler(int station, boolean station2) {
		this.stationId = station;
		this.limitedToStation = station2;
	}

	public float getBid() {
		return bid;
	}

	public int getNbActiveTrafers() {
		return priceStation.size() *3/4;
		//return nbTraders;
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		if ("sell_orders".equals(qName)){
			inSell  = true;
		}
		if ("buy_orders".equals(qName)){
			inBuy  = true;
		}
		if ((inSell||inBuy) && "price".equals(qName)){
			chars = new StringBuffer();
		}
		if ("station".equals(qName)){
			chars = new StringBuffer();
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if ("sell_orders".equals(qName)){
			inSell = false;
		} 
		if ("buy_orders".equals(qName)){
			inBuy = false;
		} 
		if (inSell && "price".equals(qName)){
			float price = Float.parseFloat(chars.toString());
			if (!limitedToStation || lastStation == stationId){
				prices.add(price);
			}
			if (lastStation == stationId){
				priceStation.add(price);
			}
		}
		if (inBuy && "price".equals(qName)){
			float price = Float.parseFloat(chars.toString());
			if (!limitedToStation || lastStation == stationId){
				asks.add(price);
			}
			if (lastStation == stationId){
				asksStation.add(price);
			}
		}

		if ("station".equals(qName)){
			lastStation = Long.parseLong(chars.toString());
		}
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		if (chars != null){
			chars.append(ch, start, length);
		}
	}

	@Override
	public void endDocument() throws SAXException {
		if (prices.isEmpty()){
			bid = Float.MAX_VALUE;
		} else {
			bid = Collections.min(prices);
		}
		if (asks.isEmpty()){
			ask  = Float.MIN_VALUE;
		} else {
			ask = Collections.max(asks);
		}

		nbTraders = 0;
		if (!priceStation.isEmpty()){
			/* compute active traders */
			float lowStation = Collections.min(priceStation);;
			final float treshold = lowStation * 1.1f;
			nbTraders = 0;
			for (float p : priceStation){
				if (p < treshold){
					nbTraders++;
				}
			}
		}

		nbAsks = 0;
		if (!asksStation.isEmpty()){
			/* compute active traders */
			float highStation = Collections.max(asksStation);;
			final float treshold = highStation * .9f;
			nbAsks = 0;
			for (float p : asksStation){
				if (p > treshold){
					nbAsks++;
				}
			}
		}
	}


}