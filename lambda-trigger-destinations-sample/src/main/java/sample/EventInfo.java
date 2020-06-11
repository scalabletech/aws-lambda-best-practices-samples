package sample;

public class EventInfo {

  private String eventName;
  private String bucketName;
  private String objectKey;

  public EventInfo(String eventName, String bucket, String objectKey) {
    this.eventName = eventName;
    this.bucketName = bucket;
    this.objectKey = objectKey;
  }

  public String getEventName() {
    return eventName;
  }

  public void setEventName(String eventName) {
    this.eventName = eventName;
  }

  public String getBucketName() {
    return bucketName;
  }

  public void setBucketName(String bucketName) {
    this.bucketName = bucketName;
  }

  public String getObjectKey() {
    return objectKey;
  }

  public void setObjectKey(String objectKey) {
    this.objectKey = objectKey;
  }
}
