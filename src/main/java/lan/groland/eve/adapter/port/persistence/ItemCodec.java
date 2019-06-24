package lan.groland.eve.adapter.port.persistence;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import lan.groland.eve.domain.market.Item;

public class ItemCodec implements Codec<Item> {

  @Override
  public void encode(BsonWriter writer, Item value, EncoderContext encoderContext) {
    writer.writeStartDocument();
    writer.writeInt32("type_id", value.getItemId().typeId());
    writer.writeString("name", value.getName());
    writer.writeDouble("volume", value.getVolume());
    writer.writeEndDocument();
  }

  @Override
  public Class<Item> getEncoderClass() {
    return Item.class;
  }

  @Override
  public Item decode(BsonReader reader, DecoderContext decoderContext) {
    reader.readStartDocument();
    reader.readObjectId();
    Item res = new Item(reader.readInt32("type_id"),
                        reader.readString("name"),
                        reader.readDouble("volume"));
    reader.readEndDocument();
    return res;
  }

}
