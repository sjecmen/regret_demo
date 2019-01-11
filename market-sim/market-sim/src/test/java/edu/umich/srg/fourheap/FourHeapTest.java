package edu.umich.srg.fourheap;

import static edu.umich.srg.fourheap.Order.OrderType.BUY;
import static edu.umich.srg.fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.collect.Iterables;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Queue;
import java.util.Random;

public class FourHeapTest {

  private final static Random rand = new Random();
  private FourHeap<Integer> fh;

  @Before
  public void setup() {
    fh = new FourHeap<>();
  }

  @Test
  public void heapOrderTest() {
    Order<Integer> b1, b2, b3, s1, s2, s3;

    b1 = new Order<>(BUY, 5, 3, 5);
    b2 = new Order<>(BUY, 10, 3, 6);
    b3 = new Order<>(BUY, 5, 3, 4);

    fh.buyUnmatched.offer(b1);
    fh.buyUnmatched.offer(b2);
    assertEquals(b2, fh.buyUnmatched.poll());
    fh.buyUnmatched.offer(b3);
    assertEquals(b3, fh.buyUnmatched.poll());

    fh.buyMatched.offer(b1);
    fh.buyMatched.offer(b2);
    assertEquals(b1, fh.buyMatched.peek());
    fh.buyMatched.offer(b3);
    assertEquals(b1, fh.buyMatched.poll());

    s1 = new Order<>(SELL, 5, 3, 5);
    s2 = new Order<>(SELL, 10, 3, 6);
    s3 = new Order<>(SELL, 5, 3, 4);

    fh.sellUnmatched.offer(s1);
    fh.sellUnmatched.offer(s2);
    assertEquals(s1, fh.sellUnmatched.peek());
    fh.sellUnmatched.offer(s3);
    assertEquals(s3, fh.sellUnmatched.poll());

    fh.sellMatched.offer(s1);
    fh.sellMatched.offer(s2);
    assertEquals(s2, fh.sellMatched.poll());
    fh.sellMatched.offer(s3);
    assertEquals(s1, fh.sellMatched.poll());
  }

  @Test
  public void insertOneBuyTest() {
    fh.submit(BUY, 5, 3);

    assertTrue(fh.buyMatched.isEmpty());
    assertFalse(fh.buyUnmatched.isEmpty());
    assertTrue(fh.sellMatched.isEmpty());
    assertTrue(fh.sellUnmatched.isEmpty());
    assertEquals((Integer) 5, fh.bidQuote());
    assertEquals(null, fh.askQuote());
    assertEquals(3, fh.getNumberOfUnits());
    assertEquals(3, fh.getBidDepth());
    assertEquals(0, fh.getAskDepth());
    assertInvariants(fh);
  }

  @Test
  public void insertOneSellTest() {
    fh.submit(SELL, 5, 3);

    assertTrue(fh.buyMatched.isEmpty());
    assertTrue(fh.buyUnmatched.isEmpty());
    assertTrue(fh.sellMatched.isEmpty());
    assertFalse(fh.sellUnmatched.isEmpty());
    assertEquals(null, fh.bidQuote());
    assertEquals((Integer) 5, fh.askQuote());
    assertEquals(3, fh.getNumberOfUnits());
    assertEquals(0, fh.getBidDepth());
    assertEquals(3, fh.getAskDepth());
    assertInvariants(fh);
  }

  @Test
  public void matchTest1() {
    fh.submit(SELL, 5, 3);
    fh.submit(BUY, 7, 3);

    assertFalse(fh.buyMatched.isEmpty());
    assertTrue(fh.buyUnmatched.isEmpty());
    assertFalse(fh.sellMatched.isEmpty());
    assertTrue(fh.sellUnmatched.isEmpty());
    assertEquals((Integer) 5, fh.bidQuote());
    assertEquals((Integer) 7, fh.askQuote());
    assertEquals(6, fh.getNumberOfUnits());
    assertEquals(3, fh.getBidDepth());
    assertEquals(3, fh.getAskDepth());
    assertInvariants(fh);
  }

  @Test
  public void matchTest2() {
    fh.submit(SELL, 5, 5);
    fh.submit(BUY, 7, 3);

    assertFalse(fh.buyMatched.isEmpty());
    assertTrue(fh.buyUnmatched.isEmpty());
    assertFalse(fh.sellMatched.isEmpty());
    assertFalse(fh.sellUnmatched.isEmpty());
    assertEquals((Integer) 5, fh.bidQuote());
    assertEquals((Integer) 5, fh.askQuote());
    assertEquals(8, fh.getNumberOfUnits());
    assertEquals(3, fh.getBidDepth());
    assertEquals(5, fh.getAskDepth());
    assertInvariants(fh);
  }

