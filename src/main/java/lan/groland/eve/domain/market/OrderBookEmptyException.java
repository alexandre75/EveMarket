package lan.groland.eve.domain.market;

/**
 * Sent when an operation could not proceed because there's no current orders.
 * @author alexandre
 *
 */
public class OrderBookEmptyException extends Exception {
  /**
   * 
   */
  private static final long serialVersionUID = 161949881736019400L;
  
  private final int itemId;
  private final Station station;

  public OrderBookEmptyException(int itemId, Station station) {
    super("No book for " + itemId + " at " + station);
    this.station = station;
    this.itemId = itemId;
  }

  public int getItemId() {
    return itemId;
  }

  public Station getStation() {
    return station;
  }

}
