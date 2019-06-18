package lan.groland.eve.domain.market;

import java.util.Collection;
import java.util.Optional;

import org.apache.log4j.Logger;

import com.google.inject.Inject;

import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;

/**
 * Shipment optimization services
 * @author alexandre
 *
 */
public class ShipmentService {
  @SuppressWarnings("unused")
  private static Logger logger = Logger.getLogger(ShipmentService.class);

  private final EveData eveData;
  private final ItemRepository itemRepository;
  private final TradeFactory tradeFactory;

  @Inject
  ShipmentService(EveData eveData, ItemRepository itemRepository, TradeFactory tradeFactory) {
    super();
    this.eveData = eveData;
    this.itemRepository = itemRepository;
    this.tradeFactory = tradeFactory;
  }

  /**
   * Load a cargo with a selection of the given items.
   * Items are selected among those available at Jita so that :
   * <ul>
   * <li>They satisfy the given specifications;
   * <li>Benefits are optimized
   * <ul>
   * @param shipSpec specification for the operation.
   * @return an optimized cargo
   * @see ShipmentSpecification
   */
  public Collection<Trade> optimizeCargo(ShipmentSpecification shipSpec) {
    logger.info("Loading items for sale and pre filtering");
    Flowable<Item> items = eveData.stationOrderStatsAsync(Station.JITA)
        .observeOn(Schedulers.io())
        .filter(os -> os.getBid() < shipSpec.cashAvailable() / 10)
        .flatMap(os -> itemRepository.findAsync(os.getItem()));
    return load(items, shipSpec);
  }
  
  /**
   * Load a cargo with a selection of the given items.
   * Items are selected so that :
   * <ul>
   * <li>They satisfy the given specifications;
   * <li>Benefits are optimized
   * <ul>
   * @param items The items available to trade.
   * @param shipSpec specification for the operation.
   * @return an optimized cargo
   * @see ShipmentSpecification
   */
  public Collection<Trade> load(Flowable<Item> items, ShipmentSpecification shipSpec) {
    logger.info("Optimizing the cargo");
    Cargo trades = new Cargo(shipSpec);
    items.filter(shipSpec::isSatisfiedBy)
         .map(item -> tradeFactory.createOptional(item, shipSpec))
         .filter(Optional::isPresent)
         .map(trade -> trade.get().adjust(shipSpec.cashAvailable() / (double) shipSpec.tradingSlots()))
         .filter(Optional::isPresent)
         .map(Optional::get)
         .blockingSubscribe(trades::add);
    return trades;
  }
}
