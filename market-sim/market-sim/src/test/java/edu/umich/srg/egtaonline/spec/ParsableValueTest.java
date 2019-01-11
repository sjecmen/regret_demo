package edu.umich.srg.egtaonline.spec;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableList;

import org.junit.Test;

import edu.umich.srg.egtaonline.spec.ParsableValue.BoolValue;
import edu.umich.srg.egtaonline.spec.ParsableValue.DoubleValue;
import edu.umich.srg.egtaonline.spec.ParsableValue.EnumValue;
import edu.umich.srg.egtaonline.spec.ParsableValue.IntValue;
import edu.umich.srg.egtaonline.spec.ParsableValue.IntsValue;
import edu.umich.srg.egtaonline.spec.ParsableValue.LongValue;
import edu.umich.srg.egtaonline.spec.ParsableValue.StringValue;

public class ParsableValueTest {

  private static enum Enumerated {
    A, B, CEEEE
  };

  // FIXME Test empty string conditions

  @Test
  public void doubleTest() {
    DoubleValue d = new DoubleValue() {};
    d.parse("5.6");
    assertEquals(5.6, d.get(), 0);
  }

  @Test
  public void intTest() {
    IntValue i = new IntValue() {};
    i.parse("7");
    assertEquals(7, (int) i.get());
  }

  @Test
  public void longTest() {
    LongValue l = new LongValue() {};
    l.parse("8");
    assertEquals(8l, (long) l.get());
  }

  @Test
  public void boolTest() {
    BoolValue b = new BoolValue() {};
    b.parse("true");
    assertEquals(true, (boolean) b.get());
    b.parse("t");
    assertEquals(true, (boolean) b.get());
    b.parse("T");
    assertEquals(true, (boolean) b.get());
    b.parse("tRuE");
    assertEquals(true, (boolean) b.get());
    b.parse("f");
    assertEquals(false, (boolean) b.get());
    b.parse("F");
    assertEquals(false, (boolean) b.get());
    b.parse("false");
    assertEquals(false, (boolean) b.get());
    b.parse("fAlSe");
    assertEquals(false, (boolean) b.get());
  }

  @Test
  public void stringTest() {
    StringValue l = new StringValue() {};
    l.parse("simulation!");
    assertEquals("simulation!", l.get());
  }

  @Test
  public void intsTest() {
    IntsValue l = new IntsValue() {};
    l.parse("8");
    assertEquals(ImmutableList.of(8), ImmutableList.copyOf(l.get()));
    l.parse("8/7/6/100");
    assertEquals(ImmutableList.of(8, 7, 6, 100), ImmutableList.copyOf(l.get()));
  }

  @Test
  public void enumTest() {
    EnumValue<Enumerated> e = new EnumValue<Enumerated>(Enumerated.class) {};
    e.parse("A");
    assertEquals(Enumerated.A, e.get());
    e.parse("B");
    assertEquals(Enumerated.B, e.get());
    e.parse("CEEEE");
    assertEquals(Enumerated.CEEEE, e.get());
  }

}
