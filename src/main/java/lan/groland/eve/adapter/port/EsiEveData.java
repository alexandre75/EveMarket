package lan.groland.eve.adapter.port;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import io.swagger.client.ApiException;
import io.swagger.client.api.MarketApi;
import io.swagger.client.model.GetMarketsRegionIdOrders200Ok;
import io.swagger.client.model.GetMarketsStructuresStructureId200Ok;
import lan.groland.eve.domain.market.EveData;
import lan.groland.eve.domain.market.OrderBookEmptyException;
import lan.groland.eve.domain.market.OrderStats;
import lan.groland.eve.domain.market.Sales;
import lan.groland.eve.domain.market.Station;
import lan.groland.eve.domain.market.Station.Region;

/**
 * EveData implementation over Eve ESI API
 * @author alexandre
 *
 */
public class EsiEveData implements EveData {
  private String token;

  private Map<Region, Map<Integer, OrderStats>> regionCache = new EnumMap<>(Region.class);
  private Map<Station, Map<Integer, OrderStats>> stationCache = new EnumMap<>(Station.class);
  private DocumentBuilder builder;

  EsiEveData(String token) {
    try {
      this.token = token;
      builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    } catch(ParserConfigurationException e) {
      throw new AssertionError(e);
    }
  }

  public static void initToken() throws IOException {
    //geKM_DYjJRgV23EB1D7wAn1BQFlT7VWmirrm02I6aV_rI2fuLk5l4yiXlbdK-d2E0

    //https://login.eveonline.com/oauth/authorize?response_type=token&redirect_uri=http://localhost/oauth-callback&client_id=9a241de46bc14beea5e2908ce01c0994&scope=esi-markets.structure_markets.v1
    HttpsURLConnection con = (HttpsURLConnection) new URL("https://login.eveonline.com/oauth/token").openConnection();
    con.setRequestMethod("POST");
    String basicAuth = Base64.getEncoder().encodeToString("9a241de46bc14beea5e2908ce01c0994:5I3Cxe2lTBIJcZZNxFWI2ueOZ3jzHGGhNggxM440".getBytes());
    con.setRequestProperty("Authorization", "Basic "+ basicAuth);
    con.setRequestProperty("Content-Type", "application/json");
    // Send post request
    con.setDoOutput(true);
    String post = "{\"grant_type\":\"authorization_code\",\"code\":\"brUKM2CSgqhiTPafJrYh-zSfcUqEqpDXrarmxTdJX0QG_FUrHxF57TWbtMa0OAl60\"}";
    con.getOutputStream().write(post.getBytes("UTF-8"));
    con.getOutputStream().close();

    int responseCode = con.getResponseCode();
    System.out.println("\nSending 'POST' request to URL : " + con.getURL().toString());
    System.out.println("Post parameters : " + post);
    System.out.println("Response Code : " + responseCode);

    //      BufferedReader in = new BufferedReader(
    //              new InputStreamReader(con.getInputStream()));
    //      StringBuffer response = new StringBuffer();
    //      int r = 0;
    //      while ((r = in.read()) != -1){
    //          response.append((char)r);
    //      }
    //      System.out.println(response.toString());
    //      String rtoken;
    //      try (JsonReader rdr = Json.createReader(con.getInputStream())){
    //          JsonObject obj = rdr.readObject();
    //          String token = obj.getString("access_token");
    //          rtoken = obj.getString("refresh_token");
    //          OrdersStatsBuilder.setToken(token);
    //          log.info(rtoken);
    //      }
  }

  @Override
  public OrderStats regionOrderStats(int itemId, Region region) {
    Map<Integer, OrderStats> regionStats = 
        regionCache.computeIfAbsent(region, k -> orderStats(k, null, false));
    return regionStats.get(itemId);
  }

  @Override
  public OrderStats stationOrderStats(int itemId, Station station) throws OrderBookEmptyException {
    Map<Integer, OrderStats> stationStats = 
        stationCache.computeIfAbsent(station, k -> orderStats(k.getRegion(), station, true));
    OrderStats res = stationStats.get(itemId);
    if (res == null) {
      throw new OrderBookEmptyException(itemId, station);
    } else {
      return res;
    }
  }

