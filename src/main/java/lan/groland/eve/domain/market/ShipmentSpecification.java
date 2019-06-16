package lan.groland.eve.domain.market;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.Preconditions;

/**
 * This class constraints shipments. They contain all the information the shipment
 * must verify such as destination, cash, trading slots etc.
 * All shipments are from Jita station.
 * Moreover this method encapsulate business rules a shipment must verify.
 * 
 * @author alexandre
 *
 */
@Immutable
public class ShipmentSpecification {
  private final int maxVolume;
  private final Set<Integer> alreadyBought;
  private final int maxSize;
  private final Station destination;
  private final double cashAvailable;
  private final float salesTax;

  private ShipmentSpecification(Builder builder) {
    super();
    this.maxVolume = builder.maxVolume;
    this.alreadyBought = builder.alreadyBought;
    this.destination = builder.destination;
    maxSize = builder.maxSize;
    cashAvailable = builder.cashAvailable;
    salesTax = builder.salesTax ;
  }

  public boolean isSatisfiedBy(Item item) {
    if (item.getVolume() <= maxVolume && !alreadyBought.contains(item.getItemId().typeId())){
      return true;
    } else {
      return false;
    }
  }
  
  public boolean isSatisfiedByTrade(Trade trade) {
    if (trade.unitSoldDay() < 1) {
      return false;
    }
    
    if (trade.lastMonthMargin() < .2D) {
      return false;
    }
    
    if (trade.expectedMargin() < .4D) {
      return false;
    }
    
    if (trade.dailyBenefit() < 1e6) {
      return false;
    }
    
    return true;
  }

  public boolean isSatisfiedByCargo(Cargo cargo) {
    return cargo.size() <= maxSize && cargo.getVolume() <= maxVolume 
        && cargo.requiredCapital() <= cashAvailable(); 
  }

  public Station getDestination() {
    return destination;
  }
  
  public double cashAvailable() {
    return cashAvailable;
  }
  
  public int tradingSlots() {
    return maxSize;
  }

  public static class Builder {
    private float salesTax = .05F;
    private final double cashAvailable;
    private int maxVolume = 320_000;
    private Set<Integer> alreadyBought = Collections.emptySet();
    private int maxSize = 30;
    private final Station destination;
    
    /**
     * Sets the destination
     * @param destination destination of the shipment
     */
    public Builder(Station destination, double cashAvailable) {
      this.destination = destination;
      this.cashAvailable = cashAvailable;
    }

    /**
     * Max volume of the shipment.
     * @param maxVolume the maximum volume the ship can carry.
     */
    public Builder maxVolume(int maxVolume) {
      this.maxVolume = maxVolume;
      return this;
    }

    /**
     * Blacklist of items.
     * @param alreadyBought used to provide a blacklist.
     */
    public Builder alreadyBought(Set<Integer> alreadyBought) {
      this.alreadyBought = new HashSet<>(alreadyBought);
      return this;
    }

    /**
     * Number of trading slots
     * @param tradingSlot
     */
    public Builder tradingSlot(int tradingSlot) {
      this.maxSize = tradingSlot;
      return this;
    }
    
    /**
     * Sets the tax rate which will be applied on the destination station
     * @param salesTax a tax rate 0.05 = 5%
     * @throws IllegalArgumentException if tax > 0 or < 100%
     */
    public Builder salesTax(float salesTax) throws IllegalArgumentException {
      Preconditions.checkArgument(salesTax >= 0 && salesTax < 1, "salesTax : " + salesTax);
      this.salesTax = salesTax;
      return this;
    }
 
    public ShipmentSpecification build() {
      return new ShipmentSpecification(this);
    }
  }

  public float salesTax() {
    return salesTax;
  }
}