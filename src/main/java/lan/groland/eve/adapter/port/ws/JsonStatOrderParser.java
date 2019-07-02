package lan.groland.eve.adapter.port.ws;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import lan.groland.eve.domain.market.ItemId;
import lan.groland.eve.domain.market.OrderStats;

/**
 * Adapter that read a json stream, parse it and publishes the resulting OrderStats.
 * @author alexandre
 *
 */
public class JsonStatOrderParser {
  private InputStream inputStream;

  public JsonStatOrderParser(InputStream is) {
    inputStream = is;
  }

  public List<OrderStats> parse() {
    JsonParser parser = Json.createParser(inputStream);
    if (parser.next() != Event.START_ARRAY) {
      throw new IllegalStateException("invalid json from server");
    }

    ItemId typeId = null;
    int nbTraders = 0;
    double bid = 0D;

    List<OrderStats> res = new ArrayList<>();

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
        res.add(new OrderStats(typeId, nbTraders, bid));
      }
    }
    return res;
  }
}
