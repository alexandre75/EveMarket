package lan.groland.eve.domain.market;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.google.inject.Inject;

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
    List<Item> items =  eveData.stationOrderStats(Station.JITA)
                               .stream().filter(os -> os.getBid() < shipSpec.cashAvailable() / 10)
                               .map(os -> itemRepository.find(os.getItem()))
                               .collect(Collectors.toList());
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
  public Collection<Trade> load(Collection<Item> items, ShipmentSpecification shipSpec) {
    logger.info("Optimizing the cargo");
    Cargo trades = new Cargo(shipSpec); 
    items.parallelStream()
         .filter(shipSpec::isSatisfiedBy)
         .map(item -> tradeFactory.createOptional(item, shipSpec))
         .flatMap(Optional::stream)
         .map(trade -> trade.adjust(shipSpec.cashAvailable() / (double) shipSpec.tradingSlots()))
         .flatMap(Optional::stream)
         .forEach(trades::add);
    return trades;
  }
}
