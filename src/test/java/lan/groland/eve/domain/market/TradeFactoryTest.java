package lan.groland.eve.domain.market;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import static org.mockito.Mockito.*;

import java.util.Collections;

import org.mockito.MockitoAnnotations;

import com.google.common.collect.ImmutableList;

class TradeFactoryTest {
  
  @Mock private EveData eveData;

  private TradeFactory subject;
  ItemId item = new ItemId(5);
  
  @BeforeEach
  void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    when(eveData.stationOrderStats(Station.JITA)).thenReturn(ImmutableList.of(new OrderStats(item, 0, 100D)));
    subject = new TradeFactory(eveData);
  }

  @Test
  void shouldAcceptTrade() throws OrderBookEmptyException {
    when(eveData.regionOrderStats(any())).thenReturn(ImmutableList.of(new OrderStats(item, 1, 120D)));
    when(eveData.medianPrice(eq(item), any(), eq(100D))).thenReturn(new Sales(100, 150D));
    
    Trade trade = subject.create(new Item(item, "", 10), Station.AMARR_STATION);
    
    assertEquals(772D, trade.benefit(), .001); // TODO verify
    assertEquals(.5D, trade.expectedMargin(), 0.001); // TODO verify
    assertEquals(1000, trade.volume(), 0.001);
    assertEquals(100, trade.unitSoldDay(), 0.001);
    assertEquals(100 * 100, trade.capital(), 0.001); // XXX
    assertEquals(772D, trade.getBenefParJour(), 0.001);
    assertEquals(.462, trade.lastMonthMargin(), 0.001);
  }

  @Test
  void shouldThrowException() {
    ItemId item = new ItemId(5);
    when(eveData.regionOrderStats(any())).thenReturn(Collections.emptyList());
    when(eveData.medianPrice(eq(item), any(), eq(100D))).thenReturn(new Sales(100, 150D));
    
    OrderBookEmptyException e = assertThrows(OrderBookEmptyException.class, 
                                 () -> subject.create(new Item(item, "", 10), Station.AMARR_STATION));
    
    assertEquals(5, e.getItemId());
    assertEquals(Station.AMARR_STATION, e.getStation());
  }
}
