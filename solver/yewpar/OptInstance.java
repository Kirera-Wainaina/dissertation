package yewpar;

// Optimisation instance

import java.util.Stack;
import yewpar.logger.NoLogger;
import yewpar.logger.Logger;
import yewpar.logger.Logger.Event;

public abstract class OptInstance<Node, T extends Comparable<T>>
extends Instance<Node, T>
{
  public OptInstance(Node root, Generator<Node> gen0) {
    super(root, gen0);
  }

  // Pruning predicate;
  // returns 1 if the subtree rooted at the current node can never beat
  // the objective function of the incumbent;
  // returns -1 if the subtree at the current node and all subtrees at
  // subsequent siblings of the current node cannot beat the incumbent;
  // returns 0 otherwise.
  public int prune(Node current, Node incumbent) {
    return 0;  // default: no pruning
  }

  // Search the tree and return a node that maximises the objective function.
  // Assumes that the objective function of the root node is minimal, and that
  // the pruning predicate is admissible.
  public Node search() {
    return search(new NoLogger<Node>());
  }

  // Search the tree and return a node that maximises the objective function.
  // Assumes that the objective function of the root node is minimal, and that
  // the pruning predicate is admissible.
  // The search will be logged by the Logger provided.
  public Node search(Logger<Node> lg) {
      return searchUntil(null, lg);
  }

  // Search the tree and return a node that maximises the objective function.
  // The search may end with by shortcircuiting as soon as a node is found whose
  // objective value equals greatest. (If a node whose objective function equals
  // greatest is returned, there is no guarantee that this node actually
  // maximises the objective function; choice of greatest is application
  // dependent). If greatest == null, the search will not be shortcircuited.
  // Assumes that the objective function of the root node is minimal, and that
  // the pruning predicate is admissible.
  public Node searchUntil(T greatest) {
      return searchUntil(greatest, new NoLogger<Node>());
  }

  // Search the tree and return a node that maximises the objective function.
  // The search may end with by shortcircuiting as soon as a node is found whose
  // objective value equals greatest. (If a node whose objective function equals
  // greatest is returned, there is no guarantee that this node actually
  // maximises the objective function; choice of greatest is application
  // dependent). If greatest == null, the search will not be shortcircuited.
  // Assumes that the objective function of the root node is minimal, and that
  // the pruning predicate is admissible.
  // The search will be logged by the Logger provided.
  public Node searchUntil(T greatest, Logger<Node> lg) {
    long iter = 0;
    Node incumbent = root;
    T obj_incumbent = objective(incumbent);
    Stack<CountingGenerator<Node>> genStack = new Stack<>();
    // push root node generator
    lg.log(Event.EXPAND, iter, genStack);
    genStack.push(new WrappedGenerator<Node>(gen0.children(root)));
    while (!genStack.empty()) {
      iter++;
      lg.timeout(iter);
      CountingGenerator<Node> gen = genStack.peek();
      int k = gen.hasNext();
      if (k > 0) {
        Node child = gen.next();
        T obj_child = objective(child);
        if (obj_child.compareTo(obj_incumbent) > 0) {
          // strengthen incumbent
          lg.logStrengthen(objectiveToJSON(obj_child), iter, genStack);
          incumbent = child;
          obj_incumbent = obj_child;
          if (greatest != null && greatest.equals(obj_child)) {
            // short-circuit (and terminate search)
            lg.log(Event.SHORTCIRCUIT, iter, genStack);
            lg.log(Event.TERMINATE, iter, genStack);
            return incumbent;
          }
          // or push child's node generator
          lg.log(Event.EXPAND, iter, genStack);
          genStack.push(new WrappedGenerator<Node>(gen.children(child)));
        }
        else {
          switch (prune(child, incumbent)) {
            case 0:  // no pruning: expand by pushing child's node generator
              lg.log(Event.EXPAND, iter, genStack);
              genStack.push(new WrappedGenerator<Node>(gen.children(child)));
              break;
            case 1:  // prune subtree: do nothing
              lg.log(Event.PRUNE, iter, genStack);
              break;
            case -1: // prune subtree + siblings "to the right" (-> backtrack)
              lg.log(Event.PRUNEBACKTRACK, iter, genStack);
              genStack.pop();
              break;
            default: // PANIC: this mustn't happen
              throw new RuntimeException("prune() returned illegal value.");
          }
        }
      }
      else {
        // closing this branch because there are no children (-> backtrack)
        lg.log(Event.BACKTRACK, iter, genStack);
        genStack.pop();
      }
    }
    // search complete: terminate
    lg.log(Event.TERMINATE, iter, genStack);
    return incumbent;
  }
}
