package lan.groland.eve.domain.market;

import javax.annotation.concurrent.Immutable;

@Immutable
public final class ItemId {
  private int typeId;
  
  public ItemId(int typeId) {
    this.typeId = typeId;
  }

  public int typeId() {
    return typeId;
  }

  @Override
  public String toString() {
    return "ItemId [typeId=" + typeId + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + typeId;
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
    ItemId other = (ItemId) obj;
    if (typeId != other.typeId)
      return false;
    return true;
  }
}
