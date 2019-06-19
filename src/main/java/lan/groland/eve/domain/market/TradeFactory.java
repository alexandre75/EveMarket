package lan.groland.eve.domain.market;

import java.util.Map;
import java.util.Optional;
import static java.util.stream.Collectors.toMap;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import org.apache.log4j.Logger;

import com.google.common.base.MoreObjects;
import com.google.inject.Inject;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import lan.groland.eve.domain.market.Station.Region;

/**
 * 
 * @author alexandre
 *
 */
@ThreadSafe
public class TradeFactory {
  private static final Logger logger = Logger.getLogger(TradeFactory.class);
  
  private final EveData eveData;
  private final Map<ItemId, Float> buyPrices;
  
  @GuardedBy("this")
  private Map<ItemId, OrderStats> destination;
  @GuardedBy("this")
  private Region region;

  @Inject
  TradeFactory(EveData eveData) {
    this.eveData = eveData;
    buyPrices = eveData.stationOrderStatsAsync(Station.JITA)
        .toMap(OrderStats::getItem, OrderStats::getBid)
        .blockingGet();
  }
  
  /**
   * Calculate financial data we could expect by shipping the given item the {@code destination}.
   * <ul>
   * <li>Load order books for origin and destination station;
   * <li>Calculate possible selling price and volume
   * <li>Derive the financial data characterizing the trade.
   * <ul>
   * @param item the item bought at Jita
   * @param station item is sold at "sell" price in this station.
   * @return the subsequent trade.
   * @throws OrderBookEmptyException
   */
  public Flowable<Trade> create(Item item, ShipmentSpecification spec) {
   return create(item, spec.getDestination(), spec.salesTax());
  }
  
  private synchronized Map<ItemId, OrderStats> getDestination(Region region){
    if (destination == null) {
      destination = eveData.regionOrderStats(region).stream()
          .collect(toMap(OrderStats::getItem, orderStats -> orderStats));
      this.region = region;
    } else {
      assert this.region == region;
    }
    return destination;
  }

  private static class RawTrade implements Trade {
    private Item item;
    private final double buyPrice;
    private OrderStats sellStats;
    private Sales sellHistory;
    private float salesTax;

    public RawTrade(Item item, double buyPrice, OrderStats sellStats, Sales sales, float salesTax) {
      this.item = item;
      this.buyPrice = buyPrice;
      this.sellHistory = sales;
      this.sellStats = sellStats;
      this.salesTax = 1F - salesTax;
    }

    @Override
    public double dailyBenefit() {
      return dailySaleForecast() * margeUnitaire();
    }

    @Override
    public double unitSoldDay() {
      return sellHistory.quantity;
    }

    public int quantiteAAcheter() {
      // we buy for 2 day
      return (int) Math.ceil(dailySaleForecast() * 2);
    }

    private double margeUnitaire() {
      return prixDeVente() * salesTax - buyPrice;
    }

    private double prixDeVente() {
      // we probably wont make more than 50%
      return ((sellStats.getBid() < Float.MAX_VALUE)? sellStats.getBid() : buyPrice * 1.5D);
    }

    @Override
    public double volume() {
      return quantiteAAcheter() * item.getVolume();
    }

    @Override
    public double capital() {
      return buyPrice * quantiteAAcheter();
    }

    @Override
    public double lastMonthMargin() {
      return (sellHistory.price * salesTax - buyPrice) / buyPrice;
    }

    @Override
    public double expectedMargin() {
      return prixDeVente() / buyPrice - 1;
    }

    private double dailySaleForecast() {
      return unitSoldDay() / (double)(sellStats.nbSellOrders() +1);
    }

    @Override
    public Optional<Trade> adjust(double maxCash) {
      if (capital() < maxCash) {
        return Optional.of(this);
      }

      double newQty = Math.floor(maxCash / buyPrice);
      if (newQty < 1) {
        return Optional.empty();
      }
      Sales sales = sellHistory.adjustQuantity(newQty);

      //euristic = euristic /quantitéJounalière*newQty;
      //quantitéJounalière = newQty;
      return Optional.of(new RawTrade(item, buyPrice, sellStats, sales, salesTax));
    }

    @Override
    public int compareTo(Trade other) {
      return Double.compare(dailyBenefit(), other.dailyBenefit());
    }
    
    @Override
    public String toString() {
      return MoreObjects.toStringHelper(TradeFactory.class)
                        .add("item", item.getName())
                        .add("qty", this.quantiteAAcheter())
                        .add("buy", this.buyPrice)
                        .add("sell", this.prixDeVente())
                        .toString();
    }

    @Override
    public Item item() {
      return item;
    }
  }

  /**
   * Calculate financial data we could expect by shipping the given item the {@code destination}.
   * <ul>
   * <li>Load order books for origin and destination station;
   * <li>Calculate possible selling price and volume
   * <li>Derive the financial data characterizing the trade.
   * <ul>
   * @param item the item bought at Jita
   * @param station item is sold at "sell" price in this station.
   * @return the subsequent trade.
   * @throws OrderBookEmptyException
   */
  public Flowable<Trade> create(Item item, Station station, float salesTax) {
    if (!buyPrices.containsKey(item.getItemId())) {
      return Flowable.error(new OrderBookEmptyException(item.getItemId(), Station.JITA));
    }
    double buyPrice = buyPrices.get(item.getItemId());
    
    OrderStats sellStats = getDestination(station.getRegion()).get(item.getItemId());
    if (sellStats == null) {
      return Flowable.error(new OrderBookEmptyException(item.getItemId(), station));
    }

    return eveData.medianPriceAsync(item.getItemId(), station.getRegion(), buyPrice)
        .map(new Function<Sales, Trade>() {
          @Override
          public Trade apply(Sales sales) throws Exception {
            try {
              if (sales.quantity == 0D){
                throw new OrderBookEmptyException(item.getItemId(), station);
              }
              return new RawTrade(item, buyPrice, sellStats, sales, salesTax);
            } catch(IllegalArgumentException e) { // sometime unknown type id are pulled off
              logger.warn("Can't get history, ignoring :" + e.getMessage());
              throw new OrderBookEmptyException(item.getItemId(), station);
            }
          }
        });
  }
}
