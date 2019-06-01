package lan.groland.eve.domain.market;

import java.util.Date;

import org.bson.Document;

public class Item {
  private ItemId id;
  private String name;
  private double volume;

  public Item(int id, String name, double volume){
    this.id = new ItemId(id);
    this.name = name;
    this.volume = volume;
  }
  
  public Item(ItemId id, String name, double volume){
    this.id = id;
    this.name = name;
    this.volume = volume;
  }
  
  public ItemId getItemId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public double getVolume() {
    return volume;
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Item other = (Item) obj;
    if (id != other.id)
      return false;
    return true;
  }

  public static Item from(Document doc) {
    return new Item(doc.getInteger("id", -1), doc.getString("name"), doc.getDouble("volume"));
  }

  public Document document() {
    return new Document("id", id).append("volume", getVolume())
                                 .append("name", getName())
                                 .append("timestamp", new Date());
  }
}
