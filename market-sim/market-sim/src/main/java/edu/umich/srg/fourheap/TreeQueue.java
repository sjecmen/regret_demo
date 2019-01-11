package edu.umich.srg.fourheap;

import com.google.common.collect.ForwardingCollection;

import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A priority queue that's based on a sorted set instead of Binary Heap. The constants associated
 * with most operations on this object are likely slower than the corresponding java Binary Heap
 * Priority Queue, but remove is O(log n) instead of O(n) making cancellations much faster. Like the
 * java.util PriorityQueue, the minimum element has the highest priority.
 */
public class TreeQueue<E> extends ForwardingCollection<E> implements Queue<E>, Serializable {

  private TreeSet<E> set;

  public TreeQueue() {
    set = new TreeSet<>();
  }

  public TreeQueue(Collection<? extends E> collection) {
    set = new TreeSet<>(collection);
  }

  public TreeQueue(Comparator<? super E> comp) {
    set = new TreeSet<>(comp);
  }

  public TreeQueue(SortedSet<? extends E> set) {
    set = new TreeSet<>(set);
  }

  @Override
  public E element() {
    return set.first();
  }

  @Override
  public boolean offer(E element) {
    set.add(element);
    return true;
  }

  @Override
  public E peek() {
    if (set.isEmpty()) {
      return null;
    } else {
      return set.first();
    }
  }

  @Override
  public E poll() {
    return set.pollFirst();
  }

  @Override
  public E remove() {
    if (set.isEmpty()) {
      throw new NoSuchElementException();
    } else {
      return set.pollFirst();
    }
  }

  @Override
  protected Collection<E> delegate() {
    return set;
  }

  /**
   * Returns the comparator used to order the elements in this queue, or null if this queue is
   * sorted according to the natural ordering of its elements.
   */
  public Comparator<? super E> comparator() {
    return set.comparator();
  }

  private static final long serialVersionUID = 6897819986731242769L;

}
