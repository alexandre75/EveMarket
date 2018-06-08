package lan.groland.eve.market;

import io.swagger.client.ApiException;
import io.swagger.client.api.MarketApi;
import io.swagger.client.model.GetMarketsRegionIdOrders200Ok;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import javax.xml.parsers.SAXParserFactory;

public class OrdersStatsBuilder {

	private static SAXParserFactory spf = SAXParserFactory.newInstance();
	{
		spf.setNamespaceAware(true);
	}

	enum State {
		START,READING,LOCATION,TYPE
	}
	
	public OrdersStatsBuilder(){
		
	}
	
	private Map<Integer, OrderStats> stats;
	
	public void init(int region, long[] stationId, boolean stationIn) throws ApiException{
		stats = new HashMap<>();
		MarketApi market = new MarketApi();
		int page = 1;
		List<GetMarketsRegionIdOrders200Ok> orders;
		do {
			while (true){
				try {
					orders = market.getMarketsRegionIdOrders("sell", (Integer)region, null, null, page, null, null, null);
					page++;
					System.out.println("Page : " + page);
					for (GetMarketsRegionIdOrders200Ok order : orders){
						if (stationIn && -1 == Arrays.binarySearch(stationId, order.getLocationId())) continue;	

						OrderStats stat = stats.get(order.getTypeId());
						if (stat == null){
							stat = new OrderStats(0, Float.MAX_VALUE, 0, Float.MIN_VALUE);
							stats.put(order.getTypeId(), stat);
						}
						if (order.isIsBuyOrder()){
							stat.newBuy(order.getPrice());
						} else {
							stat.newSell(order.getPrice(), order.getIssued().toLocalDateTime());
						}
					}
					break;
				} catch(ApiException e){
					System.out.println(e.getLocalizedMessage());
				}
			}
		} while(!orders.isEmpty());
	}
	
	public OrderStats get(int id){
		return stats.get(id);
	}

	
	public static OrdersStatsBuilder newInstance(long[] stationId, int regionlimit, boolean stationIn) throws ApiException{
		OrdersStatsBuilder o = new OrdersStatsBuilder();
		o.init(regionlimit, stationId, stationIn);
		return o;
	}

	public Iterator<Integer> cheaperThan(double d) {
		final Iterator<Entry<Integer, OrderStats>>iter = stats.entrySet().iterator();
		return new Iterator<Integer>(){
			
			private Integer next;
			
			private void skip(){
				if (next != null){
					return;
				}
				next = null;
				while(iter.hasNext()){
					Entry<Integer, OrderStats> entr = iter.next();
					if (entr.getValue().getBid() < d){
						next = entr.getKey();
						break;
					} 
				}
			}
			
			@Override
			synchronized public boolean hasNext() {
				skip();
				return next != null;
			}

			@Override
			synchronized public Integer next() {
				if (next == null){
					skip();
				}
				if (next == null){
					throw new NoSuchElementException();
				}
				int res = next;
				next = null;
				return res;
			}
			
		};
	}
}
