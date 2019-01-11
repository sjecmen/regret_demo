package edu.umich.srg.egtaonline.spec;

import static org.junit.Assert.assertEquals;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;

import org.junit.Test;

import edu.umich.srg.egtaonline.spec.ParsableValue.BoolValue;
import edu.umich.srg.egtaonline.spec.ParsableValue.DoubleValue;
import edu.umich.srg.egtaonline.spec.ParsableValue.EnumValue;
import edu.umich.srg.egtaonline.spec.ParsableValue.IntValue;
import edu.umich.srg.egtaonline.spec.ParsableValue.IntsValue;
import edu.umich.srg.egtaonline.spec.ParsableValue.LongValue;

public class SpecTest {

  private static enum Enumerated {
    A, B, C
  };

  public static final class LongKey extends LongValue {
  };
  public static final class IntKey extends IntValue {
  };
  public static final class DoubleKey extends DoubleValue {
  };
  public static final class OtherDoubleKey extends DoubleValue {
  };
  public static final class BoolKey extends BoolValue {
  };
  public static final class IntsKey extends IntsValue {
  };
  public static final class EnumKey extends EnumValue<Enumerated> {
    protected EnumKey() {
      super(Enumerated.class);
    }
  };

  public static final class PrivateKey extends Value<Object> {
    private PrivateKey() {};
  };
  public static final class BadConstructorKey extends Value<Object> {
    public BadConstructorKey(int a) {};
  };
  public static final class NotAValue {
  };
  public static final class Wrapper {
    @SuppressWarnings("unused")
    private static final class HiddenKey extends Value<Object> {
    };
  }

  @Test(expected = NullPointerException.class)
  public void emptyTest() {
    Spec props = Spec.empty();
    props.get(LongKey.class);
  }

  @Test
  public void longTest() {
    Spec props = Spec.fromPairs(LongKey.class, 5l);
    assertEquals(5l, (long) props.get(LongKey.class));
  }

  @Test
  public void doubleTest() {
    Spec props = Spec.fromPairs(DoubleKey.class, 6.6);
    assertEquals(6.6, props.get(DoubleKey.class), 0);
  }

  @Test
  public void intTest() {
    Spec props = Spec.fromPairs(IntKey.class, 7);
    assertEquals(7, (int) props.get(IntKey.class));
  }

  @Test
  public void boolTest() {
    Spec props = Spec.fromPairs(BoolKey.class, true);
    assertEquals(true, (boolean) props.get(BoolKey.class));
  }

  @Test
  public void intsTest() {
    Spec props = Spec.fromPairs(IntsKey.class, Ints.asList(1, 2, 3));
    assertEquals(ImmutableList.of(1, 2, 3), props.get(IntsKey.class));
  }

  @Test
  public void enumTest() {
    Spec props = Spec.fromPairs(EnumKey.class, Enumerated.A);
    assertEquals(Enumerated.A, props.get(EnumKey.class));
  }

  @Test
  public void parseBuilderTest() {
    Spec props = Spec.builder("edu.umich.srg.egtaonline.spec.SpecTest$").put("LongKey", "5")
        .put("EnumKey", "C").build();

    assertEquals(5l, (long) props.get(LongKey.class));
    assertEquals(Enumerated.C, props.get(EnumKey.class));
  }

  @Test
  public void parseBuilderCaseFormatTest() {
    Spec props =
        Spec.builder("edu.umich.srg.egtaonline.spec.SpecTest$", CaseFormat.LOWER_UNDERSCORE)
            .put("long_key", "5").put("enum_key", "C").build();

    assertEquals(5l, (long) props.get(LongKey.class));
    assertEquals(Enumerated.C, props.get(EnumKey.class));
  }

  @Test
  public void parseTestPairs() {
    Spec props = Spec.fromPairs("edu.umich.srg.egtaonline.spec.SpecTest$LongKey", "5",
        "edu.umich.srg.egtaonline.spec.SpecTest$EnumKey", "C");

    assertEquals(5l, (long) props.get(LongKey.class));
    assertEquals(Enumerated.C, props.get(EnumKey.class));
  }

  @Test
  public void overwriteTest() {
    Spec props = Spec.fromPairs(DoubleKey.class, 6.6, DoubleKey.class, 7.2);
    assertEquals(7.2, props.get(DoubleKey.class), 0);
  }

  @Test
  public void overwritePutAllTest() {
    Spec props1 = Spec.fromPairs(DoubleKey.class, 6.6);
    Spec props2 = Spec.builder().putAll(props1).put(DoubleKey.class, 7.2).build();
    assertEquals(7.2, props2.get(DoubleKey.class), 0);
  }

  @Test
  public void multipleKeysTest() {
    Spec props = Spec.fromPairs(DoubleKey.class, 5.6, LongKey.class, 7l, IntsKey.class,
        Ints.asList(3, 4, 5), OtherDoubleKey.class, 7.2, BoolKey.class, false);

    assertEquals(7l, (long) props.get(LongKey.class));
    assertEquals(5.6, props.get(DoubleKey.class), 0);
    assertEquals(7.2, props.get(OtherDoubleKey.class), 0);
    assertEquals(false, (boolean) props.get(BoolKey.class));
    assertEquals(ImmutableList.of(3, 4, 5), props.get(IntsKey.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void privateConstructorTest() {
    Spec.fromPairs(PrivateKey.class, 5);
  }

  @Test(expected = IllegalArgumentException.class)
  public void badConstructorTest() {
    Spec.fromPairs(BadConstructorKey.class, 5);
  }

  @Test(expected = IllegalArgumentException.class)
  public void invalidClassTest() {
    Spec.builder().put("edu.umich.srg.egtaonline.spec.SpecTest$NotAValue", "5").build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void hiddenClassTest() {
    Spec.builder().put("edu.umich.srg.egtaonline.spec.SpecTest$Wrapper$HiddenKey", "5").build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void nonexistentClassTest() {
    Spec.builder().put("edu.umich.srg.egtaonline.spec.SpecTest$Blah", "5").build();
  }

}
