package lan.groland.eve.reaction;

import java.io.IOException;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

import lan.groland.eve.market.Item;
import lan.groland.eve.market.MarketHelper;
import lan.groland.eve.market.MarketStatistics;

public class Strategy {
	
	private static Logger log = Logger.getLogger(Strategy.class.getName());
	
	int totalCpu = 7500;
	
	int usedCpu = 0;
	
	interface SaleStrategyI {
		public float sellPrice(Item item);
	}
	
	enum SaleStrategy implements SaleStrategyI {
		market {
			@Override
			public float sellPrice(Item item) {
				try {
					return MarketHelper.netJitaSell(item, false);
				} catch (IOException e) {
					throw new RuntimeException("", e);
				}
			}
		}, 
		
		best_limit {
			@Override
			public float sellPrice(Item item) {
				try {
					return MarketHelper.netJitaSell(item, true);
				} catch (IOException e) {
					throw new RuntimeException("", e);
				}
			}
		}, 
		
		historical_median {
			@Override
			public float sellPrice(Item item) {
				try {
					float res = (float)MarketStatistics.getInstance().medianPrice(item.getId(), 10000002, 0f).price;
					log.fine(item.getName() + " : " + res);
					return res;
				} catch (SQLException e) {
					throw new RuntimeException("", e);
				}
			}
		}
	}
	
	private Item moonHarvested;
	
	public void setMoonHarvested(Item goo){
		moonHarvested = goo;
	}
	
	public static class Ingredients {
		public Ingredients(int qty, Item item){
			this.qty = qty;
			this.item = item;
		}
		
		public int qty;
		public Item item;
	}

	private static SaleStrategy trade;
	
	Collection<Ingredients> input = new ArrayList<Ingredients>();
	Collection<Ingredients> output = new ArrayList<Ingredients>();
	
	public float cycleProfit() throws IOException{
		return ca() - cost();
	}
	
	public float ca() throws IOException {
		return ca(trade);
	}

	public float ca(SaleStrategy sellStrat) throws IOException {
		float res = 0;
		for (Ingredients i : output){
			res += sellStrat.sellPrice(i.item) * i.qty;
		}
		return res;
	}

	public float cost() throws IOException {
		float res = 0;		
		for (Ingredients i : input){
			if (!i.item.getName().equals(moonHarvested)){
				res += MarketHelper.jitaBuy(i.item) * i.qty;
			}
		}
		return res;
	}
	
	private NumberFormat currencyFormater = NumberFormat.getNumberInstance();
	private NumberFormat percentFormater = NumberFormat.getPercentInstance();
	
	public String toString(){
		try {
			StringBuilder res = new StringBuilder();
			res.append("Output : \n");
			for (Ingredients i : output){
				res.append("  ").append(i.qty).append("x").append(i.item.getName()).append(" : ").append(trade.sellPrice(i.item));
				res.append("\tTTF: ").append(20000/(i.qty*i.item.getVolume())).append("\n");
			}
			
			res.append("Input : \n");
			double volBatch = 0;
			for (Ingredients i : input){
				res.append("  ").append(i.qty).append("x").append(i.item.getName()).append(" : ").append(MarketHelper.jitaBuy(i.item)).append("\n");
				if (i.item.getId() != 4051){
					volBatch += i.qty * i.item.getVolume();
				}
			}
			res.append("Volume/h : " + volBatch + " ; 13.4k : " + 13400/volBatch + " x batch\n");
			
			double vaMarket = ca(SaleStrategy.market) - cost();
			double vaTrade = ca(SaleStrategy.best_limit) - cost();
			double transport = transportCost();
			res.append("Gain/c : "+ currencyFormater.format(ca()-cost())).append("\n");
			res.append("Gain/j : "+ currencyFormater.format(vaMarket*24) + "("+percentFormater.format(vaMarket/cost()) 
					+ ") / " + currencyFormater.format(vaTrade*24) + "("+percentFormater.format(vaTrade/cost()) + ")");
			res.append("\n");
			res.append("Transport / j : " + currencyFormater.format(transport *24));
			res.append("\n");
			res.append("Marge/j : "+ currencyFormater.format((vaMarket-transport)*24) + "("+percentFormater.format((vaMarket-transport)/cost()) 
					+ ") / " + currencyFormater.format((vaTrade-transport)*24) + "("+percentFormater.format((vaTrade-transport)/cost())+")");
			res.append("\n");
			
			return res.toString();
		} catch(IOException e){
			throw new RuntimeException(e);
		}
	}
	
	public double transportCost(){
		double volume = 0;
		for (Ingredients i : input){
			if (!i.item.getName().equals(moonHarvested)){
				double vol = i.item.getVolume();
				if (vol == 0){
					System.out.println(i.item.getName() + " volume was 0, setting 1");
					vol = 1;
				}
				volume += i.qty * vol;
			}
		}
		for (Ingredients i : output){
			double vol = i.item.getVolume();
			if (vol == 0){
				System.out.println(i.item.getName() + " volume was 0, setting 1");
				vol = 1;
			}
			volume += i.qty * vol;
		}
		return volume * 20e6 / 60000d;
	}

	public double netReturn(int days) throws IOException {
		return netResult(days) / (cost()*24*days);
	}

	public double netResult(int days) throws IOException {
		return (ca()-cost()-transportCost())*24*days;
	}

	public void merge(Strategy strat) {
		input.addAll(strat.input);
		output.addAll(strat.output);
		usedCpu += strat.getUsedCpu();
	}

	public static void setTrade(SaleStrategy trade) {
		Strategy.trade = trade;
	}

	public int getUsedCpu() {
		return usedCpu;
	}
	
	public void addMoonHarvesterIfPossible(){
		if (freeCpu() >= 1000){
			boolean alreadyUsed = false;
			for (Ingredients i : input){
				if (i.item.equals(moonHarvested)){
					alreadyUsed = true;
				}
			}
			for (Ingredients i : output){
				if (i.item.equals(moonHarvested)){
					alreadyUsed = true;
				}
			}
			if (!alreadyUsed){
				usedCpu += 1000;
				output.add(new Ingredients(100, moonHarvested));
			}
		}
	}

	public int freeCpu() {
		return totalCpu - usedCpu;
	}
}
