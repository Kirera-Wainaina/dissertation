package numpart;

// Multiway Number Partitioning Problem
//
// V3o: ordered (o) - simple ordering heuristic (of greedy algorithm)
//      simple bulk pruning & shortcircuting (3)
//      NB: This version essentially implements the Complete Greedy Algorithm,
//          with an optimisation avoids constructing nodes that would be pruned
//          immediately, and an optimisation that shortcircuits when no
//          improvement is possible.
//
// compile: javac -cp .. NumPartV3o.java
// run:     java -cp .. numpart.NumPartV3o PROBLEM_FILE [OPTIONS]

import java.util.Arrays;
import java.util.Comparator;
import yewpar.OptInstance;
import yewpar.Generator;
import yewpar.logger.TimeoutException;
import yewpar.logger.Logger;
import yewpar.logger.NoLogger;
import yewpar.logger.CountLogger;
import yewpar.logger.HistLogger;
import yewpar.logger.TracePredicate;
import yewpar.logger.Utils;
import numpart.NumPartInstance;

public class NumPartV3o extends OptInstance<NumPartV3o.Node, Long>
{
  final NumPartInstance np;  // problem instance (numbers, k)

  /////////////////////////////////////////////////////////////////////////////
  // Search tree node: representation of current partition and remaining nums.
  // Numbers are represented as non-negative integers < np.n().
  // Instances of this class are immutable.
  static class Node
  {
    final NumPartInstance np;  // number partition problem instance
    final int[] part;          // current partition
    final long[] sum;          // sums of current partition
    final long rsum;           // sum of remaining numbers
    final Integer[] pi;        // permutation of partition, sorted desc by sums
    final long msum;           // current maximal sum (to be minimised)

    // Invariants:
    // * part.length <= np.n()
    // * 0 <= part[i] < np.k()
    // * sum.length == np.k()
    // * sum[i] == sum {np.s(j) | part[j] == i}
    // * rsum == sum {np.s(j) | j >= part.length}
    // * pi.length == np.k()
    // * pi is a permutation such that sum[pi[i]] is a decreasing sequence
    // * msum == sum[pi[0]]

    Node(NumPartInstance np,
         int[] part,
         long[] sum,
         long rsum,
         Integer[] pi) {
      this.np = np;
      this.part = part;
      this.sum = sum;
      this.rsum = rsum;
      this.pi = pi;
      this.msum = sum[pi[0]];
    }

    // root node factory
    static Node mkRoot(NumPartInstance np) {
      int[] part = new int[0];
      long rsum = 0;
      for (int j = 0; j < np.n(); j++)
        rsum += np.s(j);
      long[] sum = new long[np.k()];
      Arrays.fill(sum, 0);
      Integer[] pi = new Integer[np.k()];
      Arrays.setAll(pi, i -> i++);
      // return a new root node
      return new Node(np, part, sum, rsum, pi);
    }
  }

  /////////////////////////////////////////////////////////////////////////////
  // Lazy node generator: dictates how to traverse the search tree
  static class NodeGenerator implements Generator<Node>
  {
    // immutable fields
    final NumPartInstance np;  // Number partition problem instance
    final Node parent;         // NodeGen generates children of this parent node
    final int level;           // np.s(level) next number added to parent.part;
                               // if level == np.n() there is no number to add

    // mutable fields
    int k;                     // number of values for part[level] to take

    NodeGenerator(Node parent) {
      this.np = parent.np;
      this.parent = parent;
      this.level = parent.part.length;
      if (level == np.n())
        this.k = 0;
      else
        this.k = np.k();
    }

    public int hasNext() { return k; }

