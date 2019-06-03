package lan.groland.eve.domain.market;

import java.util.AbstractCollection;
import java.util.Comparator;
import java.util.Iterator;
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
public class Cargo extends AbstractCollection<Trade> {
  private static final Comparator<Trade> TRADE_COMPARATOR = Comparator.comparing(Trade::getBenefParJour);
  
  @GuardedBy("this")
  private final Queue<Trade> trades = new PriorityQueue<Trade>(TRADE_COMPARATOR);
  
  @GuardedBy("this")
  private int capacity;
  
  private final ShipmentSpecification specif;

  public Cargo(ShipmentSpecification spec){
    this.specif = spec;
  }

  /**
   * Add an item to the cargo.
   * @param trade
   */
  public synchronized boolean add(Trade trade) {
    addTrade(trade);
    while(!specif.isSatifiedByCargo(this)) {
      popMin();
    }
    return true;
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

  public synchronized int getVolume() {
    return capacity;
  }
  
  @Override
  public Iterator<Trade> iterator() {
    Iterator<Trade> notThreadSafe = trades.iterator();
    return new Iterator<Trade>() {

      @Override
      public boolean hasNext() {
        synchronized(Cargo.this) {
          return notThreadSafe.hasNext();
        }
      }

      @Override
      public Trade next() {
        synchronized(Cargo.this) {
          return notThreadSafe.next();
        }
      }
    };
  }
}
