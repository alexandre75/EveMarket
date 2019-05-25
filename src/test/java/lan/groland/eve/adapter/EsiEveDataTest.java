package lan.groland.eve.adapter;

import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.log4j.Logger;
import org.junit.jupiter.api.Test;

import lan.groland.eve.domain.market.Sales;
import lan.groland.eve.domain.market.Station.Region;

class EsiEveDataTest {
  private static Logger logger = Logger.getLogger(EsiEveDataTest.class);
  
  private EsiEveData subject = new EsiEveData("");
  
  @Test
  void test() {
    Sales sales = subject.medianPrice(31183, Region.DOMAIN, 100);
    logger.info(sales);
  }

}
