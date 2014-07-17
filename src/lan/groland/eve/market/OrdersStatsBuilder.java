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
			OrderStats res = new OrderStats(handler.getNbTraders(), handler.getBid());
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
		if (inSell && "price".equals(qName)){
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
		if (inSell && "price".equals(qName)){
			float price = Float.parseFloat(chars.toString());
			if (!limitedToStation || lastStation == stationId){
				prices.add(price);
			}
		}
		if ("station".equals(qName)){
			lastStation = Integer.parseInt(chars.toString());
			if (inSell && lastStation == stationId){
				nbTraders ++;
			}
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
			nbTraders = 0;
		} else {
			bid = Collections.min(prices);
			final float treshold = bid * 1.05f;
			nbTraders = 0;
			for (float p : prices){
				if (p < treshold){
					nbTraders++;
				}
			}
		}
	}
	
	
}