package lan.groland.eve.adapter.port;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.net.MediaType;

import lan.groland.eve.domain.market.ItemId;
import lan.groland.eve.domain.market.OrderStats;
import lan.groland.eve.domain.market.Station;
import lan.groland.eve.domain.market.Station.Region;

public class EdsEveDataTest {

  @Rule
  public WireMockRule edsMock = new WireMockRule(8089);

  private EdsEveData subject = new EdsEveData("http://localhost:8089");

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
    edsMock.stubFor(get(urlPathEqualTo("/regions/10000030/stations/60004588/books"))
                    .willReturn(aResponse().withBody(loadFile("stationBooks.json"))
                                           .withHeader("Content-Type", MediaType.JSON_UTF_8.toString())
                                           .withStatus(200)));

    List<OrderStats> result = subject.stationOrderStats(Station.RENS_STATION);
    OrderStats expectedStats = new OrderStats(new ItemId(1319), 4, 386900D);

    assertThat(result.size(), equalTo(6233));
    assertThat(result.get(0), equalTo(expectedStats));
  }
  
  @Test
  public void testRegionOrderStats() throws Exception {
    edsMock.stubFor(get(urlEqualTo("/traders?region_id=10000030"))
                    .willReturn(aResponse().withBody(loadFile("stationBooks.json"))
                                           .withHeader("Content-Type", MediaType.JSON_UTF_8.toString())
                                           .withStatus(200)));

    List<OrderStats> result = subject.regionOrderStats(Region.HEIMATAR);
    OrderStats expectedStats = new OrderStats(new ItemId(1319), 4, 386900D);

    assertThat(result.size(), equalTo(6233));
    assertThat(result.get(0), equalTo(expectedStats));
  }


  private byte[] loadFile(String path) throws IOException {
      InputStream is = ClassLoader.getSystemResourceAsStream(path);
      return is.readAllBytes();
  }
}