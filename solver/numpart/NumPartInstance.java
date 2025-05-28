package numpart;

// Multiway number partition instance, represented as integer array

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class NumPartInstance
{
  private int n;          // number of items (non-negative)
  private long[] s;       // 1d array of sizes (positive); sorted in dec order
  private int k;          // number of parts (>= 2) in partition
  private long solution;  // max sum of k parts; negative if not known

  // Checks invariants; returns true if and only if all invariants hold.
  public boolean valid() {
    if (s.length != n)
      return false;
    for (int i = 0; i < n; i++)
      if (s[i] <= 0)
        return false;
    for (int i = 1; i < n; i++)
      if (s[i-1] < s[i])
        return false;
    if (k < 2)
      return false;
    return true;
  }

  // Constructs instance from a list of numbers, k, and potential solution
  private NumPartInstance(List<Long> s, int k, long solution) {
    this.n = s.size();
    this.s = new long[this.n];
    for (int i = 0; i < this.s.length; i++)
      this.s[i] = s.get(i);
    this.k = k;
    this.solution = solution;
    if (!this.valid())
      throw new IllegalArgumentException();
  }

  public int n() { return n; }

  public int k() { return k; }

  public boolean hasSolution() { return (solution >= 0); }

  public long solution() { return solution; }

  public long s(int item) { return s[item]; }

  // Constructs a number partition instance from a file in the following format:
  // * the first line is the solution if known, or -1
  // * the second line is k
  // * any subsequent line is the size of an element (in decreasing order)
  // * The rest of the lines can be white space or comments (# to end of line)
  // Returns null if the file can't be read or parsed or violated invariants.
  public static NumPartInstance parse(String filename) {
    Scanner sc = null;
    try {
      sc = new Scanner(new File(filename));
      int n = 0;
      List<Long> s = new ArrayList<>();
      int k = 0;
      long solution = -1;
      int line_type = 0;
      while (sc.hasNext()) {
        // read next line
        Scanner line = new Scanner(sc.nextLine());
        // skip blank and comment lines
        if (!line.hasNext() || line.hasNext("#"))
          continue;
        switch (line_type) {
        case 0: // reading the solution
          solution = line.nextLong();
          if (solution < -1)
            throw new Exception();
          line_type++;
          break;
        case 1: // reading k
          k = line.nextInt();
          line_type++;
          break;
        case 2: // reading an item size
          long size = line.nextLong();
          s.add(size);
          break;
        default: // this line should be unreachable
          throw new Exception();
        }
      }
      sc.close();
      return new NumPartInstance(s, k, solution);
    }
    catch (Exception e) {
      if (sc != null)
        sc.close();
      return null;
    }
  }

  // For testing: reading number partition instance file
  public static void main(String args[]) throws Exception {
    System.out.println(args[0]);
    NumPartInstance np = parse(args[0]);
    System.out.println("n: " + np.n());
    System.out.println("k: " + np.k());
  }
}
