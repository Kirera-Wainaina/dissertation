package yewpar.logger;

// Dummy logger - not actually logging anything.

import java.util.Stack;
import yewpar.CountingGenerator;
import yewpar.logger.TimeoutException;

public class NoLogger<Node>
implements Logger<Node>
{
  // number of iterations that triggers a timeout; negative for no timeout
  private long iterBound;

  public NoLogger() {
    this.iterBound = -1;
  }

  public void log(Event e,
                  long i,
                  Stack<? extends CountingGenerator<Node>> s) {}

  public void logStrengthen(String o,
                            long i,
                            Stack<? extends CountingGenerator<Node>> s) {}

  public void setTimeout(long iterBound) {
    this.iterBound = iterBound;
  }

  public void timeout(long i) {
    if (iterBound < 0)
      return;
    if (i > iterBound)
      throw new TimeoutException();
  }
}