  @Test
  public void matchTest3() {
    fh.submit(SELL, 5, 3);
    fh.submit(BUY, 7, 5);

    assertFalse(fh.buyMatched.isEmpty());
    assertFalse(fh.buyUnmatched.isEmpty());
    assertFalse(fh.sellMatched.isEmpty());
    assertTrue(fh.sellUnmatched.isEmpty());
    assertEquals((Integer) 7, fh.bidQuote());
    assertEquals((Integer) 7, fh.askQuote());
    assertEquals(8, fh.getNumberOfUnits());
    assertEquals(5, fh.getBidDepth());
    assertEquals(3, fh.getAskDepth());
    assertInvariants(fh);
  }

  @Test
  public void insertMatchedTest1() {
    fh.submit(SELL, 5, 3);
    fh.submit(BUY, 7, 3);
    fh.submit(BUY, 4, 1);
    assertEquals((Integer) 5, fh.bidQuote());
    assertEquals((Integer) 7, fh.askQuote());
    assertEquals(7, fh.getNumberOfUnits());
    assertEquals(4, fh.getBidDepth());
    assertEquals(3, fh.getAskDepth());
    assertInvariants(fh);
  }

  @Test
  public void insertMatchedTest2() {
    fh.submit(SELL, 5, 3);
    fh.submit(BUY, 7, 3);
    fh.submit(BUY, 6, 1);
    assertEquals((Integer) 6, fh.bidQuote());
    assertEquals((Integer) 7, fh.askQuote());
    assertEquals(7, fh.getNumberOfUnits());
    assertEquals(4, fh.getBidDepth());
    assertEquals(3, fh.getAskDepth());
    assertInvariants(fh);
  }

  @Test
  public void insertMatchedTest3() {
    fh.submit(SELL, 5, 3);
    fh.submit(BUY, 7, 3);
    fh.submit(BUY, 8, 1);
    assertEquals((Integer) 7, fh.bidQuote());
    assertEquals((Integer) 7, fh.askQuote());
    assertEquals(7, fh.getNumberOfUnits());
    assertEquals(4, fh.getBidDepth());
    assertEquals(3, fh.getAskDepth());
    assertInvariants(fh);
  }

  @Test
  public void withdrawOneBuyTest() {
    Order<Integer> o = fh.submit(BUY, 5, 3);
    fh.withdraw(o, 2);

    assertEquals(1, o.unmatchedQuantity);
    assertTrue(fh.buyMatched.isEmpty());
    assertFalse(fh.buyUnmatched.isEmpty());
    assertTrue(fh.sellMatched.isEmpty());
    assertTrue(fh.sellUnmatched.isEmpty());
    assertEquals((Integer) 5, fh.bidQuote());
    assertEquals(null, fh.askQuote());
    assertEquals(1, fh.getNumberOfUnits());
    assertEquals(1, fh.getBidDepth());
    assertEquals(0, fh.getAskDepth());
    assertInvariants(fh);

    fh.withdraw(o);

    assertFalse(fh.contains(o));
    assertTrue(fh.buyMatched.isEmpty());
    assertTrue(fh.buyUnmatched.isEmpty());
    assertTrue(fh.sellMatched.isEmpty());
    assertTrue(fh.sellUnmatched.isEmpty());
    assertEquals(null, fh.bidQuote());
    assertEquals(null, fh.askQuote());
    assertEquals(0, fh.getNumberOfUnits());
    assertEquals(0, fh.getBidDepth());
    assertEquals(0, fh.getAskDepth());
    assertInvariants(fh);
  }

  @Test
  public void withdrawOneSellTest() {
    Order<Integer> o = fh.submit(SELL, 5, 3);
    fh.withdraw(o, 2);

    assertEquals(1, o.unmatchedQuantity);
    assertTrue(fh.buyMatched.isEmpty());
    assertTrue(fh.buyUnmatched.isEmpty());
    assertTrue(fh.sellMatched.isEmpty());
    assertFalse(fh.sellUnmatched.isEmpty());
    assertEquals(null, fh.bidQuote());
    assertEquals((Integer) 5, fh.askQuote());
    assertEquals(1, fh.getNumberOfUnits());
    assertEquals(0, fh.getBidDepth());
    assertEquals(1, fh.getAskDepth());
    assertInvariants(fh);

    fh.withdraw(o);

    assertFalse(fh.contains(o));
    assertTrue(fh.buyMatched.isEmpty());
    assertTrue(fh.buyUnmatched.isEmpty());
    assertTrue(fh.sellMatched.isEmpty());
    assertTrue(fh.sellUnmatched.isEmpty());
    assertEquals(null, fh.bidQuote());
    assertEquals(null, fh.askQuote());
    assertEquals(0, fh.getNumberOfUnits());
    assertEquals(0, fh.getBidDepth());
    assertEquals(0, fh.getAskDepth());
    assertInvariants(fh);
  }

