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
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

public class Handler implements RequestHandler<SQSEvent, Void> {

  private static final Logger LOGGER = LogManager.getLogger(Handler.class);
  private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  private static final Region REGION = Region.US_EAST_1;

  // For better cold start performance on a Lambda function the below variables can be declared and
  // initialized statically. This will be demonstrated in another sample.
  private DynamoDbClient ddbClient = DynamoDbClient.builder().region(REGION).build();
  private SsmClient ssmClient = SsmClient.builder().build();
  private ConfigurationSettings configSettings = null;

  private static final String TABLE_NAME =
      System.getenv("TABLE_NAME") == null ? "temperature_device_reading"
          : System.getenv("TABLE_NAME");
  private static final String DEVICE_MONTH_FIELD_NAME = "device_month";
  private static final String DAY_TIME_FIELD_NAME = "day_time";
  private static final String TEMP_FIELD_NAME = "temp";
  private static final String UOM_FIELD_NAME = "uom";

  private void loadConfigSettings() {
    LOGGER.info("About to fetch configuration settings from AWS Parameter Store.");
    GetParameterRequest request = GetParameterRequest.builder().withDecryption(false)
        .name("/lambda-sqs-dynamodb-sample/config").build();
    GetParameterResponse response = ssmClient.getParameter(request);

    LOGGER.info("Parameter value: " + response.parameter().value());
    configSettings = deserializeConfigurationSettings(response.parameter().value());
  }

  public Handler() {
  }

  // This is for the purpose of unit testing. a better approach is to use dependency injection through
  // a lightweight framework like Dagger, Micronaut or Quarkus. We will demonstrate this in another
  // sample project.
  public Handler(DynamoDbClient ddbClient, SsmClient ssmClient) {
    this.ddbClient = ddbClient;
    this.ssmClient = ssmClient;
  }

  @Override
  public Void handleRequest(SQSEvent event, Context context) {
    if (configSettings == null) {
      loadConfigSettings();
    }
    LOGGER.info("Configuration settings are minTemperature {}, maxTemperature {}.",
        configSettings.getMinTemperature(), configSettings.getMaxTemperature());

    for (SQSMessage message : event.getRecords()) {
      String messageBody = message.getBody();
      LOGGER.info("Received SQS message: {}.", messageBody);

      List<WriteRequest> writeRequests = new ArrayList<>();
      List<TemperatureDeviceReading> deviceReadings = deserializeEventInfo(messageBody);

      for (TemperatureDeviceReading deviceReading : deviceReadings) {
        if (deviceReading.getTemperature() < configSettings.getMinTemperature()
            || deviceReading.getTemperature() > configSettings.getMaxTemperature()) {
          LOGGER.warn("Temperature reading of {} is out of range ({} - {}) and will be discarded.",
              deviceReading.getTemperature(), configSettings.getMinTemperature(),
              configSettings.getMaxTemperature());
          continue;
        }

        HashMap<String, AttributeValue> itemValueMap = getItemValueMap(deviceReading);
        WriteRequest writeRequest = WriteRequest.builder()
            .putRequest(PutRequest.builder().item(itemValueMap).build()).build();
        writeRequests.add(writeRequest);
        LOGGER.info("Added write request for deviceId {}, month {}, day {}, hour {}, temp {}.",
            deviceReading.getDeviceId(), deviceReading.getMonth(), deviceReading
                .getDay(), deviceReading.getHour(),
            deviceReading.getTemperature());
      }

      if (writeRequests.isEmpty()) {
        LOGGER.info("There are no temperature readings to persist.");
        return null;
      }

      HashMap<String, List<WriteRequest>> batchRequests = new HashMap<>();
      batchRequests.put(TABLE_NAME, writeRequests);
      BatchWriteItemRequest batchWriteItemRequest = BatchWriteItemRequest.builder()
          .requestItems(batchRequests).build();

      try {
        ddbClient.batchWriteItem(batchWriteItemRequest);
        LOGGER.info("The table {} was successfully updated", TABLE_NAME);
      } catch (ResourceNotFoundException e) {
        String errorMessage = String
            .format("[ResourceNotFoundException] The table %s can't be found.", TABLE_NAME);
        LOGGER.error(errorMessage);
        throw new IllegalArgumentException(errorMessage);
      } catch (DynamoDbException e) {
        String errorMessage = String.format("[DynamoDBException] %s.", e.getMessage());
        LOGGER.error(errorMessage);
        throw new RuntimeException(errorMessage);
      }
    }
    return null;
  }

  private List<TemperatureDeviceReading> deserializeEventInfo(String json) {
    return gson.fromJson(json, new TypeToken<ArrayList<TemperatureDeviceReading>>() {
    }.getType());
  }

  private ConfigurationSettings deserializeConfigurationSettings(String json) {
    return gson.fromJson(json, ConfigurationSettings.class);
  }

  private HashMap<String, AttributeValue> getItemValueMap(
      TemperatureDeviceReading temperatureDeviceReading) {
    HashMap<String, AttributeValue> itemValueMap = new HashMap<>();
    // Add all content to the table
    itemValueMap
        .put(DEVICE_MONTH_FIELD_NAME, AttributeValue.builder()
            .s(temperatureDeviceReading.getDeviceId() + "|" + temperatureDeviceReading.getYear()
                + "|" + String
                .format("%02d", temperatureDeviceReading.getMonth())).build());
    itemValueMap.put(DAY_TIME_FIELD_NAME,
        AttributeValue.builder()
            .s(String.format("%02d", temperatureDeviceReading.getDay()) + "|" + getTimeFromHour(
                temperatureDeviceReading.getHour())).build());
    itemValueMap.put(TEMP_FIELD_NAME,
        AttributeValue.builder().n(String.valueOf(temperatureDeviceReading.getTemperature()))
            .build());
    itemValueMap
        .put(UOM_FIELD_NAME, AttributeValue.builder().s(temperatureDeviceReading.getUom()).build());
    return itemValueMap;
  }

  private String getTimeFromHour(int hour) {
    return String.format("%02d", hour) + ":00:00.00";
  }
}
