package yewpar;

// Interface for objects that can accumulate values of type T.

public interface Accumulable<T>
{
  // Returns the current value of the accumulator.
  T get();

  // Adds x to the accumulator.
  // Addition must satisfy the laws of a commutative monoid, in particular:
  // * Commutativity: { o.add(x); o.add(y); } <=> { o.add(y); o.add(x); }
  void add(T x);
}
