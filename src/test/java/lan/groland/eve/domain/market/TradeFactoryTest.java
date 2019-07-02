package lan.groland.eve.domain.market;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import static org.mockito.Mockito.*;

import java.util.Collections;

import org.mockito.MockitoAnnotations;

import com.google.common.collect.ImmutableList;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.subscribers.TestSubscriber;

class TradeFactoryTest {
  
  @Mock private EveData eveData;

  private TradeFactory subject;
  ItemId item = new ItemId(5);
  
  @BeforeEach
  void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    when(eveData.stationOrderStatsAsync(Station.JITA)).thenReturn(Flowable.just(new OrderStats(item, 0, 100D)));
    subject = TradeFactory.create(eveData);
  }

  @Test
  void shouldAcceptTrade() throws OrderBookEmptyException {
    /*
     * 1 other trader at 120
     * bought at 100
     * 100 per day
     * historically sold at 150
     */
    when(eveData.regionOrderStats(any())).thenReturn(ImmutableList.of(new OrderStats(item, 1, 120D)));
    when(eveData.medianPriceAsync(eq(item), any(), eq(100D))).thenReturn(Single.just(new Sales(100, 150D)));
    
    Trade trade = subject.trade(new Item(item, "", 10), Station.AMARR_STATION, 1F-.962F).blockingGet();
    
    assertEquals(.199D, trade.expectedMargin(), 0.001);
    assertEquals(1000, trade.volume(), 0.001);
    assertEquals(100, trade.unitSoldDay(), 0.001);
    assertEquals(100 * 100, trade.capital(), 0.001);
    assertEquals(772D, trade.dailyBenefit(), 0.001); // low due to sale taxes
    assertEquals(.443, trade.lastMonthMargin(), 0.001);
  }

  @Test
  void shouldThrowException() {
    ItemId item = new ItemId(5);
    when(eveData.regionOrderStats(any())).thenReturn(Collections.emptyList());
    when(eveData.medianPriceAsync(eq(item), any(), eq(100D))).thenReturn(Single.just(new Sales(100, 150D)));
    
    TestSubscriber<Trade> subscriber = new TestSubscriber<>();
    subject.trade(new Item(item, "", 10), Station.AMARR_STATION, 1F-.962F)
           .toFlowable().subscribe(subscriber);
    subscriber.assertError(OrderBookEmptyException.class);
    subscriber.assertError(e -> ((OrderBookEmptyException) e).getItemId() == 5);
    subscriber.assertError(e -> ((OrderBookEmptyException) e).getStation() == Station.AMARR_STATION);
  }
}
