package lan.groland.eve.domain.market;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

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
	
	synchronized public void add(Trade trade) {
		if (trade.getBenefParJour() <= min && trades.size() >= size) return;
		
		if (trades.size() >= size){
			Trade t = Collections.min(trades, TradeComparator.instance());
			System.out.println("Suppression Trade pas rentable : " + t);
			trades.remove(t);
			capacity += t.volume();
			try {
				min = Collections.min(trades, TradeComparator.instance()).getBenefParJour();
			} catch(NoSuchElementException e){
				min = Double.MAX_VALUE;
			}
			System.out.println("Capacity : " + capacity);
		}
		if (capacity < trade.volume()){
			List<Trade> tmp = new ArrayList<Trade>(trades);
			Collections.sort(tmp, new Comparator<Trade>(){
				@Override
				public int compare(Trade arg0, Trade arg1) {
					return (int)(arg0.getBenefParJour()/arg0.volume() - arg1.getBenefParJour()/arg1.volume());
				}
			});
			double volumeLibere = 0;
			int npItemsAEnlever = 0;
			double perte = 0;
			for (Trade t : tmp){
				volumeLibere += t.volume();
				npItemsAEnlever++;
				perte += t.getBenefParJour();
				if (volumeLibere + capacity >= trade.volume()){
					break;
				}
			}
			if (npItemsAEnlever == tmp.size() || perte > trade.getBenefParJour()){
				return;
			}
			for (int i = 0; i < npItemsAEnlever; i++){
				System.out.println("Suppression trade trop lourd : " + tmp.get(i));
				trades.remove(tmp.get(i));
				capacity += tmp.get(i).volume();
				System.out.println("Capacity : " + capacity);
			}
			min = Collections.min(trades, TradeComparator.instance()).getBenefParJour();
		}
		addTrade(trade);
		System.out.println("Capacity : " + capacity);
	}
	
	protected void addTrade(Trade t){
		System.out.println("ajout : " + t);
		trades.add(t);
		min = Math.min(min, t.getBenefParJour());
		capacity -= t.volume();
	}

	public String toString(){
		Collections.sort(trades, new TradeComparator());
		float benef = 0, invest = 0;
		StringBuilder b = new StringBuilder("=============================================\n");
		for (Trade t : trades){
			b.append(t + "\n");
			benef+= t.getBenefParJour();
			invest += t.capital();
		}
		b.append("Benefice potentiel :" + intFormat.format(benef) + " / " + intFormat.format(invest) + ": " +NumberFormat.getPercentInstance().format(benef/invest)) ;
		return b.toString();
	}
	
	public String multiBuyString(){
		Collections.sort(trades, new TradeComparator());
		StringBuilder b = new StringBuilder("=============================================\n");
		for (Trade t : trades){
			b.append(t.multiBuyString() + "\n");
		}
		return b.toString();
	}
}

class TradeComparator implements Comparator<Trade> {
	private static TradeComparator instance = new TradeComparator();
	
	@Override
	public int compare(Trade arg0, Trade arg1) {
		return Math.round((float)(arg0.getBenefParJour() - arg1.getBenefParJour()));
	}

	public static TradeComparator instance() {
		return instance;
	}
	
};