    public Node next() {
      if (k == 0)
        return null;

      // there are more nodes: move k
      k--;

      // construct next node:
      // (1) copy parent partition and add level-th number, x, to l-th part,
      //     where l is pi[k].
      //     NB: pi is sorted in decreasing order by sums. As the iteration
      //         over pi is backwards, x will first be added to the part with
      //         the least sum (which is what the greedy algorithm does).
      long x = np.s(level);
      int l = parent.pi[k];
      int[] part = Arrays.copyOf(parent.part, level + 1);
      part[level] = l;

      // (2) copy parent sums and move level-th number, x, from rsum to l-th sum
      long rsum = parent.rsum - x;
      long[] sum = Arrays.copyOf(parent.sum,  parent.sum.length);
      sum[l] += x;

      // (3) copy parent permutation pi & sort its indices in desc order by sum
      Integer[] pi = Arrays.copyOf(parent.pi, parent.pi.length);
      Comparator<Integer> bySum = Comparator.comparingLong(i -> sum[i]);
      Arrays.sort(pi, bySum.reversed());

      // construct new Node
      return new Node(np, part, sum, rsum, pi);
    }

    public Generator<Node> children(Node node) {
      return new NodeGenerator(node);
    }

    // root generator factory
    static Generator<Node> mkRoot(Node root) {
      return new NodeGenerator(root);
    }
  }

  /////////////////////////////////////////////////////////////////////////////
  // Objective function to be maximised: returns the inverse current max sum
  public Long objective(Node current) {
    // return the smallest possible negative value for incomplete partitions
      if (current.part.length < current.np.n())
      return Long.MIN_VALUE;
    return -current.msum;
  }

  /////////////////////////////////////////////////////////////////////////////
  // Weak pruning condition
  public int prune(Node current, Node incumbent) {
    if (incumbent.part.length == incumbent.np.n())
      if (current.msum >= incumbent.msum)
        return -1;  // bulk prune: current partial solution can't beat incumbent
                    //             none of its later siblings can either
                    //             due to iteration in order of increasing sums
    return 0;
  }

  /////////////////////////////////////////////////////////////////////////////
  // Boilerplate for setting up and running instances

  // private instance constructor
  private NumPartV3o(Node root, Generator<Node> gen0) {
    super(root, gen0);
    this.np = root.np;
  }

  // instance factory; returns null if np is null.
  public static NumPartV3o mkNumPart(NumPartInstance np) {
    if (np == null)
      return null;
    // construct root node and root generator
    Node root = Node.mkRoot(np);
    Generator<Node> gen0 = NodeGenerator.mkRoot(root);
    // return new instance
    return new NumPartV3o(root, gen0);
  }

  // main function
  public static void main(String[] args) {
    try {
      NumPartInstance np = NumPartInstance.parse(args[0]);
      NumPartV3o inst = mkNumPart(np);
      if (inst == null)
        throw new Exception();
      System.out.println("java NumPart " + args[0]);
      // Calculate the best-case max sum and use its negation to shortcircuit
      Double bestCase = Math.ceil(((double)inst.root.rsum) / inst.np.k());
      Long greatest = -bestCase.longValue();
      System.out.println("Shortcircuit objective: " + greatest);
      TracePredicate tp = Utils.mkTracePredicate(args);
      Logger<Node> lg = new NoLogger<>();
      if (Utils.parseOptCountLogger(args)) { lg = new CountLogger<>(tp); }
      if (Utils.parseOptHistLogger(args))  { lg = new HistLogger<>(tp); }
      lg.setTimeout(Utils.parseOptTimeout(args));
      lg.setTimeoutMillis(Utils.parseOptTimeoutMillis(args));
      long t0 = System.nanoTime();
      try {
        Node x = inst.searchUntil(greatest, lg);
        long t1 = System.nanoTime();
        System.out.println("Partition: " + Arrays.toString(x.part));
        System.out.println("Sums: " + Arrays.toString(x.sum));
        System.out.println("Pi: " + Arrays.toString(x.pi));
        System.out.print("MaxSum: " + x.msum);
        if (np.hasSolution() && np.solution() == x.msum)
          System.out.println(" OK");
        else if (np.hasSolution() && np.solution() != x.msum)
          System.out.println(" WRONG");
        else
          System.out.println();
        System.out.println("Time: " + ((t1 - t0) / 1000000) + "ms");
      }
      catch (TimeoutException e) {
        long t1 = System.nanoTime();
        System.out.println("Timeout: " + ((t1 - t0) / 1000000) + "ms");
      }
    }
    catch (Exception e) {
      System.out.println("Usage: java NumPart PROBLEM_FILE [OPTIONS]");
    }
  }
}
