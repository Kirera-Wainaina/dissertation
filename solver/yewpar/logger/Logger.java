package yewpar.logger;

// Logger interface - implement to log search events and timeout searches.

import java.util.Stack;
import yewpar.CountingGenerator;

public interface Logger<Node>
{
  // Events that loggers can record
  enum Event {
    EXPAND,         // Expand search tree: push a node generator onto stack
    BACKTRACK,      // Contract search tree: pop a node generator off stack
    PRUNE,          // Prune search tree node
    PRUNEBACKTRACK, // Prune search tree node + siblings and backtrack
    STRENGTHEN,     // Strengthen the incumbent
    SHORTCIRCUIT,   // Short-circuit search (penultimate event if present)
    TERMINATE,      // Terminate search (always the last element)
    TIMEOUT,        // Timeout search (always the last element)
  }

  // Logs an event of type evt;
  // iter are the steps performed since the start of the search;
  // genStack is the current generator stack (before the event).
  // An implementation of log() may also print logs to stdout or to a file.
  void log(Event evt,
           long iter,
           Stack<? extends CountingGenerator<Node>> genStack);

  // Logs a STRENGTHEN event;
  // String oJSON is a JSON rep of the current value of the objective function;
  // iter are the steps performed since the start of the search;
  // genStack is the current generator stack (before the event).
  // An implementation of logStrengthen() may also print logs to stdout/file.
  void logStrengthen(String oJSON,
                     long iter,
                     Stack<? extends CountingGenerator<Node>> genStack);

  // Sets a timeout iteration bound; iterBound < 0 means no timeout.
  void setTimeout(long iterBound);

  // Sets a timeout time limit in milliseconds; timeLimit < 0 means no timeout.
  void setTimeoutMillis(long timeLimit);

  // Checks for timeout and throws TimeoutException if iter exceeds the bound;
  // iter are the steps performed since the start of the search;
  // genStack is the current generator stack.
  // An implementation of timeout() may also print logs to stdout/file.
  void timeout(long iter, Stack<? extends CountingGenerator<Node>> genStack);
}
