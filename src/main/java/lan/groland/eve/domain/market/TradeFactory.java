package lan.groland.eve.domain.market;

import java.util.Optional;

import javax.annotation.concurrent.Immutable;

import com.google.inject.Inject;

@Immutable
public class TradeFactory {
  private final EveData eveData;

  @Inject
  TradeFactory(EveData eveData) {
    this.eveData = eveData;
  }
  
  public Optional<Trade> createOptional(Item item, Station station) {
    try {
      return Optional.of(create(item, station));
    } catch(OrderBookEmptyException e) {
      return Optional.empty();
    }
  }

  public Trade create(Item item, Station station) throws OrderBookEmptyException {
    double buyPrice = eveData.stationOrderStats(item.getId(), Station.JITA).getBid();
    OrderStats sellStats = eveData.regionOrderStats(item.getId(), station.getRegion());
    Sales sales = eveData.medianPrice(item.getId(), station.getRegion(), buyPrice);

    return new RawTrade(item, buyPrice, sellStats, sales);
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
      return (sellHistory.price * .975f /*taxe*/ - buyPrice) / buyPrice;
    }

    @Override
    public double expectedMargin() {
      return sellHistory.price / buyPrice - 1;
    }

    @Override
    public double benefit() {
      return Math.min(unitSoldDay() / (double)(sellStats.nbSellOrders() +1), quantiteAAcheter()) * margeUnitaire();
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
  }
  
  
}
