package yewpar;

// Lazy node generator interface that counts calls to the next() method.

public interface CountingGenerator<Node>
extends Generator<Node>
{
    // Returns the number of calls to generator's next() method.
    int getNextCalls();
}
