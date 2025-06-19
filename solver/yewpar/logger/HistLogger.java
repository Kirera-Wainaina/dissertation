package yewpar.logger;

// Histogram logger - logging events and tracing histograms of event types.

import java.util.ArrayList;
import java.util.Stack;
import yewpar.Generator;
import yewpar.CountingGenerator;

public class HistLogger<Node>
extends NoLogger<Node>
implements Logger<Node>
{
  // max recorded stack depth; -1 if no event has been recorded yet
  private int maxStackDepth;

  // Count for all events
  private long evts;

  // Histograms for events that may occur repeatedly
  private ArrayList<Long> expandHist;
  private ArrayList<Long> backtrackHist;
  private ArrayList<Long> pruneBacktrackHist;
  private ArrayList<Long> pruneHist;
  private ArrayList<Long> strengthenHist;
  private ArrayList<Long> shortcircuitHist; // occurs at most once (always last)

  // Timestamps (i.e. iter count) for events that occur at most once;
  // -1 if event has not (yet) occured
  private long terminateAt;
  private long timeoutAt;

  // Trace predicate
  private TracePredicate tp;

  // Default constructor; sets trace predicate tp to never print.
  public HistLogger() {
    this((e, evts, genStackDepth) -> false);
  }

  // Constructor; sets the given trace predicate (e.g. a lambda expression)
  public HistLogger(TracePredicate tp) {
    this.maxStackDepth = -1;
    this.evts = 0;
    this.expandHist = mkHist();
    this.backtrackHist = mkHist();
    this.pruneBacktrackHist = mkHist();
    this.pruneHist = mkHist();
    this.strengthenHist = mkHist();
    this.shortcircuitHist = mkHist();
    this.terminateAt = -1;
    this.timeoutAt = -1;
    this.tp = tp;
  }

  // Getters
  public long maxStackDepth() { return maxStackDepth; }
  public long evts() { return evts; }
  public ArrayList<Long> expandHist() { return cpHist(expandHist); }
  public ArrayList<Long> backtrackHist() { return cpHist(backtrackHist); }
  public ArrayList<Long> pruneBacktrackHist() { return cpHist(pruneBacktrackHist); }
  public ArrayList<Long> pruneHist() { return cpHist(pruneHist); }
  public ArrayList<Long> strengthenHist() { return cpHist(strengthenHist); }
  public ArrayList<Long> shortcircuitHist() { return cpHist(shortcircuitHist); }
  public long terminateAt() { return terminateAt; }

  // Converts current log into a JSON object representation
  public String toString() { return "{" + convertToString() + "}"; }

  // Logs an event of type e during iteration i and with generator stack s.
  public void log(Event e,
                  long i,
                  Stack<? extends CountingGenerator<Node>> s) {
    log(e, i, s, null);
  }

  // Logs a STRENGTHEN event during iteration i and with generator stack s;
  // String o is a JSON representation of the strengthen value
  // of the objective function
  public void logStrengthen(String o,
                            long i,
                            Stack<? extends CountingGenerator<Node>> s) {
    log(Event.STRENGTHEN, i, s, o);
  }

  // Checks for timeout; logs TIMEOUT event and throws TimeoutException
  public void timeout(long i, Stack<? extends CountingGenerator<Node>> s) {
    try {
      super.timeout(i, s);
    }
    catch (TimeoutException e) {
      log(Event.TIMEOUT, i, s);
      throw e;
    }
  }

  // Logs an event of type e during iteration i and with generator stack s;
  // String o is either null or a JSON representation of the current value
  // of the objective function.
  private void log(Event e,
                   long i,
                   Stack<? extends CountingGenerator<Node>> s,
                   String o) {
    int stackDepth = s.size();
    if (stackDepth > maxStackDepth) {
      maxStackDepth = stackDepth;
      resizeHist(expandHist, maxStackDepth);
      resizeHist(backtrackHist, maxStackDepth);
      resizeHist(pruneBacktrackHist, maxStackDepth);
      resizeHist(pruneHist, maxStackDepth);
      resizeHist(strengthenHist, maxStackDepth);
      resizeHist(shortcircuitHist, maxStackDepth);
    }
    evts++;
    switch (e) {
      case EXPAND:         incHist(expandHist, stackDepth); break;
      case BACKTRACK:      incHist(backtrackHist, stackDepth); break;
      case PRUNEBACKTRACK: incHist(pruneBacktrackHist, stackDepth); break;
      case PRUNE:          incHist(pruneHist, stackDepth); break;
      case STRENGTHEN:     incHist(strengthenHist, stackDepth); break;
      case SHORTCIRCUIT:   incHist(shortcircuitHist, stackDepth); break;
      case TERMINATE:      terminateAt = i; break;
      case TIMEOUT:        timeoutAt = i; break;
      default: // PANIC: this mustn't happen
        throw new RuntimeException("log() called with illegal event.");
    }
    // print JSON object if trace predicate is true or TERMINATE/TIMEOUT event
    if (tp.test(e, evts, stackDepth) ||
        e == Event.TERMINATE || e == Event.TIMEOUT) {
       System.out.println("{\"iter\":" + i +
                          ",\"event\":\"" + e + "\"" +
                          (o == null ? "" : ",\"objective\":" + o) +
                          ",\"stackDepth\":" + stackDepth +
                          ",\"path\":" + pathToString(s) +
                          ",\"stack\":" + unexploredToString(s) +
                          "," + convertToString() + "}");
    }
  }

  // Converts state of this logger object into a comma-separated list
  // of key/value pairs (i.e. a JSON object minus the { and }).
  private String convertToString() {
    return "\"maxStackDepth\":" + maxStackDepth +
           ",\"evts\":" + evts +
           ",\"expandHist\":" + toStringHist(expandHist) +
           ",\"backtrackHist\":" + toStringHist(backtrackHist) +
           ",\"pruneBacktrackHist\":" + toStringHist(pruneBacktrackHist) +
           ",\"pruneHist\":" + toStringHist(pruneHist) +
           ",\"strengthenHist\":" + toStringHist(strengthenHist) +
           ",\"shortcircuitHist\":" + toStringHist(shortcircuitHist) +
           (terminateAt < 0 ? "" : ",\"terminateAt\":" + terminateAt) +
           (timeoutAt < 0 ? "" : ",\"timeoutAt\":" + timeoutAt);
  }

  // Converts generator stack stack into a JSON array of non-negative numbers
  // of to-be-explored sibling elements per level (starting at the root level);
  // this array is a representation of the size of the unexplored search space.
  private String unexploredToString(Stack<? extends Generator<Node>> stack) {
    String str = "";
    String delim = "";
    for (Generator<Node> gen : stack) {
      str += delim + gen.hasNext();
      delim = ",";
    }
    return "[" + str + "]";
  }

  // Converts generator stack stack into a JSON array of positive numbers,
  // representing a path through the search tree from the root to the current
  // leaf node.
  private String pathToString(Stack<? extends CountingGenerator<Node>> stack) {
    String str = "";
    String delim = "";
    for (CountingGenerator<Node> gen : stack) {
      str += delim + gen.getNextCalls();
      delim = ",";
    }
    return "[" + str + "]";
  }

  // Returns a new empty histogram.
  private ArrayList<Long> mkHist() {
    return new ArrayList<>();
  }

  // Returns a copy of the histogram hist.
  private ArrayList<Long> cpHist(ArrayList<Long> hist) {
    return new ArrayList<>(hist);
  }

  // Resize histogram hist to maxStackDepth.
  private void resizeHist(ArrayList<Long> hist, int maxStackDepth) {
    for (int i = hist.size(); i <= maxStackDepth; i++)
      hist.add(0L);
  }

  // Increment histogram hist at stackDepth (assuming hist is sized suitably).
  private void incHist(ArrayList<Long> hist, int stackDepth) {
    hist.set(stackDepth, hist.get(stackDepth) + 1);
  }

  // Convert histogram hist into a JSON array representation of numbers
  // (starting at stack depth 0).
  private String toStringHist(ArrayList<Long> hist) {
    String str = "";
    String delim = "";
    for (long x : hist) {
      str += delim + x;
      delim = ",";
    }
    return "[" + str + "]";
  }
}
