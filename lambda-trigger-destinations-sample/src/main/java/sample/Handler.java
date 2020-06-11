package sample;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Handler implements RequestHandler<S3Event, Object> {

  private static final Logger logger = LogManager.getLogger(Handler.class);

  private static final List<String> INVALID_EXTENSIONS = List.of(".exe", ".zip");

  @Override
  public Object handleRequest(S3Event event, Context context) {
    S3EventNotificationRecord record = event.getRecords().get(0);
    String eventName = record.getEventName();
    String bucketName = record.getS3().getBucket().getName();
    String objectKey = record.getS3().getObject().getUrlDecodedKey();

    // Log the event details
    logger.info(
        "Received notification for S3 event {}, bucket {}, object key {}.",
        eventName, bucketName, objectKey);

    EventInfo eventInfo = new EventInfo(eventName, bucketName,
        objectKey);

    // Simulate a failure state - check for an "invalid" file extension
    if (INVALID_EXTENSIONS.stream().anyMatch(x -> objectKey.toLowerCase().endsWith(x))) {
      throw new IllegalArgumentException(String
          .format("Invalid extension for object %s uploaded to bucket %s.",
              eventInfo.getObjectKey(), eventInfo.getBucketName()));
    }

    // Return the event details for downstream services to process
    return eventInfo;
  }
}
