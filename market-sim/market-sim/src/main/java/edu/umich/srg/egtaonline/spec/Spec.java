package edu.umich.srg.egtaonline.spec;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.CaseFormat;
import com.google.common.base.Joiner;
import com.google.common.base.Joiner.MapJoiner;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.MutableClassToInstanceMap;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

public class Spec {

  private static final MapJoiner toString = Joiner.on(", ").withKeyValueSeparator("=");
  private final ImmutableClassToInstanceMap<Value<?>> map;

  protected Spec(ImmutableClassToInstanceMap<Value<?>> map) {
    this.map = map;
  }

  public static Spec empty() {
    return builder().build();
  }

  public static <T> Spec fromPairs(Class<? extends Value<T>> key, T value) {
    return builder().put(key, value).build();
  }

  public static <T1, T2> Spec fromPairs(Class<? extends Value<T1>> key1, T1 value1,
      Class<? extends Value<T2>> key2, T2 value2) {
    return builder().put(key1, value1).put(key2, value2).build();
  }

  public static <T1, T2, T3> Spec fromPairs(Class<? extends Value<T1>> key1, T1 value1,
      Class<? extends Value<T2>> key2, T2 value2, Class<? extends Value<T3>> key3, T3 value3) {
    return builder().put(key1, value1).put(key2, value2).put(key3, value3).build();
  }

  public static <T1, T2, T3, T4> Spec fromPairs(Class<? extends Value<T1>> key1, T1 value1,
      Class<? extends Value<T2>> key2, T2 value2, Class<? extends Value<T3>> key3, T3 value3,
      Class<? extends Value<T4>> key4, T4 value4) {
    return builder().put(key1, value1).put(key2, value2).put(key3, value3).put(key4, value4)
        .build();
  }

  public static <T1, T2, T3, T4, T5> Spec fromPairs(Class<? extends Value<T1>> key1, T1 value1,
      Class<? extends Value<T2>> key2, T2 value2, Class<? extends Value<T3>> key3, T3 value3,
      Class<? extends Value<T4>> key4, T4 value4, Class<? extends Value<T5>> key5, T5 value5) {
    return builder().put(key1, value1).put(key2, value2).put(key3, value3).put(key4, value4)
        .put(key5, value5).build();
  }

  public static Spec fromPairs(String key1, String value1, String... keyValuePairs) {
    return fromPairs(Iterables.concat(Collections.singleton(key1), Collections.singleton(value1),
        Arrays.asList(keyValuePairs)));
  }

  public static Spec fromPairs(Iterable<String> keyValuePairs) {
    return fromPairs(keyValuePairs.iterator());
  }

  /** Build a spec from string pairs. */
  public static Spec fromPairs(Iterator<String> keyValuePairs) {
    Builder builder = builder();
    while (keyValuePairs.hasNext()) {
      builder.put(keyValuePairs.next(), keyValuePairs.next());
    }
    return builder.build();
  }

  public static Spec fromPairs(String keyClassPrefix, CaseFormat keyCaseFormat,
      Iterable<String> keyValuePairs) {
    return fromPairs(keyClassPrefix, keyCaseFormat, keyValuePairs.iterator());
  }

  /** Build a spec from string pairs with specified prefix and case format. */
  public static Spec fromPairs(String keyClassPrefix, CaseFormat keyCaseFormat,
      Iterator<String> keyValuePairs) {
    Builder builder = builder(keyClassPrefix, keyCaseFormat);
    while (keyValuePairs.hasNext()) {
      builder.put(keyValuePairs.next(), keyValuePairs.next());
    }
    return builder.build();
  }

  public static <T> Spec fromDefaultPairs(Spec defaults, Class<? extends Value<T>> key, T value) {
    return builder().putAll(defaults).put(key, value).build();
  }

  public static <T1, T2> Spec fromDefaultPairs(Spec defaults, Class<? extends Value<T1>> key1,
      T1 value1, Class<? extends Value<T2>> key2, T2 value2) {
    return builder().putAll(defaults).put(key1, value1).put(key2, value2).build();
  }

  public static <T1, T2, T3> Spec fromDefaultPairs(Spec defaults, Class<? extends Value<T1>> key1,
      T1 value1, Class<? extends Value<T2>> key2, T2 value2, Class<? extends Value<T3>> key3,
      T3 value3) {
    return builder().putAll(defaults).put(key1, value1).put(key2, value2).put(key3, value3).build();
  }

  public static <T1, T2, T3, T4> Spec fromDefaultPairs(Spec defaults,
      Class<? extends Value<T1>> key1, T1 value1, Class<? extends Value<T2>> key2, T2 value2,
      Class<? extends Value<T3>> key3, T3 value3, Class<? extends Value<T4>> key4, T4 value4) {
    return builder().putAll(defaults).put(key1, value1).put(key2, value2).put(key3, value3)
        .put(key4, value4).build();
  }