  private Map<Integer, OrderStats> orderStats(Region region, Station station, boolean stationIn) {
    List<Order> orders;
    if (region.isNullSec()) {
      orders = sellOrdersFromStructure(station);
    } else {
      orders = sellOrdersFromRegion(region);
    }
    Map<Integer, OrderStats> stats = new HashMap<>();
    for (Order order : orders){
      if (stationIn && -1 == Arrays.binarySearch(station.getStationIds(), order.getLocationId())) continue; 

      OrderStats stat = stats.get(order.getTypeId());
      if (stat == null){
        stat = new OrderStats(0, Float.MAX_VALUE, 0, Float.MIN_VALUE);
        stats.put(order.getTypeId(), stat);
      }
      if (order.isBuyOrder()){
        stat.newBuy(order.getPrice());
      } else {
        stat.newSell(order.getPrice(), order.getIssued().toLocalDateTime());
      }
    }
    return stats;
  }

  private List<Order> sellOrdersFromRegion(Region region) {
    List<Order> res = new ArrayList<>();
    MarketApi market = new MarketApi();
    int page = 1;
    List<GetMarketsRegionIdOrders200Ok> orders;
    do {
      while (true){
        try {
          orders = market.getMarketsRegionIdOrders("sell", region.getRegionId(), null, null, page, null, null, null);
          page++;
          System.out.println("Page : " + page);
          for (GetMarketsRegionIdOrders200Ok order : orders){
            res.add(Order.fromRegionOrders(order));
          }
          break;
        } catch(ApiException e){
          System.out.println(e.getLocalizedMessage());
        }
      }
    } while(!orders.isEmpty());
    return res;
  }

  private List<Order> sellOrdersFromStructure(Station station) {
    List<Order> res = new ArrayList<>();
    MarketApi market = new MarketApi();
    int page = 1;
    List<GetMarketsStructuresStructureId200Ok> orders;
    do {
      while (true){
        try {
          orders = market.getMarketsStructuresStructureId(station.getStationIds()[0], null, null, page, token, null, null);
          page++;
          System.out.println("Page : " + page);
          for (GetMarketsStructuresStructureId200Ok order : orders){
            res.add(Order.from(order));
          }
          break;
        } catch(ApiException e){
          System.out.println(e.getLocalizedMessage());
        }
      }
    } while(!orders.isEmpty());
    return res;
  }

  @Override
  public List<Integer> cheaperThan(double maxPrice, Station station) {
    return stationCache.computeIfAbsent(station, k -> orderStats(k.getRegion(), station, true))
        .entrySet().stream()
        .filter(entry -> entry.getValue().getBid() < maxPrice)
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
  }

  @Override
  public Sales medianPrice(int id, Region region, double buyPrice) {
    /*
     * Computes and returns :
     * - Daily sold volume : a transaction is considered a "sale" if price > JitaPrice
     * - Trading price : actually the median of 30days high price (TODO could be pondered with quantities?)
     */
    int remainingRetry = 3;
    try {
      while (true){
        try {
          URL url = new URL("https://api.eve-marketdata.com/api/item_history2.xml?char_name=Khamsila&region_ids="
              + region.getRegionId() + "&type_ids=" + id);
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
            if (priceLow >= buyPrice){
              prices.add(priceAverage);
              quantities.add(qty);
            } else if (priceHigh >= buyPrice){
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
          double price = 0;
          double quantity = quantities.get(quantities.size()/2)*2;
          if (prices.size() > 0){
            price = prices.get(prices.size()/2);
          }
          return new Sales(quantity, price);
        } catch(IOException e){
          if (--remainingRetry == 0){
            throw new UncheckedIOException(e);
          }
        }
      }
    } catch(SAXException e) {
      throw new AssertionError("Site changed?", e);
    }
  }
}
