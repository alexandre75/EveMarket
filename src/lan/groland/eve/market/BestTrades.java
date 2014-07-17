package lan.groland.eve.market;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * List des meilleurs trades
 * @author alexandre
 *
 */
public class BestTrades {
	private double min = Double.MAX_VALUE;
	private List<Trade> trades = new LinkedList<Trade>();
	private int size;
	private int capacity;
	public BestTrades(int nbTransaction, int capacity){
		this.size = nbTransaction;
		this.capacity = capacity;
	}
	
	private static NumberFormat intFormat = NumberFormat.getIntegerInstance();
	
	public void add(Trade trade) {
		if (trade.getMargeParTrader() <= min && trades.size() >= size) return;
		
		if (trades.size() >= size){
			Trade t = Collections.min(trades, TradeComparator.instance());
			System.out.println("Suppression Trade pas rentable : " + t);
			trades.remove(t);
			capacity += t.volume();
			min = Collections.min(trades, TradeComparator.instance()).getMargeParTrader();
			System.out.println("Capacity : " + capacity);
		}
		if (capacity < trade.volume()){
			List<Trade> tmp = new ArrayList<Trade>(trades);
			Collections.sort(tmp, new Comparator<Trade>(){
				@Override
				public int compare(Trade arg0, Trade arg1) {
					return (int)(arg0.getMargeParTrader()/arg0.volume() - arg1.getMargeParTrader()/arg1.volume());
				}
			});
			double volumeLibere = 0;
			int npItemsAEnlever = 0;
			double perte = 0;
			for (Trade t : tmp){
				volumeLibere += t.volume();
				npItemsAEnlever++;
				perte += t.getMargeParTrader();
				if (volumeLibere + capacity >= trade.volume()){
					break;
				}
			}
			if (npItemsAEnlever == tmp.size() || perte > trade.getMargeParTrader()){
				return;
			}
			for (int i = 0; i < npItemsAEnlever; i++){
				System.out.println("Suppression trade trop lourd : " + tmp.get(i));
				trades.remove(tmp.get(i));
				capacity += tmp.get(i).volume();
				System.out.println("Capacity : " + capacity);
			}
			min = Collections.min(trades, TradeComparator.instance()).getMargeParTrader();
		}
		System.out.println("ajout : " + trade);
		trades.add(trade);
		min = Math.min(min, trade.getMargeParTrader());
		capacity -= trade.volume();
		System.out.println("Capacity : " + capacity);
	}

	public String toString(){
		Collections.sort(trades, new TradeComparator());
		float benef = 0;
		StringBuilder b = new StringBuilder("=============================================\n");
		for (Trade t : trades){
			b.append(t + "\n");
			benef+= t.benefice();
		}
		b.append("Benefice potentiel :" + intFormat.format(benef));
		return b.toString();
	}
}

class TradeComparator implements Comparator<Trade> {
	private static TradeComparator instance = new TradeComparator();
	
	@Override
	public int compare(Trade arg0, Trade arg1) {
		return Math.round((float)(arg0.getMargeParTrader() - arg1.getMargeParTrader()));
	}

	public static TradeComparator instance() {
		return instance;
	}
	
};
