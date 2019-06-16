package lan.groland.eve.adapter.port;

import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;

import com.google.inject.AbstractModule;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;

import lan.groland.eve.domain.market.EveData;
import lan.groland.eve.domain.market.ItemRepository;

public class EsiEveDataModule extends AbstractModule {
  
  @Override
  protected void configure() {
    bind(EveData.class).toInstance(new EdsEveData("http://saturne:3000"));
    bind(ItemRepository.class).to(CachedItemRepository.class);

    CodecRegistry reg = CodecRegistries.fromCodecs(new ItemCodec());
    CodecRegistry pojoCodecRegistry = fromRegistries(reg, MongoClient.getDefaultCodecRegistry());
//    CodecRegistry pojoCodecRegistry = fromRegistries(reg, MongoClient.getDefaultCodecRegistry(),
//                                                     fromProviders(PojoCodecProvider.builder().automatic(false).build()));

    bind(MongoClient.class).toInstance(new MongoClient("localhost", MongoClientOptions.builder().codecRegistry(pojoCodecRegistry).build()));
  }
}
