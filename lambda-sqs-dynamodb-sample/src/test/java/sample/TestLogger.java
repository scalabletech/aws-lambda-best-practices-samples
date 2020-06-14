package sample;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.lambda.runtime.LambdaLogger;

public class TestLogger implements LambdaLogger {

  private static final Logger LOGGER = LoggerFactory.getLogger(TestLogger.class);

  public TestLogger() {
  }

  public void log(String message) {
    LOGGER.info(message);
  }

  public void log(byte[] message) {
    LOGGER.info(new String(message));
  }
}
