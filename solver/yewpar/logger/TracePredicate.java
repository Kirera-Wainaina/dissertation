package yewpar.logger;

// Interface to capture when to trace the current state of the logger.

import yewpar.logger.Logger.Event;

@FunctionalInterface
public interface TracePredicate {
  // If test() is true, the state of the logger will be traced (i.e. printed).
  // Trace predicates take three arguments:
  // * Event currently being logged: e
  // * Current events count: evts
  // * Size of current node generator stack: genStackDepth
  boolean test(Event e, long evts, int genStackDepth);
}