  public static <T1, T2, T3, T4, T5> Spec fromDefaultPairs(Spec defaults,
      Class<? extends Value<T1>> key1, T1 value1, Class<? extends Value<T2>> key2, T2 value2,
      Class<? extends Value<T3>> key3, T3 value3, Class<? extends Value<T4>> key4, T4 value4,
      Class<? extends Value<T5>> key5, T5 value5) {
    return builder().putAll(defaults).put(key1, value1).put(key2, value2).put(key3, value3)
        .put(key4, value4).put(key5, value5).build();
  }

  /*
   * We may at some point want to also have a method similar to the map (java8) getWithDefault,
   * however this may encourage putting defaults in specific class implementations which can make it
   * confusing as to what is actually being run, and won't throw errors for misconfigured
   * specifications
   */

  /** Get a value from the spec. */
  public <T> T get(Class<? extends Value<T>> key) {
    Value<T> val = checkNotNull(map.getInstance(key), "Key \"%s\" does not exist in spec",
        key.getSimpleName());
    return val.get();
  }

  public Set<Entry<Class<? extends Value<?>>, Value<?>>> entrySet() {
    return map.entrySet();
  }

  public Spec withDefault(Spec defaults) {
    return builder().putAll(defaults).putAll(this).build();
  }

  public static Builder builder() {
    return new Builder("", CaseFormat.UPPER_CAMEL);
  }

  public static Builder builder(String classPrefix) {
    return new Builder(classPrefix, CaseFormat.UPPER_CAMEL);
  }

  public static Builder builder(CaseFormat caseFormat) {
    return new Builder("", caseFormat);
  }

  public static Builder builder(String classPrefix, CaseFormat caseFormat) {
    return new Builder(classPrefix, caseFormat);
  }

  public static class Builder {
    // This is used instead of a builder, so we can overwrite keys
    private final MutableClassToInstanceMap<Value<?>> builder;
    private final String classPrefix;
    private final CaseFormat caseFormat;

    private Builder(String classPrefix, CaseFormat caseFormat) {
      this.builder = MutableClassToInstanceMap.create();
      this.classPrefix = classPrefix;
      this.caseFormat = caseFormat;
    }

    /** Put a value in the builder. */
    @SuppressWarnings("unchecked")
    public <T> Builder put(Class<? extends Value<T>> key, T value) {
      Value<T> instance = getInstance(key);
      instance.set(value);
      builder.putInstance((Class<Value<T>>) key, instance);
      return this;
    }

    /** Put a value interpreted from a string in the builder. */
    @SuppressWarnings("unchecked")
    public <T> Builder put(String className, String value) {
      className = classPrefix + caseFormat.to(CaseFormat.UPPER_CAMEL, className);
      Class<ParsableValue<?>> key;
      try {
        key = (Class<ParsableValue<?>>) Class.forName(className);
      } catch (ClassNotFoundException e) {
        throw new IllegalArgumentException(className + " doesn't exist");
      }
      ParsableValue<?> instance = getInstance(key);
      instance.parse(value);
      builder.putInstance(key, instance);
      return this;
    }

    /**
     * Put all values from a previous spec in the builder. This will override any previous settings
     * of a key.
     */
    @SuppressWarnings("unchecked")
    public Builder putAll(Spec other) {
      for (Entry<Class<? extends Value<?>>, Value<?>> e : other.map.entrySet()) {
        builder.putInstance((Class<Value<?>>) e.getKey(), e.getValue());
      }
      return this;
    }

    public Spec build() {
      // Generics allow ant compilation
      return new Spec(ImmutableClassToInstanceMap.<Value<?>, Value<?>>copyOf(builder));
    }

  }

  private static <T extends Value<?>> T getInstance(final Class<T> clazz) {
    T instance;
    try {
      instance = clazz.newInstance();
    } catch (InstantiationException e) {
      throw new IllegalArgumentException("Can't initiate empty constructor of key class " + clazz);
    } catch (IllegalAccessException e) {
      throw new IllegalArgumentException(
          "Either " + clazz + " or its empty constructor is inaccessable");
    } catch (ClassCastException e) {
      throw new IllegalArgumentException(
          clazz + " does not extend Value, and so is not a valid key");
    }
    return instance;
  }

  @Override
  public boolean equals(Object other) {
    if (other == null || !(other instanceof Spec)) {
      return false;
    }
    Spec that = (Spec) other;
    return Objects.equals(this.map, that.map);
  }

  @Override
  public int hashCode() {
    return map.hashCode();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder().append('{');
    toString.appendTo(builder, map.entrySet().stream()
        .map(e -> new SimpleImmutableEntry<>(e.getKey().getSimpleName(), e.getValue())).iterator());
    return builder.append('}').toString();
  }

}
