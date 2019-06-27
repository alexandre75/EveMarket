package lan.groland.eve.adapter.port.messaging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.AbstractService;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Delivery;

import lan.groland.eve.application.CargoApplicationService;
import lan.groland.eve.domain.market.ShipmentSpecification;
import lan.groland.eve.domain.market.Trade;

public class RabbitService extends AbstractService {
  @Inject
  private static Logger logger = Logger.getLogger("eve.adapter.port.messaging");

  private static final String QUEUE_NAME = "cargo.load";
  private final Connection connection;
  private Channel channel;
  private final CargoApplicationService shipmtService;
  
  @Inject
  RabbitService(Connection connection, CargoApplicationService shipmtService) {
    this.connection = connection;
    this.shipmtService = shipmtService;
  }

  private void messageHandler(String consumerTag, Delivery delivery) {
    logger.info("cargo.load " + delivery.getProperties().getMessageId() + ", reply-to :"
            + delivery.getProperties().getReplyTo());
    logger.fine(() -> new String(delivery.getBody()));
    try {
      Gson gson = new Gson();
      try {
        ShipmentSpecification spec = gson.fromJson(new String(delivery.getBody(), StandardCharsets.UTF_8),
                                                   ShipmentSpecification.class);
        Collection<Trade> trades = shipmtService.optimizeCargo(spec);

        byte[] tradeBytes = serialize(trades);

        BasicProperties props = getProperties(delivery);
        logger.info(delivery.getProperties().getReplyTo() + " replying...");
        logger.fine(() -> new String(tradeBytes));
        channel.queueDeclare(delivery.getProperties().getReplyTo(), true, false, false, null);
        channel.basicPublish("", delivery.getProperties().getReplyTo(), props, tradeBytes);

        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
      } catch(JsonSyntaxException | IOException | IllegalArgumentException e) {
        // TODO move to invalid queue?
        logger.log(Level.SEVERE, delivery.getProperties().getMessageId(), e);
        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
      } catch(Exception e) {
        channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
      }
    } catch(IOException e) {
      logger.log(Level.SEVERE, "Could not process : ", e);
    } catch(Exception e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
      throw e;
    }
  }

  private static BasicProperties getProperties(Delivery delivery) {
    return new BasicProperties.Builder()
            .correlationId(delivery.getProperties().getMessageId())
            .deliveryMode(2)
            .priority(0)
            .contentType("application/json")
            .build();
  }

  private byte[] serialize(Collection<Trade> trades) {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    TradesTranslator translator = new TradesTranslator(output);
    translator.write(trades);
    return output.toByteArray();
  }

  @Override
  protected void doStart() {
    try {
      try {
        channel = connection.createChannel();
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);

        //channel.basicQos(1);

        channel.basicConsume(QUEUE_NAME, false, this::messageHandler, consummerTag -> {
        });
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      notifyStarted();
    } catch(Throwable e) {
      notifyFailed(e);
    }
  }

  @Override
  protected void doStop() {
    try {
      channel.close();
      connection.close();
    } catch(TimeoutException e) {
      throw new IllegalStateException(e);
    } catch(IOException e) {
      throw new UncheckedIOException(e);
    }
    notifyStopped();
  }
}
