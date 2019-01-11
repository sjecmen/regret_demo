package edu.umich.srg.collect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import edu.umich.srg.testing.Repeat;
import edu.umich.srg.testing.RepeatRule;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class UPOrderedRandomQueueTest {

  private static Random rand;

  @Rule
  public RepeatRule repeatRule = new RepeatRule();

  @BeforeClass
  public static void setup() {
    rand = new Random();
  }

  @Test
  public void emptyConstructorTest() {
    PermOrderedRandomQueue<Integer> a = PermOrderedRandomQueue.create(rand);
    assertTrue(a.isEmpty());
  }

  @Test
  public void collectionConstructorTest() {
    Collection<Integer> numbers = randomNumbers(10);
    PermOrderedRandomQueue<Integer> a = PermOrderedRandomQueue.create(rand);
    a.addAll(numbers);
    for (int i : numbers)
      assertTrue(a.contains(i));
  }

  @Test
  public void randomSeedTest() {
    long seed = rand.nextLong();
    Collection<Integer> numbers = randomNumbers(10);
    PermOrderedRandomQueue<Integer> a = PermOrderedRandomQueue.create(new Random(seed));
    a.addAll(numbers);
    PermOrderedRandomQueue<Integer> b = PermOrderedRandomQueue.create(new Random(seed));
    b.addAll(numbers);

    Iterator<Integer> ita = a.iterator();
    Iterator<Integer> itb = b.iterator();
    while (ita.hasNext())
      assertTrue(ita.next().equals(itb.next()));

    PermOrderedRandomQueue<Integer> c = PermOrderedRandomQueue.create(new Random(seed));
    c.addAll(numbers);

    itb = b.iterator();
    Iterator<Integer> itc = c.iterator();
    while (itc.hasNext())
      assertTrue(itb.next().equals(itc.next()));
  }

  @Test
  public void clearTest() {
    PermOrderedRandomQueue<Integer> a = PermOrderedRandomQueue.create(rand);
    a.addAll(randomNumbers(10));
    assertFalse(a.isEmpty());
    a.clear();
    assertTrue(a.isEmpty());
  }

  @Test
  public void removeTest() {
    PermOrderedRandomQueue<Integer> a = PermOrderedRandomQueue.create(rand);
    a.addAll(Ints.asList(1, 2, 3, 4, 5));
    assertTrue(a.contains(4));
    a.remove(4);
    assertFalse(a.contains(4));
    assertTrue(a.containsAll(Ints.asList(1, 2, 3, 5)));
  }

  @Test
  /** This tests that if you input 1000 random numbers, they come out in a different order */
  public void pollPermutationTest() {
    // Note, this test could fail due to inconceivably small random chance ~1/1000!
    Collection<Integer> numbers = randomNumbers(1000);
    PermOrderedRandomQueue<Integer> a = PermOrderedRandomQueue.create(rand);
    a.addAll(numbers);

    Iterator<Integer> ita = numbers.iterator();
    while (!a.isEmpty())
      if (!ita.next().equals(a.poll()))
        return;
    fail();
  }

  @Test
  /** Tests that sometimes the list isn't permuted */
  public void offerNonpermutationTest() {
    // Note, this test could fail due to inconceivably small random chance as well. 1/2^10000 ~
    // 1/1000^1000
    int allResponses = 0; // This is a bit set
    for (int i = 0; i < 10000; i++) {
      PermOrderedRandomQueue<Integer> a = PermOrderedRandomQueue.create(rand);
      a.add(1);
      a.add(2);
      allResponses |= a.peek();
    }
    assertEquals(3, allResponses);
  }

  @Test
  @Repeat(100)
  /** Tests that addAll doesn't get permuted */
  public void addAllOrderedTest() {
    Collection<Integer> numbers = randomNumbers(1000);
    PermOrderedRandomQueue<Integer> a = PermOrderedRandomQueue.create(rand);
    a.addAllOrdered(numbers);

    Iterator<Integer> ita = numbers.iterator();
    while (!a.isEmpty())
      if (!ita.next().equals(a.poll()))
        fail();
  }

  @Test
  /** Tests that addAll doesn't get permuted */
  public void addAllOrderedPermutationsTest() {
    // This can fail with probability (2/3)^1000
    Set<List<Integer>> allPermutations = new HashSet<>();
    for (int i = 0; i < 1000; ++i) {
      PermOrderedRandomQueue<Integer> a = PermOrderedRandomQueue.create(rand);
      a.add(1);
      a.addAllOrdered(Ints.asList(2, 3));

      allPermutations.add(ImmutableList.copyOf(Iters.consumeQueue(a)));
    }
    assertEquals(3, allPermutations.size());
  }

  private static Collection<Integer> randomNumbers(int size) {
    Collection<Integer> numbers = Lists.newArrayListWithCapacity(size);
    for (int i = 0; i < size; i++)
      numbers.add(rand.nextInt());
    return numbers;
  }

}
