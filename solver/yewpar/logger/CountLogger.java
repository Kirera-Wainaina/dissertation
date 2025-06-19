package yewpar.logger;

// Count logger - logging events and dumping counts of event types to stdout.

import java.util.Stack;
import yewpar.Generator;
import yewpar.CountingGenerator;

public class CountLogger<Node>
extends NoLogger<Node>
implements Logger<Node>
{
  // max recorded stack depth; -1 if no event has been recorded yet
  private int maxStackDepth;

  // Count for all events
  private long evts;

  // Counts for events that may occur repeatedly
  private long expandEvts;
  private long backtrackEvts;
  private long pruneBacktrackEvts;
  private long pruneEvts;
  private long strengthenEvts;
  private long shortcircuitEvts;  // occurs at most once (and always last)

  // Timestamps (i.e. iter count) for events that occur at most once;
  // -1 if event has not (yet) occured
  private long terminateAt;
  private long timeoutAt;

  // Trace predicate
  private TracePredicate tp;

  // Default constructor; sets trace predicate tp to never print.
  public CountLogger() {
    this((e, evts, genStackDepth) -> false);
  }

  // Constructor; sets the given trace predicate (e.g. a lambda expression)
  public CountLogger(TracePredicate tp) {
    this.maxStackDepth = -1;
    this.evts = 0;
    this.expandEvts = 0;
    this.backtrackEvts = 0;
    this.pruneBacktrackEvts = 0;
    this.pruneEvts = 0;
    this.strengthenEvts = 0;
    this.shortcircuitEvts = 0;
    this.terminateAt = -1;
    this.timeoutAt = -1;
    this.tp = tp;
  }

  // Getters
  public long maxStackDepth() { return maxStackDepth; }
  public long evts() { return evts; }
  public long expandEvts() { return expandEvts; }
  public long backtrackEvts() { return backtrackEvts; }
  public long pruneBacktrackEvts() { return pruneBacktrackEvts; }
  public long pruneEvts() { return pruneEvts; }
  public long strengthenEvts() { return strengthenEvts; }
  public long shortcircuitEvts() { return shortcircuitEvts; }
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
    if (stackDepth > maxStackDepth) { maxStackDepth = stackDepth; }
    evts++;
    switch (e) {
      case EXPAND:         expandEvts++; break;
      case BACKTRACK:      backtrackEvts++; break;
      case PRUNEBACKTRACK: pruneBacktrackEvts++; break;
      case PRUNE:          pruneEvts++; break;
      case STRENGTHEN:     strengthenEvts++; break;
      case SHORTCIRCUIT:   shortcircuitEvts++; break;
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
           ",\"expandEvts\":" + expandEvts +
           ",\"backtrackEvts\":" + backtrackEvts +
           ",\"pruneBacktrackEvts\":" + pruneBacktrackEvts +
           ",\"pruneEvts\":" + pruneEvts +
           ",\"strengthenEvts\":" + strengthenEvts +
           ",\"shortcircuitEvts\":" + shortcircuitEvts +
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
}
