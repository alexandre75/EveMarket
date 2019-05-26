package lan.groland.eve.adapter.port;

import org.threeten.bp.OffsetDateTime;

import io.swagger.client.model.GetMarketsRegionIdOrders200Ok;
import io.swagger.client.model.GetMarketsStructuresStructureId200Ok;

/**
 * Provide an interface for order.
 * @author alexandre
 *
 */
public interface Order {

  long getLocationId();

  int getTypeId();

  boolean isBuyOrder();

  double getPrice();

  OffsetDateTime getIssued();

  static Order from(GetMarketsStructuresStructureId200Ok order) {
    return new StructureOrder(order);
  }

  static Order fromRegionOrders(GetMarketsRegionIdOrders200Ok order) {
    return new RegionOrder(order);
  }

}

class StructureOrder implements Order {
  private GetMarketsStructuresStructureId200Ok order;
  
  public StructureOrder(GetMarketsStructuresStructureId200Ok order) {
    this.order = order;
  }
  
  @Override
  public long getLocationId() {
    return order.getLocationId();
  }

  @Override
  public int getTypeId() {
    return order.getTypeId();
  }

  @Override
  public boolean isBuyOrder() {
    return order.isIsBuyOrder();
  }

  @Override
  public double getPrice() {
    return order.getPrice();
  }

  @Override
  public OffsetDateTime getIssued() {
    return order.getIssued();
  }
}

class RegionOrder implements Order {
  private GetMarketsRegionIdOrders200Ok order;
  
  public RegionOrder(GetMarketsRegionIdOrders200Ok order) {
    this.order = order;
  }
  
  @Override
  public long getLocationId() {
    return order.getLocationId();
  }

  @Override
  public int getTypeId() {
    return order.getTypeId();
  }

  @Override
  public boolean isBuyOrder() {
    return order.isIsBuyOrder();
  }

  @Override
  public double getPrice() {
    return order.getPrice();
  }

  @Override
  public OffsetDateTime getIssued() {
    return order.getIssued();
  }
}