package lan.groland.eve.domain.market;

public interface ItemRepository {

  /**
   * Return the item associated with the given id or null
   * @param id
   */ 
  Item find(ItemId id);
}
