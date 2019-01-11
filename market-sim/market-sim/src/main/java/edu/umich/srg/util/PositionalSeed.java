package edu.umich.srg.util;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This class allows getting sufficiently different positional seeds to ensure good random
 * performance for sequential integers.
 */
public class PositionalSeed {

  private static final int offset = Long.SIZE / Byte.SIZE;

  private final MessageDigest hash;
  private final ByteBuffer buffer;

  private PositionalSeed(long seed, String method) {
    this.buffer = ByteBuffer.allocate(2 * Long.SIZE / Byte.SIZE);
    try {
      this.hash = MessageDigest.getInstance(method);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Unable to get " + method + " Hash");
    }
    buffer.putLong(seed);
  }

  public static PositionalSeed with(long seed) {
    return new PositionalSeed(seed, "MD5");
  }

  /** Get the seed for a given position. */
  public long getSeed(long position) {
    buffer.putLong(offset, position);
    LongBuffer digest = ByteBuffer.wrap(hash.digest(buffer.array())).asLongBuffer();

    long seed = 1125899906842597L;
    while (digest.hasRemaining()) {
      seed = 31 * seed + digest.get();
    }

    return seed;
  }

}
