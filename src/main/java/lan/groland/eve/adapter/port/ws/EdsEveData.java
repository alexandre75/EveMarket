package lan.groland.eve.adapter.port.ws;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.BodySubscribers;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow.Publisher;

import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import com.google.inject.name.Named;
import org.apache.log4j.Logger;
import org.reactivestreams.FlowAdapters;

import io.reactivex.Flowable;
import io.reactivex.Single;
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
  private static final Logger logger = Logger.getLogger(EdsEveData.class);
  
  private final String httpPrefix;

  private final HttpClient client = HttpClient.newHttpClient();
  
  public EdsEveData(@Named("eve-data.url") String httpPrefix) {
    this.httpPrefix = httpPrefix;
  }
  
  @Override
  public Flowable<OrderStats> stationOrderStatsAsync(Station station) {
    URI orderStatsUri = URI.create(httpPrefix + "/regions/" +  station.getRegionId() + "/stations/" + station.getStationIds()[0] + "/book");
    return Flowable.fromPublisher(FlowAdapters.toPublisher(ordersAsync(orderStatsUri)));
  }

  private Publisher<OrderStats> ordersAsync(URI orderStatsUri) {
    try {     
      HttpRequest request = HttpRequest.newBuilder()
          .uri(orderStatsUri)
          .header("Content-Type", "application/json")
          .build();
      
      HttpResponse<InputStream> is = client.send(request, BodyHandlers.ofInputStream());
      if (is.statusCode() == 200) {
        return new JsonStatOrderParser(is.body());
      } else {
        throw new IllegalStateException(orderStatsUri.toString() + ":" + is.statusCode());
      }
    } catch(InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    } catch(IOException e) {
      throw new UncheckedIOException(e);
    }
  }
  

  @Override
  public List<OrderStats> regionOrderStats(Region region) {
    URI orderStatsUri = URI.create(httpPrefix + "/regions/" + region.getRegionId() + "/book");
    return Flowable.fromPublisher(FlowAdapters.toPublisher(ordersAsync(orderStatsUri)))
                   .toList().blockingGet();
  }

  @Override
  public Single<Sales> medianPriceAsync(ItemId item, Region region, double buyPrice) {
    return Single.fromCallable(() -> medianPrice(item, region, buyPrice));
  }
  
  @Override
  public Sales medianPrice(ItemId item, Region region, double buyPrice) {
    try {
      int error = 0;
      while (true) {
        URI url = URI.create(httpPrefix + "/regions/" +  region.getRegionId() + "/histories/" + item.typeId());
        HttpRequest request = HttpRequest.newBuilder()
            .uri(url)
            .header("Content-Type", "application/json")
            .build();
        var response = client.send(request, resp -> resp.statusCode() == 200 ? new SalesSubscriber() 
            : BodySubscribers.replacing((Sales)null));
        if (response.statusCode() == 200) {
         return response.body();
        } else if (response.statusCode() == 404) { 
          throw new IllegalArgumentException(item + ":" + response.statusCode());
        } else if (response.statusCode() < 500 || ++error > 3){
          throw new IllegalStateException("Item : " + item + ", region:" + region + ", status:" + response.statusCode());
        } else {
          logger.warn("Retrying : Item : " + item + ", region:" + region + ", status:" + response.statusCode());
        }
      }
    } catch(IOException e) {
      throw new UncheckedIOException(item.toString(), e);
    } catch(InterruptedException e) {
      Thread.interrupted();
      throw new IllegalStateException(e);
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