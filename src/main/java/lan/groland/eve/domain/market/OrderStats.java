package lan.groland.eve.domain.market;

import org.threeten.bp.LocalDateTime;

public class OrderStats {
  private static LocalDateTime yesterday = LocalDateTime.now().minusDays(2);
  private int nbAsks;
  private float ask;

  private int nbTraders;
  private float bid;

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

  public OrderStats(int nbTraders, float f, int nbAsks, float a) {
    this.nbTraders = nbTraders;
    this.bid = f;
    this.nbAsks = nbAsks;
    this.ask = a;
  }

  public float getBid() {
    return bid;
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
}