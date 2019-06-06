package lan.groland.eve.adapter.port;

import com.google.inject.AbstractModule;

import lan.groland.eve.domain.market.EveData;
import lan.groland.eve.domain.market.ItemRepository;

public class EsiEveDataModule extends AbstractModule {
  
  @Override
  protected void configure() {
    bind(EveData.class).toInstance(new EdsEveData("http://localhost:3000"));
    bind(ItemRepository.class).to(CachedItemRepository.class);
  }
}
