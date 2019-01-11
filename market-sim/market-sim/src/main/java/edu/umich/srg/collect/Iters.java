package edu.umich.srg.collect;

import com.google.common.collect.PeekingIterator;

import java.util.Iterator;
import java.util.Queue;

public final class Iters {

  /** Repeats every element in an iterable a given number of times. */
  public static <T> Iterator<T> repeat(Iterator<T> iterator, long num) {
    PeekingIterator<T> peekable = com.google.common.collect.Iterators.peekingIterator(iterator);
    return new Iterator<T>() {

      long numLeft = num;

      @Override
      public boolean hasNext() {
        return peekable.hasNext();
      }

      @Override
      public T next() {
        T item = peekable.peek();
        if (--numLeft == 0) {
          peekable.next();
          numLeft = num;
        }
        return item;
      }

    };
  }

  /** Standard list hashcode for an iterator. */
  public static <T> int hashCode(Iterator<T> iter) {
    int hashCode = 1;
    while (iter.hasNext()) {
      T next = iter.next();
      hashCode = 31 * hashCode + (next == null ? 0 : next.hashCode());
    }
    return hashCode;
  }

  /** Standard hashcode for an iterable. */
  public static <T> int hashCode(Iterable<T> iter) {
    return hashCode(iter.iterator());
  }

  /** Returns an iterator that consumes the supplied queue in pop order. */
  public static <T> Iterator<T> consumeQueue(Queue<T> queue) {
    return new Iterator<T>() {

      @Override
      public boolean hasNext() {
        return !queue.isEmpty();
      }

      @Override
      public T next() {
        return queue.remove();
      }

    };
  }

  // TODO There has to be a better way
  /**
   * This is a wrapper iterator that will print any exception stack traces that may be gobbled up by
   * a thread pool.
   */
  public static <T> Iterator<T> printExceptions(Iterator<T> iterator) {
    return new Iterator<T>() {
      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      public T next() {
        try {
          return iterator.next();
        } catch (Exception e) {
          e.printStackTrace();
          throw e;
        }
      }
    };
  }

  private Iters() {} // Unconstructable

}
