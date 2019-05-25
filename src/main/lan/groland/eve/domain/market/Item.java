package lan.groland.eve.domain.market;

public class Item {
  private int id;
  private String name;
  private double volume;

  public int getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public double getVolume() {
    return volume;
  }

  public Item(int id, String name, double volume){
    this.id = id;
    this.name = name;
    this.volume = volume;
  }

  @Override
  public int hashCode() {
    return Integer.hashCode(id);
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

}
