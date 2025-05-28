package yewpar.logger;

// Unchecked exception signalling a search timeout.

public class TimeoutException
extends RuntimeException
{
  public TimeoutException() {
    super();
  }

  public TimeoutException(String message) {
    super(message);
  }
}
