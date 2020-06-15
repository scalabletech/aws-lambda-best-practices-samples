package sample;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.google.gson.JsonSyntaxException;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;

@ExtendWith(MockitoExtension.class)
public class HandlerTest {

  private static final String DEVICE_MONTH_FIELD_NAME = "device_month";
  private static final String DAY_TIME_FIELD_NAME = "day_time";
  private static final String TEMP_FIELD_NAME = "temp";
  private static final String UOM_FIELD_NAME = "uom";

  ArgumentCaptor<BatchWriteItemRequest> argCaptor;

  @Mock
  DynamoDbClient ddbClient;

  @Mock
  SsmClient ssmClient;

  @BeforeEach
  public void beforeEach() {
    when(ssmClient.getParameter(any(GetParameterRequest.class)))
        .thenReturn(GetParameterResponse
            .builder().parameter(Parameter.builder().name("config")
                .value("{'minTemperature': -90, 'maxTemperature': 135}").build())
            .build());
  }

  @Test
  public void testHandleRequestWithBadMessage() {
    SQSEvent sqsEvent = new SQSEvent();
    SQSEvent.SQSMessage sqsMessage = new SQSEvent.SQSMessage();
    sqsMessage.setBody("Test");
    sqsEvent.setRecords(List.of(sqsMessage));

    Handler handler = new Handler(ddbClient, ssmClient);

    Assertions.assertThrows(JsonSyntaxException.class,
        () -> handler.handleRequest(sqsEvent, new StubContext()));
  }

  @Test
  public void testHandleRequestWithOneItem() {
    SQSEvent sqsEvent = new SQSEvent();
    SQSEvent.SQSMessage sqsMessage = new SQSEvent.SQSMessage();
    sqsMessage.setBody("[{\n"
        + "  \"deviceId\": \"1234\",\n"
        + "  \"year\": 2020,\n"
        + "  \"month\": 6,\n"
        + "  \"day\": 1,\n"
        + "  \"hour\": 11,\n"
        + "  \"temperature\": 80,\n"
        + "  \"uom\": \"f\"\n"
        + "}]");
    sqsEvent.setRecords(List.of(sqsMessage));

    argCaptor = ArgumentCaptor
        .forClass(BatchWriteItemRequest.class);
    when(ddbClient.batchWriteItem(argCaptor.capture()))
        .thenReturn(BatchWriteItemResponse.builder().build());

    Handler handler = new Handler(ddbClient, ssmClient);
    handler.handleRequest(sqsEvent, new StubContext());

    BatchWriteItemRequest batchWriteItemRequest = argCaptor.getValue();
    Map<String, List<WriteRequest>> batchRequests = batchWriteItemRequest.requestItems();

    assertEquals(1, batchRequests.size());

    for (Map.Entry<String, List<WriteRequest>> entry : batchRequests.entrySet()) {
      // There will be one WriteRequest object
      WriteRequest writeRequest = entry.getValue().get(0);
      assertEquals("1234|2020|06",
          writeRequest.putRequest().item().get(DEVICE_MONTH_FIELD_NAME).s());
      assertEquals("01|11:00:00.00", writeRequest.putRequest().item().get(DAY_TIME_FIELD_NAME).s());
      assertEquals("80", writeRequest.putRequest().item().get(TEMP_FIELD_NAME).n());
      assertEquals("f", writeRequest.putRequest().item().get(UOM_FIELD_NAME).s());
    }
  }

