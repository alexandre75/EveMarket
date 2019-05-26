package lan.groland.eve.domain.market;

import java.util.List;

import lan.groland.eve.domain.market.Station.Region;

/**
 * Interface used to fetch RT data
 * @author alexandre
 *
 */
public interface EveData {

  OrderStats regionOrderStats(int id, Station.Region region);

  OrderStats stationOrderStats(int id, Station jita) throws OrderBookEmptyException;

  /**
   * Returns the typeId of all the items cheaper than the given value in the given region.
   * @param maxPrice cap price
   * @param station the station scope 
   */
  List<Integer> cheaperThan(double maxPrice, Station station);

  Sales medianPrice(int id, Region region, double buyPrice);
}
