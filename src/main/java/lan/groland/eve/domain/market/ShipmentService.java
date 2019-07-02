package lan.groland.eve.domain.market;

import java.util.Collection;
import java.util.Optional;
import java.util.logging.Logger;

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
  private static Logger logger = Logger.getLogger(ShipmentService.class.getName());

  private final EveData eveData;
  private final ItemRepository itemRepository;

  @Inject
  ShipmentService(EveData eveData, ItemRepository itemRepository) {
    super();
    this.eveData = eveData;
    this.itemRepository = itemRepository;
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
  public Collection<Trade> optimizeCargo(ShipmentSpecification shipSpec, TradeFactory tradeFactory) {
    logger.info("Loading items for sale and pre filtering");
    Flowable<Item> items = eveData.stationOrderStatsAsync(Station.JITA)
        .filter(os -> os.getBid() < shipSpec.cashAvailable() / 10)
        .flatMap(os -> itemRepository.findAsync(os.getItem()));
    return load(items, shipSpec, tradeFactory);
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
  public Collection<Trade> load(Flowable<Item> items, ShipmentSpecification shipSpec, TradeFactory tradeFactory) {
    logger.info("Optimizing the cargo");

    Cargo trades = new Cargo(shipSpec);
    items.filter(shipSpec::isSatisfiedBy)
         .flatMap(item -> tradeFactory.trade(item, shipSpec).subscribeOn(Schedulers.io()).toFlowable()
                                      .onExceptionResumeNext(Flowable.empty()))
         .filter(shipSpec::isSatisfiedByTrade)
         .map(trade -> trade.adjust(shipSpec.cashAvailable() / (double) shipSpec.tradingSlots()))
         .filter(Optional::isPresent)
         .map(Optional::get)
         .blockingSubscribe(trades::add);
    logger.fine("... cargo optimized");
    return trades;
  }
}
