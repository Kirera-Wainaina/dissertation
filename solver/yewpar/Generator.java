package yewpar;

// Lazy node generator interface.

public interface Generator<Node>
{
  // Returns the number of nodes left to iterate over; the number returned
  // may be an undercount but must be zero only if there are no more nodes.
  // Must not change the state of the node generator.
  int hasNext();

  // Returns the next node, or null if there is no next node.
  // Changes the state of the node generator.
  Node next();

  // Returns a new generator for the children of the given search tree node.
  // Must not change the state of the node generator.
  Generator<Node> children(Node node);
}
