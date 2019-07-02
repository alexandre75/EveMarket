package lan.groland.eve.adapter.port.ws;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import lan.groland.eve.domain.market.OrderStats;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

class JsonStatOrderParserTest {

  private JsonStatOrderParser subject;
  
  @BeforeEach
  public void setup() {
    subject = new JsonStatOrderParser(ClassLoader.getSystemResourceAsStream("stationBooks.json"));
  }
  
  @Test
  public void shouldParse6233orders() {
    List<OrderStats> orderStatsList = subject.parse();
    
    assertThat(orderStatsList.size(), equalTo(6233));
  }
}
