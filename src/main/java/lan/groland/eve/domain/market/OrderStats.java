package lan.groland.eve.domain.market;

import java.util.Objects;

import org.threeten.bp.LocalDateTime;

import com.google.common.base.Preconditions;

public class OrderStats {
  private static LocalDateTime yesterday = LocalDateTime.now().minusDays(2);
  private int nbAsks;
  private float ask;

  private int nbTraders;
  private double bid;
  private final ItemId itemId;

  /**
   * Number of <b>active</b> traders (buy orders).
   * @return
   */
  public int getNbAsks() {
    return nbAsks;
  }

  public float getAsk() {
    return ask;
  }

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

  public void newSell(Double price, LocalDateTime offsetDateTime) {
    if (offsetDateTime.compareTo(yesterday) >= 0){
      nbTraders++;
    }
    bid = (float) Math.min(bid, price);
  }

  public double expSellPrice(double buyPrice, double medianPrice) {
    double prixDeVente = ((getBid() < Float.MAX_VALUE)? getBid() : buyPrice*1.5);
    prixDeVente = Math.min(1.5 * medianPrice, prixDeVente); // on ne vendra probablement pas à n'importe quel prix
    return prixDeVente;
  }

  public ItemId getItem() {
    return itemId;
  }
}
