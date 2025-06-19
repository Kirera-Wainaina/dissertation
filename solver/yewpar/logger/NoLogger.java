package yewpar.logger;

// Dummy logger - not actually logging anything.

import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;
import yewpar.CountingGenerator;
import yewpar.logger.TimeoutException;

public class NoLogger<Node>
implements Logger<Node>
{
  // number of iterations that triggers a timeout; negative for no timeout
  private long iterBound;

  // flag to indicate that solver should stop due to timeout
  private AtomicBoolean stopFlag;

  public NoLogger() {
    this.iterBound = -1;
    this.stopFlag = new AtomicBoolean(false);
  }

  public void log(Event e,
                  long i,
                  Stack<? extends CountingGenerator<Node>> s) {}

  public void logStrengthen(String o,
                            long i,
                            Stack<? extends CountingGenerator<Node>> s) {}

  // Sets a timeout iteration bound unless iterBound < 0
  public void setTimeout(long iterBound) {
    if (iterBound < 0)
      return;
    this.iterBound = iterBound;
  }

  // Sets a timeout time limit (in milliseconds) and starts a timer thread
  // unless timeLimit < 0
  public void setTimeoutMillis(long timeLimit) {
    if (timeLimit < 0)
      return;
    Thread timer =
      new Thread(() -> {
        try { Thread.sleep(timeLimit); } catch (Exception e) {}
        stopFlag.set(true);
      });
    timer.setDaemon(true);
    timer.start();
  }

  // Checks for timeout; throws TimeoutException
  public void timeout(long i, Stack<? extends CountingGenerator<Node>> s) {
    if (stopFlag.get() || iterBound >= 0 && i >= iterBound)
      throw new TimeoutException();
  }
}
