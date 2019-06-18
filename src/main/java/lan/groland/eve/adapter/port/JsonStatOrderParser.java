package lan.groland.eve.adapter.port;

import java.io.InputStream;
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
public class JsonStatOrderParser implements Publisher<OrderStats> {
  private boolean subscribed;
  private InputStream inputStream;

  public JsonStatOrderParser(InputStream is) {
    inputStream = is;
  }

  @Override
  public synchronized void subscribe(Subscriber<? super OrderStats> subscriber) {
    if (subscribed) {
      throw new IllegalStateException("Can only be subscribed once");
    } else {
      subscribed = true;
      subscriber.onSubscribe(new Parser(inputStream, subscriber));
    }
  }
  
}

class Parser implements Subscription {
  private final JsonParser parser;
  private final Subscriber<? super OrderStats> subscriber;
  private volatile boolean stopped;
  
  public Parser(InputStream is, Subscriber<? super OrderStats> subscriber) {
    this.subscriber = subscriber;
    this.parser = Json.createParser(is);
    if (parser.next() != Event.START_ARRAY) {
      throw new IllegalStateException("invalid json from server");
    }
  }

  @Override
  public void cancel() {
    stopped = true;
    new Thread(() -> {
      synchronized (Parser.this) {
        parser.close();
      }
    }).start();
  }

  @Override
  public void request(long nbRequested) {
    for (int i = 0 ; i < nbRequested ; i++) {
      OrderStats order = next();
      if (order == null) {
        subscriber.onComplete();
        break;
      } else {
        subscriber.onNext(order);
      }
    }
  }
  
  private synchronized OrderStats next() {
    if (stopped) return null;
    
    OrderStats next = null;
    ItemId typeId = null;
    int nbTraders = 0;
    double bid = 0D;
    while(next == null && parser.hasNext()) {
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
        next = new OrderStats(typeId, nbTraders, bid);
      }
    }
    if (!parser.hasNext()) {
      parser.close();
    }
    return next;
  }
}