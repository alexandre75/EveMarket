package lan.groland.eve.adapter;

import com.google.inject.AbstractModule;

import lan.groland.eve.domain.market.EveData;

public class EsiEveDataModule extends AbstractModule {
  private static final String TOKEN = "CTicF45_qyChdfB_Db-tP48wKqNteSHehHeJJ-T9xyVjbSZs5oM6OjwlwhIVjwhIDzHeWKrPJNeamJea3v04-A2";

  @Override
  protected void configure() {
    bind(EveData.class).toInstance(new EsiEveData(TOKEN));
  }
}
