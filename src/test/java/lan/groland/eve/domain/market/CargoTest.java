package lan.groland.eve.domain.market;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import static org.hamcrest.number.IsCloseTo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CargoTest {

  private Cargo subject;
  private Trade newTrade;
  
  @BeforeEach
  void setUp() throws Exception {
    ShipmentSpecification spec = new ShipmentSpecification.Builder(Station.AMARR_STATION, 1000)
                                                          .maxVolume(2000)
                                                          .build();
    subject = new Cargo(spec);
    
    newTrade = new DefaultTrade() {
      @Override
      public int volume() {
        return 500;
      }
      
      @Override
      public double capital() {
        return 400;
      }
    };
  }

  @Test
  void shouldTrackDetailsOnAdd() {
    subject.add(newTrade);
    
    assertThat(subject.requiredCapital(), closeTo(400, .01));
    assertThat(subject.getVolume(), equalTo(500));
  }

  @Test
  void shouldTrackDetailsOnOverflow() {

    
    subject.add(newTrade);
    subject.add(newTrade);
    subject.add(newTrade);
    
    assertThat(subject.requiredCapital(), closeTo(800, .01));
    assertThat(subject.getVolume(), equalTo(1000));
  }
}
