package lan.groland.eve.domain.market;

import java.util.List;

import org.apache.log4j.Logger;

import com.google.inject.Inject;

public class ShipmentService {
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

  public BestTrades optimizeCargo(Station station, double cash, int cargo, ShipmentSpecification shipSpec) throws InterruptedException {
    BestTrades trades = new BestTrades(TRADING_SLOT, cargo); 
    List<Integer> items =  eveData.cheaperThan(cash / 10, Station.JITA);
    for (int id : items) {   // Move the cursor to the next row
      try {
        Item item = itemRepository.find(id);            

        if (shipSpec.isSatisfiedBy(item)) {
          Trade trade = tradeFactory.create(item, station);

          if (shipSpec.isSatisfiedByTrade(trade)) {
            trade.ajust(cash / TRADING_SLOT).ifPresent(trades::add);
          }
        }
      } catch(OrderBookEmptyException e) {
        log.info(e.getMessage());
      }
    }
    return trades;
  }
}
