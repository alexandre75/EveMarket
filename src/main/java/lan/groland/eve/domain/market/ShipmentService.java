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

  public BestTrades optimizeCargo(Station station, double cash, int cargo, ShipmentSpecification shipSpec) {
    BestTrades trades = new BestTrades(TRADING_SLOT, cargo); 
      List<Item> items =  eveData.cheaperThan(cash / 10, Station.JITA)
                                 .stream()
                                 .map(itemRepository::find)
                                 .filter(shipSpec::isSatisfiedBy)
                                 .collect(Collectors.toList());
    return optimizeCargo(items, trades, station, cash, shipSpec);
  }
  
  public BestTrades optimizeCargo(Collection<Item> items, BestTrades trades, Station station, double cash, ShipmentSpecification shipSpec) {
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
