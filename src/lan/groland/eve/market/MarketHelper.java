package lan.groland.eve.market;

import io.swagger.client.ApiException;

import java.io.IOException;

public class MarketHelper {
	
	public static float jitaTax = 0.0379f;
	
	private MarketHelper(){
	}

	static OrdersStatsBuilder ob;
	{
		try {
			ob = OrdersStatsBuilder.newInstance(new long[]{6000376}, 10000002, true);
		} catch(ApiException e){

		}
	}
	
	public static float netJitaSell(Item item, boolean trade) throws IOException {
		
		if (trade){
			return ob.get(item.getId()).getBid() * (1-jitaTax);
		} else {
			return ob.get(item.getId()).getAsk() * (1-jitaTax);
		}
	}

	public static float jitaBuy(Item item) throws IOException {
		return ob.get(item.getId()).getBid();
	}
}
