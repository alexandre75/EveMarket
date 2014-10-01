package lan.groland.eve.market;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
	 
	
	public static OrderStats build(int id, int stationId, int regionlimit) throws IOException {
		return build(id, stationId, regionlimit, false);
	}


	public static OrderStats build(int id, int stationId, int regionlimit, boolean station) throws IOException {
		try {
			URL url = new URL("http://api.eve-central.com/api/quicklook?typeid="+id+"&regionlimit="+regionlimit);
			URLConnection conn = url.openConnection();
			SAXParser saxParser = spf.newSAXParser();
			XMLReader xmlReader = saxParser.getXMLReader();
			Handler handler = new Handler(stationId, station);
			xmlReader.setContentHandler(handler);
			InputSource source = new InputSource(conn.getInputStream());
			xmlReader.parse(source);
			OrderStats res = new OrderStats(handler.getNbTraders(), handler.getBid(), handler.getNbAsks(), handler.getAsk());
			return res;
		} catch (MalformedURLException e) {
			throw new RuntimeException("", e);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("", e);
		} catch (SAXException e) {
			throw new RuntimeException("", e);
		}
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
	private int lastStation;
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

	public int getNbTraders() {
		return nbTraders;
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
			lastStation = Integer.parseInt(chars.toString());
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