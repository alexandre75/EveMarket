package lan.groland.eve.domain.market;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.google.inject.Inject;

public class ShipmentService {
  @SuppressWarnings("unused")
  private static Logger log = Logger.getLogger(ShipmentService.class);

  private static final int TRADING_SLOT = 30;

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

  public Cargo optimizeCargo(Station station, double cash, ShipmentSpecification shipSpec) {
      List<Item> items =  eveData.cheaperThan(cash / 10, Station.JITA)
                                 .stream()
                                 .map(itemRepository::find)
                                 .filter(shipSpec::isSatisfiedBy)
                                 .collect(Collectors.toList());
    return load(items, station, cash, shipSpec);
  }
  
  public Cargo load(Collection<Item> items, Station station, double cash, ShipmentSpecification shipSpec) {
    Cargo trades = new Cargo(shipSpec); 
    items.parallelStream()
         .map(item -> tradeFactory.createOptional(item, station))
         .flatMap(Optional::stream)
         .filter(shipSpec::isSatisfiedByTrade)
         .map(trade -> trade.adjust(cash / TRADING_SLOT))
         .flatMap(Optional::stream)
         .forEach(trades::add);
    return trades;
  }
}
