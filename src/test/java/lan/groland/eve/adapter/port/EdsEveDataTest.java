package lan.groland.eve.adapter.port;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.number.IsCloseTo.closeTo;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.net.MediaType;

import io.reactivex.Flowable;
import lan.groland.eve.domain.market.EveData;
import lan.groland.eve.domain.market.ItemId;
import lan.groland.eve.domain.market.OrderStats;
import lan.groland.eve.domain.market.Sales;
import lan.groland.eve.domain.market.Station;
import lan.groland.eve.domain.market.Station.Region;

public class EdsEveDataTest {

  @Rule
  public WireMockRule edsMock = new WireMockRule(8089);

  private EveData subject = new EdsEveData("http://localhost:8089");

  @BeforeEach
  void setup() {
    edsMock.start();
  }
  
  @AfterEach
  void tearDown() {
    edsMock.stop();
  }
  
  @Test
  public void testStationOrderStats() throws Exception {
    edsMock.stubFor(get(urlPathEqualTo("/regions/10000030/stations/60004588/book"))
                    .willReturn(aResponse().withBody(loadFile("stationBooks.json"))
                                           .withHeader("Content-Type", MediaType.JSON_UTF_8.toString())
                                           .withStatus(200)));

    Publisher<OrderStats> publisher = subject.stationOrderStatsAsync(Station.RENS_STATION);
    OrderStats expectedStats = new OrderStats(new ItemId(1319), 4, 386900D);
    List<OrderStats> result = Flowable.fromPublisher(publisher).toList().blockingGet();
    assertThat(result.size(), equalTo(6233));
    assertThat(result.get(0), equalTo(expectedStats));
  }
  
  @Test
  public void testRegionOrderStats() throws Exception {
    edsMock.stubFor(get(urlEqualTo("/regions/10000030/book"))
                    .willReturn(aResponse().withBody(loadFile("stationBooks.json"))
                                           .withHeader("Content-Type", MediaType.JSON_UTF_8.toString())
                                           .withStatus(200)));

    List<OrderStats> result = subject.regionOrderStats(Region.HEIMATAR);
    OrderStats expectedStats = new OrderStats(new ItemId(1319), 4, 386900D);

    assertThat(result.size(), equalTo(6233));
    assertThat(result.get(0), equalTo(expectedStats));
  }
  
  @Test
  public void testHistory() throws Exception {
    edsMock.stubFor(get(urlEqualTo("/regions/10000030/histories/19685"))
                    .willReturn(aResponse().withBody("{\"id\":1,\"median\":212899998.0,\"quantity\":5.0,\"item\":19685,\"region_id\":8,\"created_at\":\"2019-06-05T10:00:54.304Z\",\"updated_at\":\"2019-06-05T10:00:54.304Z\"}")
                                           .withStatus(200)));
    
    Sales sales = subject.medianPrice(new ItemId(19685), Region.HEIMATAR, 100);
    
    assertThat(sales.quantity, closeTo(5, 1E-3));
    assertThat(sales.price, closeTo(212899998.0, 1E5));
  }


  private byte[] loadFile(String path) throws IOException {
      InputStream is = ClassLoader.getSystemResourceAsStream(path);
      return is.readAllBytes();
  }
}
