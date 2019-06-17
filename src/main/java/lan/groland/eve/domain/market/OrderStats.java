package lan.groland.eve.domain.market;

import java.util.Objects;

import com.google.common.base.Preconditions;

public class OrderStats {
  private int nbAsks;
  private float ask;

  private int nbTraders;
  private double bid;
  private final ItemId itemId;

  public OrderStats(ItemId item, int nbTraders, double bid) {
    this(item, nbTraders, bid, -1, -1F);
  }
  
  public OrderStats(ItemId item, int nbTraders, double bid, int nbAsks, float ask) {
    Preconditions.checkArgument(nbTraders >= 0, "nbTraders > 0");
    Preconditions.checkArgument(bid >= 0, "bid > 0");
    
    this.nbTraders = nbTraders;
    this.bid = bid;
    this.nbAsks = nbAsks;
    this.ask = ask;
    itemId = Objects.requireNonNull(item, "item");
  }

  public float getBid() {
    return (float) bid;
  }

  /**
   * Number of <b>active</b> traders (sell orders).
   * @return
   */
  public int nbSellOrders() {
    return nbTraders;
  }

  public void newBuy(Double price) {
    nbAsks++;
    ask = (float)Math.max(ask, price);
  }

  public ItemId getItem() {
    return itemId;
  }

  @Override
  public String toString() {
    return "OrderStats [nbTraders=" + nbTraders + ", bid=" + bid + ", itemId=" + itemId + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Float.floatToIntBits(ask);
    long temp;
    temp = Double.doubleToLongBits(bid);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result + ((itemId == null) ? 0 : itemId.hashCode());
    result = prime * result + nbAsks;
    result = prime * result + nbTraders;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    OrderStats other = (OrderStats) obj;
    if (Float.floatToIntBits(ask) != Float.floatToIntBits(other.ask))
      return false;
    if (Double.doubleToLongBits(bid) != Double.doubleToLongBits(other.bid))
      return false;
    if (itemId == null) {
      if (other.itemId != null)
        return false;
    } else if (!itemId.equals(other.itemId))
      return false;
    if (nbAsks != other.nbAsks)
      return false;
    if (nbTraders != other.nbTraders)
      return false;
    return true;
  }
}
