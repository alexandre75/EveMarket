package lan.groland.eve.application;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.inject.Inject;
import lan.groland.eve.domain.market.*;

public class CargoApplicationService {
  private static Logger logger = Logger.getLogger("lan.groland.eve.application");

  private ShipmentService service;
  private EveData eveData;

  @Inject
  CargoApplicationService(ShipmentService service, EveData eveData) {
    this.service = service;
    this.eveData = eveData;
  }

  /**
   * Load a cargo with a selection of the given items.
   * Items are selected so that :
   * <ul>
   * <li>They satisfy the given specifications;
   * <li>Benefits are optimized
   * <ul>
   * @param spec specification for the operation.
   * @return an optimized cargo
   * @see ShipmentSpecification
   */
  public Collection<Trade> optimizeCargo(ShipmentSpecification spec) {

    try {
      return service.optimizeCargo(spec, TradeFactory.create(eveData));
    } catch(Throwable e) {
      logger.log(Level.SEVERE, spec.toString(), e);
      throw e;
    }
  }

}
