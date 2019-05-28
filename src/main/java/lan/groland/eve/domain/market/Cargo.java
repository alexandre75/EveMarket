package lan.groland.eve.domain.market;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

/**
 * List des meilleurs trades
 * @author alexandre
 *
 */
@ThreadSafe
public class Cargo {
  private static final Comparator<Trade> TRADE_COMPARATOR = Comparator.comparing(Trade::getBenefParJour);
  private static NumberFormat intFormat = NumberFormat.getIntegerInstance();
  
  @GuardedBy("this")
  private final Queue<Trade> trades = new PriorityQueue<Trade>(TRADE_COMPARATOR);
  
  @GuardedBy("this")
  private int capacity;
  private ShipmentSpecification specif;

  public Cargo(ShipmentSpecification spec){
    this.specif = spec;
  }

  /**
   * Add an item to the cargo.
   * @param trade
   */
  public synchronized void add(Trade trade) {
    addTrade(trade);
    while(!specif.isSatifiedByCargo(this)) {
      popMin();
    }
  }

  private void popMin() {
    capacity -= trades.poll().volume();
  }

  public synchronized int size() {
    return trades.size();
  }
  
  private void addTrade(Trade t){
    System.out.println("ajout : " + t);
    trades.add(t);
    capacity += t.volume();
  }

  public String toString(){
    List<Trade> sortedTrade = new ArrayList<>(trades);
    Collections.sort(sortedTrade, TRADE_COMPARATOR);
    float benef = 0, invest = 0;
    StringBuilder b = new StringBuilder("=============================================\n");
    for (Trade t : sortedTrade){
      b.append(t + "\n");
      benef += t.getBenefParJour();
      invest += t.capital();
    }
    b.append("Benefice potentiel :" + intFormat.format(benef) + " / " + intFormat.format(invest) + ": " +NumberFormat.getPercentInstance().format(benef/invest)) ;
    return b.toString();
  }

  public String multiBuyString(){
    List<Trade> sortedTrade = new ArrayList<>(trades);
    Collections.sort(sortedTrade, TRADE_COMPARATOR);
    StringBuilder b = new StringBuilder("=============================================\n");
    for (Trade t : sortedTrade){
      b.append(t.multiBuyString() + "\n");
    }
    return b.toString();
  }

  public synchronized int getVolume() {
    return capacity;
  }
}
