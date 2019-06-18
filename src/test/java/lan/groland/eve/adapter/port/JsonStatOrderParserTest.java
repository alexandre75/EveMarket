package lan.groland.eve.adapter.port;

import java.util.concurrent.Flow.Publisher;

import org.junit.jupiter.api.BeforeEach;
import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowPublisherVerification;

import lan.groland.eve.domain.market.OrderStats;

class JsonStatOrderParserTest extends FlowPublisherVerification<OrderStats> {

  public JsonStatOrderParserTest() {
    super(new TestEnvironment());
  }

  private JsonStatOrderParser subject;
  
  @BeforeEach
  public void setup() {
    subject = new JsonStatOrderParser(ClassLoader.getSystemResourceAsStream("stationBooks.json"));
  }
  
  @Override
  public Publisher<OrderStats> createFlowPublisher(long elements) {
    return subject;
  }

  @Override
  public Publisher<OrderStats> createFailedFlowPublisher() {
    return subject;
  }
  
  @Override public long maxElementsFromPublisher() {
    return 6233;
  }
}
