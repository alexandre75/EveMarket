package lan.groland.eve.market;

import java.io.IOException;
import java.util.HashMap;

public class MarketHelper {
	
	public static float jitaTax = 0.0379f;
	
	private MarketHelper(){
	}

	private static HashMap<Integer, OrderStats> cache = new HashMap<Integer, OrderStats>();

	
	private static OrderStats orderStatsFromCache(int id) throws IOException{
		OrderStats res = cache.get(id);
		if (res == null){
			res = OrdersStatsBuilder.build(id, new long[]{6000376}, 10000002, true);
			cache.put(id, res);
		}
		return res;
	}
	
	public static float netJitaSell(Item item, boolean trade) throws IOException {
		
		if (trade){
			return orderStatsFromCache(item.getId()).getBid() * (1-jitaTax);
		} else {
			return orderStatsFromCache(item.getId()).getAsk() * (1-jitaTax);
		}
	}

	public static float jitaBuy(Item item) throws IOException {
		return orderStatsFromCache(item.getId()).getBid();
	}
}
