package edu.umich.srg.collect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.google.common.math.LongMath;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import edu.umich.srg.testing.Repeat;
import edu.umich.srg.testing.RepeatRule;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;

public class RandomKeyedQueueTest {

  private static final Random rand = new Random();
  private RandomPriorityQueue<Integer, Integer> queue;

  @Rule
  public RepeatRule repeatRule = new RepeatRule();

  @Before
  public void setup() {
    queue = RandomPriorityQueue.create(rand);
  }

  @Test
  public void basicTest() {
    assertTrue(queue.isEmpty());
    assertEquals(0, queue.size());

    queue.add(1, 1);
    assertFalse(queue.isEmpty());
    assertEquals(1, queue.size());
    assertEquals(1, (int) queue.peek().getValue());

    assertEquals(1, (int) queue.remove().getValue());
    assertTrue(queue.isEmpty());
    assertEquals(0, queue.size());

    queue.add(3, 3);
    queue.add(2, 2);
    assertEquals(2, (int) queue.peek().getValue());
    assertEquals(2, queue.size());

    assertEquals(2, (int) queue.poll().getValue());
    assertEquals(3, (int) queue.poll().getValue());
    assertTrue(queue.isEmpty());
    assertEquals(0, queue.size());
  }

  /** Test that items added individually come out randomly ordered */
  @Test
  public void randomOrderingTest() {
    int n = 3;
    Set<Integer> listHashes = Sets.newHashSet();

    for (int i = 0; i < 1000; ++i) {
      RandomPriorityQueue<Integer, Integer> queue = RandomPriorityQueue.create(rand);
      for (int j = 0; j < n; ++j)
        queue.add(0, j);
      Builder<Integer> builder = ImmutableList.builder();
      while (!queue.isEmpty())
        builder.add(queue.poll().getValue());
      listHashes.add(builder.build().hashCode());
    }

    assertEquals(LongMath.factorial(n), listHashes.size());
  }

  /** Test that items added together always come out in order */
  @Test
  @Repeat(1000)
  public void addAllTest() {
    SortedSet<Integer> list =
        ContiguousSet.create(Range.closedOpen(0, 10), DiscreteDomain.integers());
    RandomPriorityQueue<Integer, Integer> queue = RandomPriorityQueue.create(rand);
    queue.addAllOrdered(0, list);
    assertTrue(Iterators.elementsEqual(list.iterator(),
        Streams.stream(Iters.consumeQueue(queue)).map(x -> x.getValue()).iterator()));
  }

  /** Test that mixed keys get ordered appropriately */
  @Test
  public void mixedTest() {
    Set<ImmutableList<Entry<Integer, Integer>>> listHashes = new HashSet<>();

    for (int i = 0; i < 1000; ++i) {
      RandomPriorityQueue<Integer, Integer> queue = RandomPriorityQueue.create(rand);
      queue.add(0, 0);
      queue.addAllOrdered(0, ImmutableList.of(1, 2));
      listHashes.add(ImmutableList.copyOf(Iters.consumeQueue(queue)));
    }

    // Three ways to order them if 1 and 2 must come in order
    assertEquals(3, listHashes.size());
  }

  @Test
  public void emptyPeekTest() {
    assertEquals(null, queue.peek());
  }

  @Test
  public void emptyPollTest() {
    assertEquals(null, queue.poll());
  }

  @Test(expected = NoSuchElementException.class)
  public void emptyElementTest() {
    queue.element();
  }

  @Test(expected = NoSuchElementException.class)
  public void emptyRemoveTest() {
    queue.remove();
  }

  @Test
  public void clearTest() {
    queue.addAllOrdered(0, ImmutableList.of(1, 2, 3));
    assertFalse(queue.isEmpty());
    queue.clear();
    assertTrue(queue.isEmpty());
  }

  @Test
  public void pollTest() {
    List<Integer> list = Lists.newArrayList(1, 2, 3);
    for (int i = 0; i < 1000; ++i) {
      Collections.shuffle(list);
      for (int j : list)
        queue.add(j, j);

      // Check that poll will return activities in correct order & update size
      assertEquals(1, (int) queue.poll().getValue());
      assertEquals(2, queue.size());
      assertEquals(2, (int) queue.poll().getValue());
      assertEquals(1, queue.size());
      assertEquals(3, (int) queue.poll().getValue());
      assertTrue(queue.isEmpty());
      assertEquals(null, queue.poll());
    }
  }

  @Test
  public void iteratorTest() {
    List<Integer> acts = ImmutableList.of(1, 2, 3);

    queue.addAllOrdered(0, acts);
    for (Entry<Integer, Integer> a : queue)
      assertTrue(acts.contains(a.getValue()));

    assertEquals(3, queue.size());
  }

}
