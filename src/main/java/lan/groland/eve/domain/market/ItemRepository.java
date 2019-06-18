package lan.groland.eve.domain.market;

import io.reactivex.Flowable;

public interface ItemRepository {

  /**
   * Return the item associated with the given id or null
   * @param id
   */ 
  Item find(ItemId id);

  default Flowable<Item> findAsync(ItemId item) {
    return Flowable.fromCallable(() -> find(item));
  }
}
