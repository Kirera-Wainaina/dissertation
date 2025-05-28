package yewpar;

// Decision instance

import java.util.Stack;
import yewpar.logger.NoLogger;
import yewpar.logger.Logger;
import yewpar.logger.Logger.Event;

public abstract class DecInstance<Node, T extends Comparable<T>>
extends OptInstance<Node, T>
{
  public DecInstance(Node root, Generator<Node> gen0) {
    super(root, gen0);
  }

  // Search the tree and return a node whose objective function matches
  // the given greatest element if such a node exists; returns null otherwise.
  // Assumes that the objective function of the root node is minimal, that
  // the objective function is bounded from above by greatest, and that
  // the pruning predicate is admissible.
  public Node search(T greatest) {
    return search(greatest, new NoLogger<Node>());
  }

  // Search the tree and return a node whose objective function matches
  // the given greatest element if such a node exists; returns null otherwise.
  // Assumes that the objective function of the root node is minimal, that
  // the objective function is bounded from above by greatest, and that
  // the pruning predicate is admissible.
  // The search will be logged by the Logger provided.
  public Node search(T greatest, Logger<Node> lg) {
    Node x = searchUntil(greatest, lg);
    if (greatest.equals(objective(x)))
      return x;
    // search completed unsuccessfully: return null
    return null;
  }
}
