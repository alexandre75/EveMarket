package lan.groland.eve.adapter.port.persistence;

import static org.hamcrest.MatcherAssert.assertThat; 

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import lan.groland.eve.adapter.port.persistence.CachedItemRepository;
import lan.groland.eve.adapter.port.persistence.EveItemRepository;
import lan.groland.eve.adapter.port.persistence.MongoItemRepository;
import lan.groland.eve.domain.market.Item;
import lan.groland.eve.domain.market.ItemId;

import static org.hamcrest.Matchers.*;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


class CachedItemRepositoryTest {

  @Mock private MongoItemRepository mongoRepo;
  @Mock private EveItemRepository eveRepo;
  
  private CachedItemRepository subject;
  
  @BeforeEach
  public void setup() {
    MockitoAnnotations.initMocks(this);
    subject = new CachedItemRepository(mongoRepo, eveRepo);
  }
  
  @Test
  void shouldReturnFromCache() {
    Item pistol = new Item(5, "pistol", 6D);
    when(mongoRepo.find(new ItemId(5))).thenReturn(pistol);
    Item mayBePistol = subject.find(new ItemId(5));
    
    assertThat(mayBePistol, equalTo(pistol));
    verify(eveRepo, never()).find(any());
  }

  @Test
  void shouldGoThroughCache() {
    Item pistol = new Item(5, "pistol", 6D);
    when(eveRepo.find(new ItemId(5))).thenReturn(pistol);
    
    Item mayBePistol = subject.find(new ItemId(5));
    
    assertThat(mayBePistol, equalTo(pistol));
    verify(eveRepo, times(1)).find(new ItemId(5));
    verify(mongoRepo, times(1)).add(pistol);
  }
}
