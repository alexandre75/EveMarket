package lan.groland.eve.application;

import java.util.Collection;

import com.google.inject.Inject;
import lan.groland.eve.domain.market.ShipmentService;
import lan.groland.eve.domain.market.ShipmentSpecification;
import lan.groland.eve.domain.market.Trade;

public class CargoApplicationService {

  private ShipmentService service;

  @Inject
  CargoApplicationService(ShipmentService service) {
    this.service = service;
  }

  /**
   * Load a cargo with a selection of the given items.
   * Items are selected so that :
   * <ul>
   * <li>They satisfy the given specifications;
   * <li>Benefits are optimized
   * <ul>
   * @param items The items available to trade.
   * @param shipSpec specification for the operation.
   * @return an optimized cargo
   * @see ShipmentSpecification
   */
  public Collection<Trade> optimizeCargo(ShipmentSpecification spec) {
    return service.optimizeCargo(spec);
  }

}
