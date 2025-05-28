package yewpar.logger;

// Utilities for command-line parsing of logger options.

import java.util.Scanner;
import yewpar.logger.Logger.Event;
import yewpar.logger.TracePredicate;

public class Utils
{
  // Returns a non-negative number N if option -timeout=N was found;
  // -1 otherwise.
  public static long parseOptTimeout(String[] args) {
    for (int i = 0; i < args.length; i++) {
      try {
        Scanner s = new Scanner(args[i]);
        s.skip("-timeout=");
        long n = s.nextLong();
	if (n >= 0)
          return n;
      } catch (Exception e) {}
    }
    return -1;
  }
    
  // Returns true iff option '-countlogger' was found.
  public static boolean parseOptCountLogger(String[] args) {
    for (int i = 0; i < args.length; i++)
      if (args[i].equals("-countlogger"))
        return true;
    return false;
  }

  // Returns true iff option '-histlogger' was found.
  public static boolean parseOptHistLogger(String[] args) {
    for (int i = 0; i < args.length; i++)
      if (args[i].equals("-histlogger"))
        return true;
    return false;
  }

  // Returns true iff option -strengthen was found.
  public static boolean parseOptStrengthen(String[] args) {
    for (int i = 0; i < args.length; i++)
      if (args[i].equals("-strengthen"))
        return true;
    return false;
  }

  // Returns a positive number N if option -evts=N was found;
  // -1 otherwise.
  public static long parseOptEvts(String[] args) {
    for (int i = 0; i < args.length; i++) {
      try {
        Scanner s = new Scanner(args[i]);
        s.skip("-evts=");
        long n = s.nextLong();
	if (n > 0)
          return n;
      } catch (Exception e) {}
    }
    return -1;
  }

  // Returns a non-negative number N if option -stackdepth=N was found;
  // -1 otherwise.
  public static int parseOptStackdepth(String[] args) {
    for (int i = 0; i < args.length; i++) {
      try {
        Scanner s = new Scanner(args[i]);
        s.skip("-stackdepth=");
        int n = s.nextInt();
	if (n >= 0)
          return n;
      } catch (Exception e) {}
    }
    return -1;
  }

  // Returns a non-negative number N if option -maxstackdepth=N was found;
  // -1 otherwise.
  public static int parseOptMaxstackdepth(String[] args) {
    for (int i = 0; i < args.length; i++) {
      try {
        Scanner s = new Scanner(args[i]);
        s.skip("-maxstackdepth=");
        int n = s.nextInt();
	if (n >= 0)
          return n;
      } catch (Exception e) {}
    }
    return -1;
  }

  // Processes options and returns a TracePredicate.
  public static TracePredicate mkTracePredicate(String[] args) {
    boolean strengthen = parseOptStrengthen(args);
    long evts_freq = parseOptEvts(args);
    int depth = parseOptStackdepth(args);
    int maxdepth = parseOptMaxstackdepth(args);
    return (e, evts, stack_depth) ->
           (!strengthen ? false : e == Event.STRENGTHEN) ||
           (evts_freq > 0 ? evts % evts_freq == 0 : false) ||
           (depth >= 0 ? stack_depth == depth : false) ||
           (maxdepth >= 0 ? stack_depth <= maxdepth : false);
  }
}
