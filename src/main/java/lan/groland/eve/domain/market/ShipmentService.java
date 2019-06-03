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

  public Collection<Trade> optimizeCargo(ShipmentSpecification shipSpec) {
    List<Item> items =  eveData.stationOrderStats(Station.JITA)
                               .stream().filter(os -> os.getBid() < shipSpec.cashAvailable() / 10)
                               .map(os -> itemRepository.find(os.getItem()))
                               .filter(shipSpec::isSatisfiedBy)
                               .collect(Collectors.toList());
    return load(items, shipSpec);
  }
  
  public Collection<Trade> load(Collection<Item> items, ShipmentSpecification shipSpec) {
    Cargo trades = new Cargo(shipSpec); 
    items.parallelStream()
         .map(item -> tradeFactory.createOptional(item, shipSpec))
         .flatMap(Optional::stream)
         .map(trade -> trade.adjust(shipSpec.cashAvailable() / TRADING_SLOT))
         .flatMap(Optional::stream)
         .forEach(trades::add);
    return trades;
  }
}
