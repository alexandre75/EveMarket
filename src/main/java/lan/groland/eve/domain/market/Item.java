package lan.groland.eve.domain.market;

public class Item {
  private ItemId itemId;
  private String name;
  private double volume;
  
  public Item(int id, String name, double volume){
    this.itemId = new ItemId(id);
    this.name = name;
    this.volume = volume;
  }
  
  public Item(ItemId id, String name, double volume){
    this.itemId = id;
    this.name = name;
    this.volume = volume;
  }
  
  public ItemId getItemId() {
    return itemId;
  }

  public String getName() {
    return name;
  }

  public double getVolume() {
    return volume;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((itemId == null) ? 0 : itemId.hashCode());
    return result;
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
    if (itemId == null) {
      if (other.itemId != null)
        return false;
    } else if (!itemId.equals(other.itemId))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "Item [itemId=" + itemId + ", name=" + name + ", volume=" + volume + "]";
  }
}
