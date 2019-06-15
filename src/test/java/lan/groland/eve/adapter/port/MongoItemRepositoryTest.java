package lan.groland.eve.adapter.port;

import static org.junit.Assert.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.hamcrest.Matchers.*;
import com.google.inject.Guice;
import com.google.inject.Inject;

import lan.groland.eve.application.TestModule;
import lan.groland.eve.domain.market.Item;

public class MongoItemRepositoryTest {

  @Inject
  private MongoItemRepository subject;
  
  @BeforeEach
  public void setup() {
    Guice.createInjector(new EsiEveDataModule(), new TestModule()).injectMembers(this);
    subject.clear();
  }
  
  @Test
  public void shouldStoreAndFetch() {
    Item trit = new Item(34, "Tritanium", 0.01);
    subject.add(trit);
    
    Item mayBeTrit = subject.find(trit.getItemId());
    assertThat(mayBeTrit, equalTo(trit));
    assertThat(mayBeTrit.getName(), equalTo(trit.getName()));
    assertThat(mayBeTrit.getVolume(), equalTo(trit.getVolume()));
  }
}
