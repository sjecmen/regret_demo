package edu.umich.srg.collect;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Randomly ordered queue. Elements will come out in a random order independent of when they were
 * inserted. Given a collection of elements in the queue at a given time. The probability that any
 * specific element will be removed next is uniform.
 */
public class RandomQueue<E> extends AbstractQueue<E> {

  protected Random rand;
  protected List<E> elements;

  protected RandomQueue(Random seed) {
    elements = new ArrayList<>();
    rand = seed;
  }

  protected RandomQueue(Iterable<? extends E> initialElements, Random seed) {
    this(seed);
    Iterables.addAll(this, initialElements);
  }

  public static <E> RandomQueue<E> create() {
    return withSeed(new Random());
  }

  public static <E> RandomQueue<E> withSeed(Random seed) {
    return new RandomQueue<E>(seed);
  }

  public static <E> RandomQueue<E> withElements(Iterable<? extends E> initialElements) {
    return withElementsAndSeed(initialElements, new Random());
  }

  public static <E> RandomQueue<E> withElementsAndSeed(Iterable<? extends E> initialElements,
      Random seed) {
    return new RandomQueue<E>(initialElements, seed);
  }

  @Override
  public void clear() {
    elements.clear();
  }

  @Override
  public Iterator<E> iterator() {
    return elements.iterator();
  }

  @Override
  public int size() {
    return elements.size();
  }

  @Override
  public boolean offer(E element) {
    elements.add(element);
    Collections.swap(elements, rand.nextInt(size()), size() - 1);
    return true;
  }

  @Override
  public E peek() {
    return Iterables.getLast(elements, null);
  }

  @Override
  public E poll() {
    if (isEmpty()) {
      return null;
    }
    return elements.remove(size() - 1);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof RandomQueue)) {
      return false;
    }
    return elements.equals(((RandomQueue<?>) obj).elements);
  }

  @Override
  public int hashCode() {
    return elements.hashCode();
  }

  @Override
  public String toString() {
    return Lists.reverse(elements).toString();
  }

}
