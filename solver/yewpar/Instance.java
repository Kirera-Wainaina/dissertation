package yewpar;

// Enumeration instance

import java.util.Stack;
import yewpar.logger.NoLogger;
import yewpar.logger.Logger;
import yewpar.logger.Logger.Event;

public abstract class Instance<Node, T>
{
  protected final Node root;             // root node of search tree
  protected final Generator<Node> gen0;  // initial node generator

  public Instance(Node root, Generator<Node> gen0) {
    this.root = root;
    this.gen0 = gen0;
  }

  // Objective function, mapping search tree nodes to values of type T.
  public abstract T objective(Node current);

  // Convert a value of type T into a prinbable JSON representation.
  // Defaults to toString() which should be sufficient for number types.
  public String objectiveToJSON(T value) { return value.toString(); }

  // Search the entire tree and accumulate the value of the objective function,
  // using the given accumulator accu (which is expected to start out zeroed).
  public T search(Accumulable<T> accu) {
    return search(accu, new NoLogger<Node>());
  }

  // Search the entire tree and accumulate the value of the objective function,
  // using the given accumulator accu (which is expected to start out zeroed).
  // The search will be logged by the Logger provided.
  public T search(Accumulable<T> accu, Logger<Node> lg) {
    long iter = 0;
    Stack<CountingGenerator<Node>> genStack = new Stack<>();
    // push root node generator
    lg.log(Event.EXPAND, iter, genStack);
    genStack.push(new WrappedGenerator<Node>(gen0.children(root)));
    while (!genStack.empty()) {
      iter++;
      lg.timeout(iter, genStack);
      CountingGenerator<Node> gen = genStack.peek();
      int k = gen.hasNext();
      if (k > 0) {
        Node child = gen.next();
        // accumulate objective function
        accu.add(objective(child));
        // and push child's node generator
        lg.log(Event.EXPAND, iter, genStack);
        genStack.push(new WrappedGenerator<Node>(gen.children(child)));
      }
      else {
        // closing this branch because there are no children (-> backtrack)
        lg.log(Event.BACKTRACK, iter, genStack);
        genStack.pop();
      }
    }
    // search complete: terminate
    lg.log(Event.TERMINATE, iter, genStack);
    return accu.get();
  }
}
