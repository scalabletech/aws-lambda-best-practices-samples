package sample;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

public class StubContext implements Context {

  public String getAwsRequestId() {
    return "skjh23e2-uwsd-8dqy-9165-695162te2676";
  }

  public String getLogGroupName() {
    return "/aws/lambda/lambda-trigger-destinations-sample";
  }

  public String getLogStreamName() {
    return "2020/06/11/[$LATEST]625efyq614283giqne3962jkhkh21pwe";
  }

  public String getFunctionName() {
    return "lambda-trigger-destinations-sample";
  }

  public String getFunctionVersion() {
    return "$LATEST";
  }

  public String getInvokedFunctionArn() {
    return "arn:aws:lambda:us-east-1:23768265731:function:lambda-trigger-destinations-sample";
  }

  public CognitoIdentity getIdentity() {
    return null;
  }

  public ClientContext getClientContext() {
    return null;
  }

  public int getRemainingTimeInMillis() {
    return 300000;
  }

  public int getMemoryLimitInMB() {
    return 512;
  }

  public LambdaLogger getLogger() {
    return new TestLogger();
  }
}