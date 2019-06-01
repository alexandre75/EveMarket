package lan.groland.eve.domain.market;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.concurrent.Immutable;

@Immutable
public class ShipmentSpecification {
  private final int maxVolume;
  private final Set<Integer> alreadyBought;
  private final int maxSize = 30;

  public ShipmentSpecification(int cargo, Set<Integer> alreadyBought) {
    super();
    this.maxVolume = cargo;
    this.alreadyBought = new HashSet<>(alreadyBought);
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
    
    if (trade.benefit() < 1e6) {
      return false;
    }
    
    return true;
  }

  public boolean isSatifiedByCargo(Cargo cargo) {
    return cargo.size() <= maxSize && cargo.getVolume() <= maxVolume;
  }
}