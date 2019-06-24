package lan.groland.eve.adapter.port.messaging;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.AbstractService;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Delivery;

import lan.groland.eve.application.CargoApplicationService;
import lan.groland.eve.domain.market.ShipmentSpecification;

public class RabbitService extends AbstractService {
  @Inject
  private static Logger logger;

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
        TradesTranslator translator = new TradesTranslator(shipmtService.optimizeCargo(spec));

        channel.queueDeclare(delivery.getProperties().getReplyTo(), true, false, false, null);
        BasicProperties props = new BasicProperties.Builder()
            .correlationId(delivery.getProperties().getMessageId())
            .deliveryMode(2)
            .priority(0)
            .contentType("application/json")
            .build();
        logger.info(delivery.getProperties().getReplyTo() + " replying...");
        logger.fine(() -> new String(translator.toBytes()));
        channel.basicPublish("", delivery.getProperties().getReplyTo(), props, translator.toBytes());
      } catch(JsonSyntaxException e) {
        logger.log(Level.SEVERE, "Could not process : ", e);
      }
      channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
    } catch(IOException e) {
      logger.log(Level.WARNING, "Could not process : ", e);
    }
  }

  @Override
  protected void doStart() {
    try {
      try {
        channel = connection.createChannel();
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);

        //channel.basicQos(1);

        channel.basicConsume(QUEUE_NAME, true, this::messageHandler, consummerTag -> {
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