  @Test
  public void withdrawMatchTest1() {
    Order<Integer> os = fh.submit(SELL, 5, 3);
    Order<Integer> ob = fh.submit(BUY, 7, 3);
    fh.withdraw(ob, 2);

    assertFalse(fh.buyMatched.isEmpty());
    assertTrue(fh.buyUnmatched.isEmpty());
    assertFalse(fh.sellMatched.isEmpty());
    assertFalse(fh.sellUnmatched.isEmpty());
    assertEquals((Integer) 5, fh.bidQuote());
    assertEquals((Integer) 5, fh.askQuote());
    assertEquals(4, fh.getNumberOfUnits());
    assertEquals(1, fh.getBidDepth());
    assertEquals(3, fh.getAskDepth());
    assertInvariants(fh);

    fh.withdraw(os);

    assertTrue(fh.buyMatched.isEmpty());
    assertFalse(fh.buyUnmatched.isEmpty());
    assertTrue(fh.sellMatched.isEmpty());
    assertTrue(fh.sellUnmatched.isEmpty());
    assertEquals((Integer) 7, fh.bidQuote());
    assertEquals(null, fh.askQuote());
    assertEquals(1, fh.getNumberOfUnits());
    assertEquals(1, fh.getBidDepth());
    assertEquals(0, fh.getAskDepth());
    assertInvariants(fh);
  }

  @Test
  public void withdrawMatchTest2() {
    Order<Integer> ob = fh.submit(BUY, 7, 3);
    Order<Integer> os = fh.submit(SELL, 5, 5);
    fh.withdraw(os, 3);

    assertFalse(fh.buyMatched.isEmpty());
    assertFalse(fh.buyUnmatched.isEmpty());
    assertFalse(fh.sellMatched.isEmpty());
    assertTrue(fh.sellUnmatched.isEmpty());
    assertEquals((Integer) 7, fh.bidQuote());
    assertEquals((Integer) 7, fh.askQuote());
    assertEquals(5, fh.getNumberOfUnits());
    assertEquals(3, fh.getBidDepth());
    assertEquals(2, fh.getAskDepth());
    assertInvariants(fh);

    fh.withdraw(ob);

    assertTrue(fh.buyMatched.isEmpty());
    assertTrue(fh.buyUnmatched.isEmpty());
    assertTrue(fh.sellMatched.isEmpty());
    assertFalse(fh.sellUnmatched.isEmpty());
    assertEquals(null, fh.bidQuote());
    assertEquals((Integer) 5, fh.askQuote());
    assertEquals(2, fh.getNumberOfUnits());
    assertEquals(0, fh.getBidDepth());
    assertEquals(2, fh.getAskDepth());
    assertInvariants(fh);
  }

  @Test
  public void withdrawMatchTest3() {
    fh.submit(SELL, 5, 3);
    Order<Integer> ob = fh.submit(BUY, 7, 5);
    fh.withdraw(ob, 4);

    assertFalse(fh.buyMatched.isEmpty());
    assertTrue(fh.buyUnmatched.isEmpty());
    assertFalse(fh.sellMatched.isEmpty());
    assertFalse(fh.sellUnmatched.isEmpty());
    assertEquals((Integer) 5, fh.bidQuote());
    assertEquals((Integer) 5, fh.askQuote());
    assertEquals(4, fh.getNumberOfUnits());
    assertEquals(1, fh.getBidDepth());
    assertEquals(3, fh.getAskDepth());
    assertInvariants(fh);
  }

  /**
   * Test that withdrawing with orders waiting to get matched actually works appropriately
   */
  @Test
  public void withdrawWithWaitingOrders1() {
    Order<Integer> o = fh.submit(BUY, 4, 3);
    fh.submit(SELL, 1, 3);
    fh.submit(SELL, 2, 2);
    fh.submit(BUY, 3, 4);
    assertInvariants(fh);
    fh.withdraw(o);
    assertInvariants(fh);
    fh.clear();
    assertInvariants(fh);
  }

