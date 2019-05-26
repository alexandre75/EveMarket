package lan.groland.eve.domain.market;

import static lan.groland.eve.domain.market.Station.Region.*;

public enum Station {
  JITA(new long[]{60003760L}, FORGE),
  D_P(new long[]{1024004680659L}, ESOTERIA),
  DODIXIE_STATION(new long[] {60011866}, SINQ_SAISON),
  AMARR_STATION(new long[] {60008494}, DOMAIN),
  HEK_STATION(new long[] {60005686}, METROPOLIS),
  RENS_STATION(new long[] {60004588}, HEIMATAR),
  BRAVELAND_STATION(new long[] {61000182, 1023729674815l}, CATCH),
  TAMA(new long[] {60005203}, THE_CITADEL);
  
  public enum Region {
    ESOTERIA(10000039, true),
    FORGE (10000002),
    SINQ_SAISON(10000032),
    DOMAIN(10000043),
    METROPOLIS(10000042),
    HEIMATAR(10000030),
    CATCH(10000014, true),
    CURSE(10000012, true),
    THE_CITADEL(10000033);
    
    
    private final int regionId;
    private final boolean nullSec;

    private Region(int regionId) {
      this.regionId = regionId;
      nullSec = false;
    }

    private Region(int regionId, boolean nullSec) {
      this.regionId = regionId;
      this.nullSec = nullSec;
    }
    
    public int getRegionId() {
      return regionId;
    }
    
    public boolean isNullSec() {
      return nullSec;
    }
  }
  
  private final long[] stationIds;
  private final Region region;

  private Station(long[] stationIds, Region region) {
    this.stationIds = stationIds;
    this.region = region;
  }

  public long[] getStationIds() {
    return stationIds.clone();
  }

  public int getRegionId() {
    return region.regionId;
  }

  public Region getRegion() {
    return region;
  }
}
