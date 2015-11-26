/*
 * Copyright 2015 defrac inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package defrac.util;

import defrac.lang.FloatFunction;

import javax.annotation.Nonnull;

import static defrac.lang.Preconditions.checkArgument;

/**
 * The FloatLUT class represents a look-up table for floating point values
 */
public final class FloatLUT {
  private static final int DEFAULT_SIZE = 2048;

  /**
   * Creates and returns a new FloatLUT for the given function
   *
   * <p>To create a look-up table for the Math.sin function, one
   * would invoke {@code build(0.0f, (float)Math.PI * 2.0f, x -> (float)Math.sin(x))}
   *
   * @param min The minimum value, inclusive
   * @param max The maximum value, inclusive
   * @param function The float function
   * @return The look-up table for the given function
   */
  @Nonnull
  public static FloatLUT build(final float min,
                               final float max,
                               @Nonnull final FloatFunction function) {
    return build(min, max, DEFAULT_SIZE, function);
  }

  /**
   * Creates and returns a new FloatLUT for the given function
   *
   * <p>To create a look-up table for the Math.sin function, one
   * would invoke {@code build(0.0f, (float)Math.PI * 2.0f, 4096, x -> (float)Math.sin(x))}
   *
   * @param min The minimum value, inclusive
   * @param max The maximum value, inclusive
   * @param size The size of the look-up table; must be a power of two
   * @param function The float function
   * @return The look-up table for the given function
   * @throws IllegalArgumentException If the given size is not a power of two
   * @throws IllegalArgumentException If the given size is one
   */
  public static FloatLUT build(final float min,
                               final float max,
                               final int size,
                               @Nonnull final FloatFunction function) {
    checkArgument(MathUtil.isPowerOfTwo(size), "size must be a power of two");
    checkArgument(size > 1, "size must be larger than one");

    final float[] values = new float[size];

    final float d = (max - min);
    final float phaseInc = d / (float)(size - 1);

    float phase = min;

    for(int i = 0; i < size; ++i) {
      values[i] = function.apply(phase);
      phase += phaseInc;
    }

    return new FloatLUT(values, (float)size / d, size - 1);
  }

  /**
   * Creates and returns a new FloatLUT for the given values
   *
   * <p>To create a look-up table for the Math.sin function, one
   * would invoke {@code create(0.0f, (float)Math.PI * 2.0f, sinValues)}
   * assuming {@code sinValues} is an array that contains values in the
   * range {@code [0, 2π]}
   *
   * @param min The minimum value, inclusive
   * @param max The maximum value, inclusive
   * @param values The array of pre-computed values; must have a length that is a power of two
   * @throws IllegalArgumentException If {@code values.length} equals one
   * @throws IllegalArgumentException If {@code values.length} is not a power of two
   * @return The look-up table for the given values
   */
  public static FloatLUT create(final float min,
                                final float max,
                                @Nonnull final float[] values) {
    final int size = values.length;

    checkArgument(MathUtil.isPowerOfTwo(size), "values.length must be a power of two");

    final float d = (max - min);
    return new FloatLUT(values, (float)size / d, size - 1);
  }

  @Nonnull
  private final float[] values;

  private final float inv;

  private final int bitmask;

  private FloatLUT(@Nonnull final float[] values,
                   final float inv,
                   final int bitmask) {
    this.values = values;
    this.inv = inv;
    this.bitmask = bitmask;
  }

  /**
   * Performs a look-up in the table using linear interpolation
   *
   * <p>The given input value will be wrapped around. If the table has been created
   * with {@code min=0} and {@code max=2π} the result of {@code get(2π + 0.1)} will
   * be the same as {@code get(0.1)}
   *
   * @param x The value to lookup
   * @return The computed value for {@code x}
   */
  public float get(final float x) {
    final float mapped = x * inv;
    final int floored = (int)mapped;
    final float alpha = mapped - floored;
    final float a = values[ floored      & bitmask];
    final float b = values[(floored + 1) & bitmask];
    return a + alpha * (b - a);
  }

  /**
   * Performs a look-up in the table
   *
   * <p>The given input value will be wrapped around. If the table has been created
   * with {@code min=0} and {@code max=2π} the result of {@code get(2π + 0.1)} will
   * be the same as {@code get(0.1)}
   *
   * @param x The value to lookup
   * @return The computed value for {@code x}
   */
  public float getRaw(final float x) {
    return values[(int)(x * inv) & bitmask];
  }
}
