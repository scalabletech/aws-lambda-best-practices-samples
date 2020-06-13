package sample;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.RequestParametersEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.ResponseElementsEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3BucketEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3Entity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3ObjectEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.UserIdentityEntity;

public class HandlerTest {

  @Test
  public void testHandleRequest() throws IOException {
    final String bucketName = new String(Files.readAllLines(Paths.get("bucket-name.tmp")).get(0));
    final String objectKey = "lambda-trigger-destinations-sample.png";
    final String eventName = "ObjectCreated:Put";

    S3EventNotificationRecord notificationRecord = new S3EventNotificationRecord("us-east-2",
        eventName,
        "aws:s3",
        "2020-06-11T11:03:18.237Z",
        "2.1",
        new RequestParametersEntity("54.230.227.61"),
        new ResponseElementsEntity(
            "yWTytwuyetiwuyteytwuytew23/kkHiw2987GJHVjhegjhdbjshgjgjhg2gjhwgjhgjhahgjhge2/87623gwyut23bvsw2S",
            "AF2D7AB6002E898D"),
        new S3Entity("sg276533-uwsd-8dqy-9165-695162te2676",
            new S3BucketEntity(bucketName,
                new UserIdentityEntity("IYW287TUYGW238"),
                "arn:aws:s3:::" + bucketName),
            new S3ObjectEntity(objectKey,
                44855l,
                "g2yjhbvdbnv2jhg23",
                "",
                "892UTP273917283"),
            "1.0"),
        new UserIdentityEntity("AWS:UOQYHWHGSVQHW"));

    List<S3EventNotificationRecord> notificationRecords = List.of(notificationRecord);
    S3Event s3Event = new S3Event(notificationRecords);

    Handler handler = new Handler();
    EventInfo eventInfo = (EventInfo) handler.handleRequest(s3Event, new StubContext());

    assertTrue(eventInfo.getBucketName().equals(bucketName));
    assertTrue(eventInfo.getObjectKey().equals(objectKey));
    assertTrue(eventInfo.getEventName().equals(eventName));
  }
}