  @Test
  public void withdrawWithWaitingOrders2() {
    Order<Integer> o = fh.submit(SELL, 1, 3);
    fh.submit(BUY, 4, 3);
    fh.submit(BUY, 3, 2);
    fh.submit(SELL, 2, 4);
    assertInvariants(fh);
    fh.withdraw(o);
    assertInvariants(fh);
    fh.clear();
    assertInvariants(fh);
  }

  /**
   * Test a strange edge case with withdrawing orders, where quantity may get misinterpreted.
   */
  @Test
  public void strangeWithdrawEdgeCase1() {
    fh.submit(BUY, 4, 3);
    Order<Integer> o = fh.submit(SELL, 1, 3);
    fh.submit(SELL, 2, 2);
    fh.submit(BUY, 3, 4);
    assertInvariants(fh);
    fh.withdraw(o);
    assertInvariants(fh);
    fh.clear();
    assertInvariants(fh);
  }

  @Test
  public void strangeWithdrawEdgeCase2() {
    fh.submit(SELL, 1, 3);
    Order<Integer> o = fh.submit(BUY, 4, 3);
    fh.submit(BUY, 3, 2);
    fh.submit(SELL, 2, 4);
    assertInvariants(fh);
    fh.withdraw(o);
    assertInvariants(fh);
    fh.clear();
    assertInvariants(fh);
  }

  @Test
  public void emptyClearTest() {
    fh.submit(SELL, 7, 3);
    fh.submit(BUY, 5, 3);
    assertTrue(fh.clear().isEmpty());
  }

  @Test
  public void clearTest() {
    Order<Integer> os = fh.submit(SELL, 5, 2);
    Order<Integer> ob = fh.submit(BUY, 7, 3);
    Collection<MatchedOrders<Integer>> transactions = fh.clear();

    assertEquals(1, transactions.size());
    MatchedOrders<Integer> trans = Iterables.getOnlyElement(transactions);
    assertEquals(os, trans.getSell());
    assertEquals(ob, trans.getBuy());
    assertEquals(2, trans.getQuantity());
    assertEquals(1, ob.unmatchedQuantity);
    assertEquals(1, fh.getNumberOfUnits());
    assertEquals(1, fh.getBidDepth());
    assertEquals(0, fh.getAskDepth());
    assertFalse(fh.contains(os));
    assertTrue(fh.contains(ob));
    assertInvariants(fh);
  }

  @Test
  public void multiOrderClearTest() {
    Order<Integer> os = fh.submit(SELL, 5, 3);
    fh.submit(SELL, 6, 2);
    Order<Integer> ob = fh.submit(BUY, 7, 4);
    Collection<MatchedOrders<Integer>> transactions = fh.clear();

    assertEquals(2, transactions.size());
    assertInvariants(fh);
    assertEquals(1, fh.getNumberOfUnits());
    assertEquals(0, fh.getBidDepth());
    assertEquals(1, fh.getAskDepth());
    assertFalse(fh.contains(os));
    assertFalse(fh.contains(ob));

    boolean one = false, three = false;
    for (MatchedOrders<Integer> trans : transactions) {
      switch (trans.getQuantity()) {
        case 1:
          assertEquals(ob, trans.getBuy());
          assertNotEquals(os, trans.getSell());
          one = true;
          break;
        case 3:
          assertEquals(os, trans.getSell());
          assertEquals(ob, trans.getBuy());
          three = true;
          break;
        default:
          fail("Incorrect transaction quantities");
      }
    }
    assertTrue(one);
    assertTrue(three);
  }

  @Test
  public void containsTest() {
    Order<Integer> ob = fh.submit(BUY, 5, 1);
    assertTrue(fh.contains(ob));
    Order<Integer> os = fh.submit(SELL, 6, 1);
    assertTrue(fh.contains(os));
    os = fh.submit(SELL, 4, 1);
    // Verify that sell order @ 4 which has matched is still in FH
    assertTrue(fh.contains(os));
    assertTrue(fh.contains(ob));
  }

  @Test
  public void matchedQuoteTest() {
    // Test when only matched orders in order book
    fh.submit(BUY, 10, 1);
    fh.submit(SELL, 5, 1);
    // BID=max{max(matched sells), max(unmatched buys)}
    // ASK=min{min(matched buys), min(unmatched sells)}
    assertEquals((Integer) 5, fh.bidQuote());
    assertEquals((Integer) 10, fh.askQuote());
  }

