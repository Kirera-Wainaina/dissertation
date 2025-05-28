package yewpar;

// Wrapper around a node generator that counts calls to the next() method.
// The wrapper implements the CountingGenerator interface, a sub-interface
// of the Generator interface, so the wrapped generator can be used
// in place of the unwrapped one.

public class WrappedGenerator<Node>
implements CountingGenerator<Node>
{
    private final Generator<Node> gen;  // wrapped node generator
    private int nextCalls;              // number of calls to gen's next()

    // Constructs a new wrapped generator, initialising calls to 0.
    public WrappedGenerator(Generator<Node> gen) {
	this.gen = gen;
	this.nextCalls = 0;
    }

    // Returns the number of nodes left to iterate over; delegates to gen.
    public int hasNext() {
        return gen.hasNext();
    }

    // Returns the next node, or null; increments count and delegates to gen.
    public Node next() {
        nextCalls++;
        return gen.next();
    }

    // Returns a new generator for the children of node; delegates to gen.
    // Note that the new generator is not wrapped.
    public Generator<Node> children(Node node) {
        return gen.children(node);
    }

    // Returns the number of calls to gen's next() method.
    public int getNextCalls() {
        return nextCalls;
    }
}
