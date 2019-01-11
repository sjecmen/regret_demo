package edu.umich.srg.collect;

import edu.umich.srg.collect.Sparse.Entry;

import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

public interface Sparse<E> extends Iterable<Entry<E>> {

  void add(long index, E element);

  void add(Entry<E> entry);

  Stream<? extends Entry<? extends E>> stream();

  int size();

  interface Entry<E> {

    long getIndex();

    E getElement();

  }

  /** Returns a view of a map entry as a Sparse.Entry. */
  static <E> Entry<E> asSparseEntry(Map.Entry<? extends Number, ? extends E> mapEntry) {
    return new Entry<E>() {
      @Override
      public long getIndex() {
        return mapEntry.getKey().longValue();
      }

      @Override
      public E getElement() {
        return mapEntry.getValue();
      }

      @Override
      public String toString() {
        return mapEntry.toString();
      }
    };
  }

  /** Returns a view of an iterator of map entries as an iterator of sparse entries. */
  static <E> Iterator<Entry<E>> asSparseIterator(
      Iterator<? extends Map.Entry<? extends Number, ? extends E>> base) {
    return new Iterator<Entry<E>>() {
      @Override
      public boolean hasNext() {
        return base.hasNext();
      }

      @Override
      public Entry<E> next() {
        return asSparseEntry(base.next());
      }
    };
  }

  /** Returns a view of an iterable of map entriest as an iterable of sparse entires. */
  static <E> Iterable<Entry<E>> asSparseIterable(
      Iterable<? extends Map.Entry<? extends Number, ? extends E>> base) {
    return () -> asSparseIterator(base.iterator());
  }

  /** View a standard iterator as a view of sparse entries. */
  static <T> Iterator<Entry<T>> fromDense(Iterator<T> iterator) {
    return new Iterator<Entry<T>>() {
      private long index = 0;

      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      public Entry<T> next() {
        return immutableEntry(index++, iterator.next());
      }

    };
  }

  /** View a standard iterable as a view of sparse entries. */
  static <T> Iterable<Entry<T>> fromDense(Iterable<T> iterator) {
    return () -> fromDense(iterator.iterator());
  }

  /** Create a simple immutable sparse entry. */
  static <E> Entry<E> immutableEntry(long index, E element) {
    return new Entry<E>() {

      @Override
      public long getIndex() {
        return index;
      }

      @Override
      public E getElement() {
        return element;
      }

      @Override
      public String toString() {
        return index + "=" + element;
      }

    };
  }

}
