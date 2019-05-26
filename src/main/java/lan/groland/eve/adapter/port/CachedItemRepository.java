package lan.groland.eve.adapter.port;

import java.lang.ref.Cleaner;

import org.bson.Document;

import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import io.swagger.client.ApiException;
import io.swagger.client.api.UniverseApi;
import io.swagger.client.model.GetUniverseTypesTypeIdOk;
import lan.groland.eve.domain.market.Item;
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
  public Item find(int id) {
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
  
  private final MongoCollection<Document> itemDescritions;
  private final MongoClient mongoClient;

  public MongoItemRepository() {
    mongoClient = new MongoClient(new MongoClientURI("mongodb://jupiter:27017"));
    MongoDatabase eve = mongoClient.getDatabase("Eve");
    itemDescritions = eve.getCollection("Items");
    
    cleanable = CLEANER.register(this, mongoClient::close);
  }

  public void close() {
    cleanable.clean();
  }

  @Override
  public Item find(int id) {
    BasicDBObject query = new BasicDBObject("id", id);
    return Item.from(itemDescritions.find(query).first());
  }
  
  public void add(Item item) {
    Document res = item.document();
    itemDescritions.insertOne(res);
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
  public Item find(int id) {
    GetUniverseTypesTypeIdOk info;
    while(true){
      try {
        info = univers.getUniverseTypesTypeId(id, null, null, null, null, null);
        return new Item(id, info.getName(), info.getVolume());
      } catch(ApiException e){
      }
    }
  }
}