  @Test
  public void askQuoteTest() {
    assertEquals(null, fh.bidQuote());
    assertEquals(null, fh.askQuote());

    // Test when no matched orders
    fh.submit(SELL, 10, 1);
    assertEquals((Integer) 10, fh.askQuote());
    assertEquals(null, fh.bidQuote());
    fh.submit(BUY, 5, 1);
    assertEquals((Integer) 5, fh.bidQuote());

    // Test when some orders matched
    // BID=max{max(matched sells), max(unmatched buys)} -> max(10, 5)
    // ASK=min{min(matched buys), min(unmatched sells)} -> min(15, -)
    fh.submit(BUY, 15, 1);
    assertEquals((Integer) 15, fh.askQuote()); // the matched buy at 15
    assertEquals((Integer) 10, fh.bidQuote());

    // Now orders in each container in FH
    fh.submit(SELL, 20, 1);
    assertEquals((Integer) 10, fh.bidQuote()); // max(10, 5)
    assertEquals((Integer) 15, fh.askQuote()); // min(15, 20)

  }


  @Test
  public void bidQuoteTest() {
    assertEquals(null, fh.bidQuote());
    assertEquals(null, fh.askQuote());

    // Test when no matched orders
    fh.submit(BUY, 15, 1);
    assertEquals((Integer) 15, fh.bidQuote());
    assertEquals(null, fh.askQuote());
    fh.submit(SELL, 20, 1);
    assertEquals((Integer) 20, fh.askQuote());

    // Test when some orders matched
    // BID=max{max(matched sells), max(unmatched buys)} -> max(10, -)
    // ASK=min{min(matched buys), min(unmatched sells)} -> min(15, 20)
    fh.submit(SELL, 10, 1);
    assertEquals((Integer) 10, fh.bidQuote()); // the matched sell at 10
    assertEquals((Integer) 15, fh.askQuote());

    // Now orders in each container in FH
    fh.submit(BUY, 5, 1);
    assertEquals((Integer) 10, fh.bidQuote()); // max(10, 5)
    assertEquals((Integer) 15, fh.askQuote()); // min(15, 20)
  }

  @Test
  public void specificInvariantTest1() {
    fh.submit(BUY, 2, 1);
    fh.submit(SELL, 1, 1);
    fh.submit(SELL, 4, 1);
    fh.submit(BUY, 3, 1);
    fh.submit(BUY, 5, 1);

    assertInvariants(fh);
  }

  @Test
  public void specificInvariantTest2() {
    fh.submit(SELL, 4, 1);
    fh.submit(BUY, 5, 1);
    fh.submit(BUY, 2, 1);
    fh.submit(SELL, 3, 1);
    fh.submit(SELL, 1, 1);

    assertInvariants(fh);
  }

  @Test
  public void quoteInvariantTest() {
    for (int i = 0; i < 1000; i++) {
      fh.submit(rand.nextBoolean() ? BUY : SELL, rand.nextInt(900000) + 100000, 1);
      assertInvariants(fh);
    }
  }

  @Test
  public void repeatedInvarianceTest() {
    for (int i = 0; i < 10; i++)
      quoteInvariantTest();
  }

  @Test
  public void serializableTest() throws IOException {
    ByteArrayOutputStream raw = new ByteArrayOutputStream();
    ObjectOutputStream objects = new ObjectOutputStream(raw);
    objects.writeObject(fh);
    objects.flush();
    assertTrue(raw.size() > 0);
  }

  // Helper methods

  private static int matchedSize(Queue<Order<Integer>> bh) {
    int size = 0;
    for (Order<Integer> so : bh)
      size += so.matchedQuantity;
    return size;
  }

  private static void assertInvariants(FourHeap<Integer> fh) {
    Order<Integer> bi, bo, si, so;
    Integer bid, ask;

    bi = fh.buyMatched.peek();
    bo = fh.buyUnmatched.peek();
    si = fh.sellMatched.peek();
    so = fh.sellUnmatched.peek();
    bid = fh.bidQuote();
    ask = fh.askQuote();

    assertTrue(bi == null || bo == null || bi.price >= bo.price);
    assertTrue(so == null || si == null || so.price >= si.price);
    assertTrue(so == null || bo == null || so.price >= bo.price);
    assertTrue(bi == null || si == null || bi.price >= si.price);
    assertTrue(bid == null || ask == null || bid <= ask);
    assertEquals(matchedSize(fh.sellMatched), matchedSize(fh.buyMatched));
    assertEquals(fh.getNumberOfUnits(), fh.getBidDepth() + fh.getAskDepth());
  }

}