  @Test
  public void testHandleRequestWithTwoItems() {
    SQSEvent sqsEvent = new SQSEvent();
    SQSEvent.SQSMessage sqsMessage = new SQSEvent.SQSMessage();
    sqsMessage.setBody("[{\n"
        + "  \"deviceId\": \"1234\",\n"
        + "  \"year\": 2020,\n"
        + "  \"month\": 6,\n"
        + "  \"day\": 1,\n"
        + "  \"hour\": 8,\n"
        + "  \"temperature\": 75,\n"
        + "  \"uom\": \"f\"\n"
        + "},\n"
        + "{\n"
        + "  \"deviceId\": \"1234\",\n"
        + "  \"year\": 2020,\n"
        + "  \"month\": 6,\n"
        + "  \"day\": 1,\n"
        + "  \"hour\": 9,\n"
        + "  \"temperature\": 77,\n"
        + "  \"uom\": \"f\"\n"
        + "}]");
    sqsEvent.setRecords(List.of(sqsMessage));

    argCaptor = ArgumentCaptor
        .forClass(BatchWriteItemRequest.class);
    when(ddbClient.batchWriteItem(argCaptor.capture()))
        .thenReturn(BatchWriteItemResponse.builder().build());

    Handler handler = new Handler(ddbClient, ssmClient);
    handler.handleRequest(sqsEvent, new StubContext());

    BatchWriteItemRequest batchWriteItemRequest = argCaptor.getValue();
    Map<String, List<WriteRequest>> batchRequests = batchWriteItemRequest.requestItems();

    assertEquals(1, batchRequests.size());

    for (Map.Entry<String, List<WriteRequest>> entry : batchRequests.entrySet()) {
      // There will be two WriteRequest objects
      WriteRequest writeRequest = entry.getValue().get(0);
      assertEquals("1234|2020|06",
          writeRequest.putRequest().item().get(DEVICE_MONTH_FIELD_NAME).s());
      assertEquals("01|08:00:00.00", writeRequest.putRequest().item().get(DAY_TIME_FIELD_NAME).s());
      assertEquals("75", writeRequest.putRequest().item().get(TEMP_FIELD_NAME).n());
      assertEquals("f", writeRequest.putRequest().item().get(UOM_FIELD_NAME).s());

      writeRequest = entry.getValue().get(1);
      assertEquals("1234|2020|06",
          writeRequest.putRequest().item().get(DEVICE_MONTH_FIELD_NAME).s());
      assertEquals("01|09:00:00.00", writeRequest.putRequest().item().get(DAY_TIME_FIELD_NAME).s());
      assertEquals("77", writeRequest.putRequest().item().get(TEMP_FIELD_NAME).n());
      assertEquals("f", writeRequest.putRequest().item().get(UOM_FIELD_NAME).s());
    }
  }

  @Test
  public void testHandleRequestWithLowTemp() {
    SQSEvent sqsEvent = new SQSEvent();
    SQSEvent.SQSMessage sqsMessage = new SQSEvent.SQSMessage();
    sqsMessage.setBody("[{\n"
        + "  \"deviceId\": \"1234\",\n"
        + "  \"year\": 2020,\n"
        + "  \"month\": 6,\n"
        + "  \"day\": 1,\n"
        + "  \"hour\": 11,\n"
        + "  \"temperature\": -100,\n"
        + "  \"uom\": \"f\"\n"
        + "}]");
    sqsEvent.setRecords(List.of(sqsMessage));

    Handler handler = new Handler(ddbClient, ssmClient);
    handler.handleRequest(sqsEvent, new StubContext());

    verify(ddbClient, never()).batchWriteItem(any(BatchWriteItemRequest.class));
  }

  @Test
  public void testHandleRequestWithHighTemp() {
    SQSEvent sqsEvent = new SQSEvent();
    SQSEvent.SQSMessage sqsMessage = new SQSEvent.SQSMessage();
    sqsMessage.setBody("[{\n"
        + "  \"deviceId\": \"1234\",\n"
        + "  \"year\": 2020,\n"
        + "  \"month\": 6,\n"
        + "  \"day\": 1,\n"
        + "  \"hour\": 11,\n"
        + "  \"temperature\": 145,\n"
        + "  \"uom\": \"f\"\n"
        + "}]");
    sqsEvent.setRecords(List.of(sqsMessage));

    Handler handler = new Handler(ddbClient, ssmClient);
    handler.handleRequest(sqsEvent, new StubContext());

    verify(ddbClient, never()).batchWriteItem(any(BatchWriteItemRequest.class));
  }
}
