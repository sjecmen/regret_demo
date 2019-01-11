package edu.umich.srg.collect;

import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class Streams {

  /** Convert an iterator into a stream. */
  public static <T> Stream<T> stream(Iterator<T> iterator) {
    return StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(Objects.requireNonNull(iterator), Spliterator.ORDERED),
        false);
  }

  /** Convert an iterable into a stream. */
  public static <T> Stream<T> stream(Iterable<T> iterable) {
    return StreamSupport.stream(iterable.spliterator(), false);
  }

  /** Convert a spliterator into a stream. */
  public static <T> Stream<T> stream(Spliterator<T> spliterator) {
    return StreamSupport.stream(spliterator, false);
  }

  private Streams() {} // Unconstructable

}
