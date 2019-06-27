package lan.groland.eve.adapter.port.persistence;

import static com.mongodb.client.model.Filters.eq;

import java.io.IOException;
import java.io.StringReader;
import java.lang.ref.Cleaner;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;

import lan.groland.eve.domain.market.Item;
import lan.groland.eve.domain.market.ItemId;
import lan.groland.eve.domain.market.ItemRepository;

/**
 * Look for items in a local DB then EVE API
 * @author alexandre
 *
 */
public class CachedItemRepository implements ItemRepository, AutoCloseable {
  private static Logger logger = Logger.getLogger("lan.groland.eve.adapter.port.persistence");

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
    logger.fine("findItem " + id + "...");
    Item item = mongoRepo.find(id);
    if (item == null) {
      item = eveRepo.find(id);
      mongoRepo.add(item);
    }
    assert item != null;
    logger.fine("...findItem");
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
  
  private final HttpClient client = HttpClient.newHttpClient();
  
  @Override
  public Item find(ItemId id) {
    URI uri = URI.create("https://esi.evetech.net/latest/universe/types/" + id.typeId());
    HttpRequest typeRequest = HttpRequest.newBuilder(uri)
        .header("Content-Type", "application/json")
        .build();
    while(true) {
      try {
      HttpResponse<String> resp = client.send(typeRequest, BodyHandlers.ofString());
      if (resp.statusCode() == 200) {
        JsonReader jsonReader = Json.createReader(new StringReader(resp.body()));
        JsonObject typeObj = jsonReader.readObject();
        return new Item(id.typeId(), typeObj.getString("name"), typeObj.getJsonNumber("volume").doubleValue());
      }
      } catch(InterruptedException e) {
        throw new IllegalStateException(e);
      } catch(IOException ignored) {
        // was working with previous code
      }
    }
  }
}
