package lan.groland.eve.domain.market;

import java.util.Map;
import java.util.Optional;
import static java.util.stream.Collectors.toMap;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import com.google.common.base.MoreObjects;
import com.google.inject.Inject;

import lan.groland.eve.domain.market.Station.Region;

/**
 * 
 * @author alexandre
 *
 */
@ThreadSafe
public class TradeFactory {
  private final EveData eveData;
  private final Map<ItemId, Float> buyPrices;
  
  @GuardedBy("this")
  private Map<ItemId, OrderStats> destination;
  @GuardedBy("this")
  private Region region;

  @Inject
  TradeFactory(EveData eveData) {
    this.eveData = eveData;
    buyPrices = eveData.stationOrderStats(Station.JITA).stream()
                       .collect(toMap(OrderStats::getItem, OrderStats::getBid));
  }
  
  public Optional<Trade> createOptional(Item item, ShipmentSpecification shipSpec) {
    try {
      Trade trade = null;
      if (isTradable(item)) {
        trade = create(item, shipSpec.getDestination());
        if (!shipSpec.isSatisfiedByTrade(trade)) {
          trade = null;
        }
      }
      return Optional.ofNullable(trade);
    } catch(OrderBookEmptyException e) {
      return Optional.empty();
    }
  }

  private boolean isTradable(Item item) {
    // TODO implements item filter in order to avoid exception
    return true;
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
  public Trade create(Item item, Station station) throws OrderBookEmptyException {
    if (!buyPrices.containsKey(item.getItemId())) {
      throw new OrderBookEmptyException(item.getItemId(), Station.JITA);
    }
    double buyPrice = buyPrices.get(item.getItemId());
    
    OrderStats sellStats = getDestination(station.getRegion()).get(item.getItemId());
    if (sellStats == null) {
      throw new OrderBookEmptyException(item.getItemId(), station);
    }
    Sales sales = eveData.medianPrice(item.getItemId(), station.getRegion(), buyPrice);

    return new RawTrade(item, buyPrice, sellStats, sales);
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
    private static final float FEE = .962f; //.95f;

    private Item item;
    private double buyPrice;
    private OrderStats sellStats;
    private Sales sellHistory;

    public RawTrade(Item item, double buyPrice, OrderStats sellStats, Sales sales) {
      this.item = item;
      this.buyPrice = buyPrice;
      this.sellHistory = sales;
      this.sellStats = sellStats;
    }

    @Override
    public double getBenefParJour() {
      return Math.min(unitSoldDay()/(double)(sellStats.nbSellOrders() +1), quantiteAAcheter()) * margeUnitaire();
    }

    @Override
    public double unitSoldDay() {
      return sellHistory.quantity;
    }

    private int quantiteAAcheter() {
      return (int) Math.ceil(unitSoldDay()/(double)(sellStats.nbSellOrders() +1));
    }

    private double margeUnitaire() {
      return prixDeVente() * FEE /*taxe*/ - buyPrice;
    }

    private double prixDeVente() {
      return ((sellStats.getBid() < Float.MAX_VALUE)? sellStats.getBid() : buyPrice * 1.5D);
    }

    @Override
    public int volume() {
      return (int) Math.round(unitSoldDay() * item.getVolume());
    }

    @Override
    public double capital() {
      return buyPrice * unitSoldDay();
    }

    @Override
    public double lastMonthMargin() {
      return (sellHistory.price * .975D /*taxe*/ - buyPrice) / buyPrice;
    }

    @Override
    public double expectedMargin() {
      return sellHistory.price / buyPrice - 1;
    }

    @Override
    public double benefit() {
      return Math.min(unitSoldDay() / (double)(sellStats.nbSellOrders() +1), quantiteAAcheter()) 
          * margeUnitaire();
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
      return Optional.of(new RawTrade(item, buyPrice, sellStats, sales));
    }

    @Override
    public String multiBuyString(){
      return String.format("%s x%d", item.getName(), (int)unitSoldDay());
    }
    
    @Override
    public int compareTo(Trade other) {
      return Double.compare(getBenefParJour(), other.getBenefParJour());
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
  }
}
