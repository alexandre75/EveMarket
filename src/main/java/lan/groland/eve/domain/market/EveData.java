package lan.groland.eve.domain.market;

import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Single;
import lan.groland.eve.domain.market.Station.Region;

/**
 * Interface used to fetch RT data
 * @author alexandre
 *
 */
public interface EveData {
  
  /**
   * Returns a summary of the order book in the given station
   * @param station a station
   */
  Flowable<OrderStats> stationOrderStatsAsync(Station station);

  /**
   * Returns a summary of the order book for a given id and region.
   * @param item the item of interest
   * @param region the scope of the survey
   * @throws OrderBookEmptyException the book is empty
   */
  List<OrderStats> regionOrderStats(Station.Region region);

  /**
   * Returns a summary of the last month sales for an item in a region.
   * @param id item the item of interest
   * @param region the scope of the survey
   * @param buyPrice price at which we intend to buy the item.
   */
  Sales medianPrice(ItemId item, Region region, double buyPrice);

  default List<OrderStats> stationOrderStats(Station jita) {
    return stationOrderStatsAsync(jita).toList().blockingGet();
  }

  Single<Sales> medianPriceAsync(ItemId item, Region region, double buyPrice);
}
