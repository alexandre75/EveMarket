package lan.groland.eve.domain.market;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.reactivex.Flowable;
import io.reactivex.Single;

import static org.mockito.Mockito.*;

class ShipmentServiceTest {
  
  @Mock private EveData eveData;
  @Mock private TradeFactory tradeFactory;
  @Mock private ShipmentSpecification specs;
  @Mock private Trade mockTrade1;
  @Mock private ItemRepository itemRepo;
  
  private ShipmentService subject;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.initMocks(this);
    subject = new ShipmentService(eveData, itemRepo, tradeFactory);
  }
  
  @Test
  void shouldPassAllSpec() {
    List<Item> items = items();
    when(specs.isSatisfiedBy(any())).thenReturn(true);
    when(specs.isSatisfiedByCargo(any())).thenReturn(true);
    when(specs.isSatisfiedByTrade(any())).thenReturn(true);
    when(tradeFactory.trade(any(), any())).thenReturn(Single.just(mockTrade1));
    when(mockTrade1.adjust(anyDouble())).thenReturn(Optional.of(mockTrade1));
    
    Collection<Trade> trades = subject.load(Flowable.fromIterable(items), specs);
    
    assertEquals(1000, trades.size());
  }
  
  @Test
  void shouldBeFilteredByItem() {
    List<Item> items = items();
    when(specs.isSatisfiedBy(any())).thenReturn(false);
    when(specs.isSatisfiedBy(argThat(item -> item.getItemId().typeId() < 30))).thenReturn(true);
    when(specs.isSatisfiedByCargo(any())).thenReturn(true);
    when(specs.isSatisfiedByTrade(any())).thenReturn(true);
    when(tradeFactory.trade(any(), any())).thenReturn(Single.just(mockTrade1));
    when(mockTrade1.adjust(anyDouble())).thenReturn(Optional.of(mockTrade1));
    
    Collection<Trade> trades = subject.load(Flowable.fromIterable(items), specs);
    
    assertEquals(30, trades.size());
  }
  
  @Test
  void shouldBeFilteredByAdjust() {
    List<Item> items = items();
    when(specs.isSatisfiedBy(any())).thenReturn(true);
    when(specs.isSatisfiedByCargo(any())).thenReturn(true);
    
    when(tradeFactory.trade(any(), any())).thenReturn(Single.just(mockTrade1));
    when(mockTrade1.adjust(anyDouble())).thenReturn(Optional.empty());
    
    Collection<Trade> trades = subject.load(Flowable.fromIterable(items), specs);
    
    assertEquals(0, trades.size());
  }
  
  @Test
  void shouldBeFilteredByTrade() {
    List<Item> items = items();
    when(specs.isSatisfiedBy(any())).thenReturn(true);
    when(specs.isSatisfiedByCargo(any())).thenReturn(true);
    
    when(tradeFactory.trade(any(), any()))
                     .thenReturn(Single.error(new OrderBookEmptyException(5, Station.AMARR_STATION)));
    when(mockTrade1.adjust(anyDouble())).thenReturn(Optional.of(mockTrade1));
    
    Collection<Trade> trades = subject.load(Flowable.fromIterable(items), specs);
    
    assertEquals(0, trades.size());
  }
  
  @Test
  void shouldPassOptimize() {
    Iterator<Item> iter = items().iterator();
    when(eveData.stationOrderStatsAsync(any())).thenReturn(Flowable.just(new OrderStats(new ItemId(3), 4, 9D)));
    when(specs.cashAvailable()).thenReturn(1000D);
    when(itemRepo.findAsync(any())).thenReturn(Flowable.just(iter.next()));
    when(specs.isSatisfiedBy(any())).thenReturn(true);
    when(specs.isSatisfiedByCargo(any())).thenReturn(true);
    when(specs.isSatisfiedByTrade(any())).thenReturn(true);
    
    when(tradeFactory.trade(any(), any())).thenReturn(Single.just(mockTrade1));
    when(mockTrade1.adjust(anyDouble())).thenReturn(Optional.of(mockTrade1));
    
    Collection<Trade> trades = subject.optimizeCargo(specs);
    
    assertEquals(1, trades.size());
  }

  private List<Item> items() {
    List<Item> result = new ArrayList<>();
    for (int i = 0 ; i < 1000 ; i++) {
      Item item = new Item(i, "Item " + i, 3000 - i * 2);
      result.add(item);
    }
    return result;
  }
  
}
