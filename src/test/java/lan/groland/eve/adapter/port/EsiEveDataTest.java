package lan.groland.eve.adapter.port;

import org.junit.jupiter.api.Test;
import org.apache.log4j.Logger;
import lan.groland.eve.adapter.port.EsiEveData;
import lan.groland.eve.domain.market.ItemId;
import lan.groland.eve.domain.market.Sales;
import lan.groland.eve.domain.market.Station.Region;

class EsiEveDataTest {
  private static Logger logger = Logger.getLogger(EsiEveDataTest.class);
  
  private EsiEveData subject = new EsiEveData("");
  
  @Test
  void test() {
    Sales sales = subject.medianPrice(new ItemId(31183), Region.DOMAIN, 100);
    logger.info(sales);
  }

}
