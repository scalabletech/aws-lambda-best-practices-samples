package sample;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.PutRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

public class Handler implements RequestHandler<SQSEvent, Void> {

  private static final Logger logger = LogManager.getLogger(Handler.class);
  private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  private static final Region region = Region.US_EAST_1;
  private DynamoDbClient ddbClient = DynamoDbClient.builder().region(region).build();

  private static final String TABLE_NAME =
      System.getenv("TABLE_NAME") == null ? "temperature_device_reading"
          : System.getenv("TABLE_NAME");
  private static final String DEVICE_MONTH_FIELD_NAME = "device_month";
  private static final String DAY_TIME_FIELD_NAME = "day_time";
  private static final String TEMP_FIELD_NAME = "temp";
  private static final String UOM_FIELD_NAME = "uom";

  public Handler() {
  }

  // This is for the purpose of unit testing. a better approach is to use dependency injection through
  // a lightweight framework like Dagger, Micronaut or Quarkus. We will demonstrate this in another
  // sample project.
  public Handler(DynamoDbClient ddbClient) {
    this.ddbClient = ddbClient;
  }

  @Override
  public Void handleRequest(SQSEvent event, Context context) {
    for (SQSMessage message : event.getRecords()) {
      String messageBody = message.getBody();
      logger.info("Received SQS message: {}.", messageBody);

      List<WriteRequest> writeRequests = new ArrayList<>();
      List<EventInfo> eventInfoList = deserializeEventInfo(messageBody);

      for (EventInfo eventInfo : eventInfoList) {
        HashMap<String, AttributeValue> itemValueMap = getItemValueMap(eventInfo);
        WriteRequest writeRequest = WriteRequest.builder()
            .putRequest(PutRequest.builder().item(itemValueMap).build()).build();
        writeRequests.add(writeRequest);
        logger.info("Added write request for deviceId {}, month {}, day {}, hour {}, temp {}.",
            eventInfo.getDeviceId(), eventInfo.getMonth(), eventInfo.getDay(), eventInfo.getHour(),
            eventInfo.getTemperature());
      }

      HashMap<String, List<WriteRequest>> batchRequests = new HashMap<>();
      batchRequests.put(TABLE_NAME, writeRequests);
      BatchWriteItemRequest batchWriteItemRequest = BatchWriteItemRequest.builder()
          .requestItems(batchRequests).build();

      try {
        ddbClient.batchWriteItem(batchWriteItemRequest);
        logger.info("The table {} was successfully updated", TABLE_NAME);
      } catch (ResourceNotFoundException e) {
        String errorMessage = String
            .format("[ResourceNotFoundException] The table %s can't be found.", TABLE_NAME);
        logger.error(errorMessage);
        throw new IllegalArgumentException(errorMessage);
      } catch (DynamoDbException e) {
        String errorMessage = String.format("[DynamoDBException] %s.", e.getMessage());
        logger.error(errorMessage);
        throw new RuntimeException(errorMessage);
      }
    }
    return null;
  }

  private List<EventInfo> deserializeEventInfo(String json) {
    return gson.fromJson(json, new TypeToken<ArrayList<EventInfo>>() {
    }.getType());
  }

  private HashMap<String, AttributeValue> getItemValueMap(EventInfo eventInfo) {
    HashMap<String, AttributeValue> itemValueMap = new HashMap<>();
    // Add all content to the table
    itemValueMap
        .put(DEVICE_MONTH_FIELD_NAME, AttributeValue.builder()
            .s(eventInfo.getDeviceId() + "|" + eventInfo.getYear() + "|" + String
                .format("%02d", eventInfo.getMonth())).build());
    itemValueMap.put(DAY_TIME_FIELD_NAME,
        AttributeValue.builder()
            .s(String.format("%02d", eventInfo.getDay()) + "|" + getTimeFromHour(
                eventInfo.getHour())).build());
    itemValueMap.put(TEMP_FIELD_NAME,
        AttributeValue.builder().n(String.valueOf(eventInfo.getTemperature())).build());
    itemValueMap.put(UOM_FIELD_NAME, AttributeValue.builder().s(eventInfo.getUom()).build());
    return itemValueMap;
  }

  private String getTimeFromHour(int hour) {
    return String.format("%02d", hour) + ":00:00.00";
  }
}
