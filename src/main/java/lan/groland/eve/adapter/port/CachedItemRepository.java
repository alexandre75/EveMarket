package lan.groland.eve.adapter.port;

import static com.mongodb.client.model.Filters.eq;

import java.lang.ref.Cleaner;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;

import io.swagger.client.ApiException;
import io.swagger.client.api.UniverseApi;
import io.swagger.client.model.GetUniverseTypesTypeIdOk;
import lan.groland.eve.domain.market.Item;
import lan.groland.eve.domain.market.ItemId;
import lan.groland.eve.domain.market.ItemRepository;

/**
 * Look for items in a local DB then EVE API
 * @author alexandre
 *
 */
public class CachedItemRepository implements ItemRepository, AutoCloseable {
  
  private final MongoItemRepository mongoRepo;
  private final EveItemRepository eveRepo;
  
  @Inject
  CachedItemRepository(MongoItemRepository mongoRepo, EveItemRepository eveRepo) {
    super();
    this.mongoRepo = mongoRepo;
    this.eveRepo = eveRepo;
  }

  @Override
  public Item find(ItemId id) {
    Item item = mongoRepo.find(id);
    if (item == null) {
      item = eveRepo.find(id);
      mongoRepo.add(item);
    }
    assert item != null;
    return item;
  }

  @Override
  public void close() throws Exception {
    mongoRepo.close();
  }
}

/**
 * Retrieve items description from DB
 * @author alexandre
 *
 */
class MongoItemRepository implements ItemRepository, AutoCloseable {
  private static final Cleaner CLEANER = Cleaner.create();
  private final Cleaner.Cleanable cleanable;
  
  private final MongoCollection<Item> itemDescritions;

  @Inject
  public MongoItemRepository(MongoClient mongoClient, @Named("mongo.schema") String schema) {
    MongoDatabase eve = mongoClient.getDatabase(schema);
    itemDescritions = eve.getCollection("Items", Item.class);
    itemDescritions.createIndex(Indexes.hashed("type_id"));
    cleanable = CLEANER.register(this, mongoClient::close);
  }

  public void close() {
    cleanable.clean();
  }

  @Override
  public Item find(ItemId id) {
    return itemDescritions.find(eq("type_id", id.typeId())).first();
  }
  
  public void add(Item item) {
    itemDescritions.insertOne(item);
  }

  /**
   * Clears the items for testing purpose
   */
  void clear() {
    itemDescritions.deleteMany(new BasicDBObject());
  }
}

/**
 * Retrieve item description from EVE API.
 * @author alexandre
 *
 */
class EveItemRepository implements ItemRepository {
  private UniverseApi univers = new UniverseApi();
  
  @Override
  public Item find(ItemId id) {
    GetUniverseTypesTypeIdOk info;
    while(true){
      try {
        info = univers.getUniverseTypesTypeId(id.typeId(), null, null, null, null, null);
        return new Item(id.typeId(), info.getName(), info.getVolume());
      } catch(ApiException e){
      }
    }
  }
}
