package lan.groland.eve.adapter.port;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import lan.groland.eve.domain.market.EveData;
import lan.groland.eve.domain.market.ItemId;
import lan.groland.eve.domain.market.OrderStats;
import lan.groland.eve.domain.market.Sales;
import lan.groland.eve.domain.market.Station;
import lan.groland.eve.domain.market.Station.Region;

/**
 * Implementation Eve Data Service
 * @author alexandre
 *
 */
public class EdsEveData implements EveData {
  
  private final String httpPrefix;
  
  public EdsEveData(String httpPrefix) {
    this.httpPrefix = httpPrefix;
  }

  @Override
  public List<OrderStats> stationOrderStats(Station station) {
    try {
      URL url = new URL(httpPrefix + "/regions/" +  station.getRegionId() + "/stations/" + station.getStationIds()[0] + "/books");
      try (InputStream is = url.openStream()){
        OrderStatsTranslator translator = new OrderStatsTranslator(is);
          return translator.parse();
      }
    } catch(MalformedURLException e) {
      throw new AssertionError(e);
    } catch(IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public List<OrderStats> regionOrderStats(Region region) {
    try {
      URL url = new URL(httpPrefix + "/traders?region_id=" + region.getRegionId());
      try (InputStream is = url.openStream()){
        OrderStatsTranslator translator = new OrderStatsTranslator(is);
          return translator.parse();
      }
    } catch(MalformedURLException e) {
      throw new AssertionError(e);
    } catch(IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public Sales medianPrice(ItemId item, Region region, double buyPrice) {
    try {
      URL url = new URL(httpPrefix + "/regions/" +  region.getRegionId() + "/histories/" + item.typeId());
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      if (connection.getResponseCode() == 200) {
        try (InputStream is = connection.getInputStream()) {
          JsonReader historiesReader = Json.createReader(is);
          JsonObject historiesObj = historiesReader.readObject();
          Sales sales = new Sales(historiesObj.getJsonNumber("quantity").doubleValue(),
                                  historiesObj.getJsonNumber("median").doubleValue());
          return sales;
        }
      } else {
        throw new IllegalStateException("Item : " + item + ", region:" + region);
      }
    } catch(MalformedURLException e) {
      throw new AssertionError(e);
    } catch(IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}

class OrderStatsTranslator {
  private InputStream is;
  
  public OrderStatsTranslator(InputStream is) {
    super();
    this.is = is;
  }

  public List<OrderStats> parse() {
    try (JsonParser parser = Json.createParser(is)){
      if (parser.next() != Event.START_ARRAY) {
        throw new WrongFormatException("json resource should contain an array");
      }
      
      List<OrderStats> books = new ArrayList<>();
      
      ItemId typeId = null;
      int nbTraders = 0;
      double bid = 0D;
      
      while(parser.hasNext()) {
        Event e = parser.next();
        if (e == Event.KEY_NAME) {
          switch(parser.getString()) {
          case "type_id":
            parser.next();
            typeId = new ItemId(parser.getInt());
            break;
            
          case "nb_active_trader":
            parser.next();
            nbTraders = parser.getInt();
            break;
            
          case "bid":
            parser.next();
            bid = parser.getBigDecimal().doubleValue();
            break;
          }
        } else if (e == Event.END_OBJECT){
          books.add(new OrderStats(typeId, nbTraders, bid));
        }
      }
      return books;
    }
  }
}