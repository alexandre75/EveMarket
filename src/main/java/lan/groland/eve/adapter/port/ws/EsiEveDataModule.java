package lan.groland.eve.adapter.port.ws;

import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

import com.google.inject.Provides;
import com.google.inject.name.Named;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;

import com.google.inject.AbstractModule;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;

import lan.groland.eve.adapter.port.persistence.CachedItemRepository;
import lan.groland.eve.adapter.port.persistence.ItemCodec;
import lan.groland.eve.domain.market.EveData;
import lan.groland.eve.domain.market.ItemRepository;

public class EsiEveDataModule extends AbstractModule {

  private MongoClient mongoClient;

  @Override
  protected void configure() {
    bind(EveData.class).to(EdsEveData.class);
    bind(ItemRepository.class).to(CachedItemRepository.class);
  }

  @Provides
  public synchronized MongoClient provideMongoClient(@Named("mongo.host") String host){
    if (mongoClient == null) {
      CodecRegistry reg = CodecRegistries.fromCodecs(new ItemCodec());
      CodecRegistry pojoCodecRegistry = fromRegistries(reg, MongoClient.getDefaultCodecRegistry());

      mongoClient = new MongoClient(host, MongoClientOptions.builder().codecRegistry(pojoCodecRegistry).build());
    }
    return mongoClient;
  }
}
