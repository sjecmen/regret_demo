package edu.umich.srg.collect;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;

import java.util.AbstractQueue;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Random;

/**
 * A random queue (i.e. a queue where inputs come out in a random order) where groups of items added
 * together (with addAllOrdered) maintain their order relationships when coming out, hence
 * constrained. The randomness is such that if you were to drain the queue, the distribution over
 * the order in which things came out would uniform over all feasible orderings. For example if one
 * added [1] and then added [2, 3] ordered, the possible outputs from draining would be [1, 2, 3],
 * [2, 1, 3], and [2, 3, 1]. Each would occur with roughly equal probability. This also means that
 * if you're just removing one element, 2 is twice as likely as 1.
 */
public class PermOrderedRandomQueue<V> extends AbstractQueue<V> implements OrderedQueue<V> {
  protected Random rand;
  protected final List<Queue<V>> queue;
  protected int size;
  private boolean picked;

  protected PermOrderedRandomQueue(Random rand) {
    this.rand = rand;
    this.queue = new ArrayList<>();
    this.size = 0;
    this.picked = false;
  }

  public static <T> PermOrderedRandomQueue<T> create(Random rand) {
    return new PermOrderedRandomQueue<>(rand);
  }

  @Override
  public boolean offer(V activity) {
    return addAllOrdered(ImmutableList.of(activity));
  }

  @Override
  public boolean addAllOrdered(Collection<? extends V> collection) {
    queue.add(new ArrayDeque<>(collection));
    size += collection.size();
    picked = false;
    return true;
  }

  private void pick() {
    int swap = rand.nextInt(size);
    int sum = 0;
    int index = -1;
    while (sum <= swap) {
      sum += queue.get(++index).size();
    }
    Collections.swap(queue, queue.size() - 1, index);
  }

  @Override
  public V poll() {
    if (queue.isEmpty()) {
      return null;
    }
    if (!picked) {
      pick();
    }
    picked = false;

    Queue<V> seq = queue.remove(queue.size() - 1);
    size--;
    V ret = seq.poll();

    if (!seq.isEmpty()) {
      queue.add(seq);
    }
    return ret;
  }

  @Override
  public V peek() {
    if (queue.isEmpty()) {
      return null;
    }
    if (!picked) {
      pick();
    }
    picked = true;
    return queue.get(queue.size() - 1).peek();
  }

  @Override
  public Iterator<V> iterator() {
    return Iterators.unmodifiableIterator(Iterables.concat(queue).iterator());
  }

  @Override
  public boolean remove(Object element) {
    for (Queue<V> a : queue) {
      if (a.remove(element)) {
        size--;
        return true;
      }
    }
    return false;
  }

  @Override
  public int size() {
    return size;
  }

}
