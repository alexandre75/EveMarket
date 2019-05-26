package lan.groland.eve.domain.market;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.concurrent.Immutable;

@Immutable
public class ShipmentSpecification {
  private int cargo;
  private Set<Integer> alreadyBought;

  public ShipmentSpecification(int cargo, Set<Integer> alreadyBought) {
    super();
    this.cargo = cargo;
    this.alreadyBought = new HashSet<>(alreadyBought);
  }

  public boolean isSatisfiedBy(Item item) {
    if (item.getVolume() <= cargo && !alreadyBought.contains(item.getId())){
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
}