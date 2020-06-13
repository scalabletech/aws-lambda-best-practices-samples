package sample;

public class EventInfo {

  private final String eventName;
  private final String bucketName;
  private final String objectKey;

  public EventInfo(String eventName, String bucket, String objectKey) {
    this.eventName = eventName;
    this.bucketName = bucket;
    this.objectKey = objectKey;
  }

  public String getEventName() {
    return eventName;
  }

  public String getBucketName() {
    return bucketName;
  }

  public String getObjectKey() {
    return objectKey;
  }
}
