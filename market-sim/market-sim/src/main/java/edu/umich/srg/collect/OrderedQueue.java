package edu.umich.srg.collect;

import java.util.Collection;
import java.util.Queue;

/**
 * A special form of queue that allows adding a collection of elements that are guaranteed to remain
 * in the specified order in the queue.
 */
public interface OrderedQueue<E> extends Queue<E> {

  boolean addAllOrdered(Collection<? extends E> elements);

}
