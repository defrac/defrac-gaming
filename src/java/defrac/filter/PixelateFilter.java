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
package defrac.filter;

import defrac.display.TextureData;
import defrac.display.render.GLUniformLocationCache;
import defrac.geom.Point;
import defrac.gl.GL;
import defrac.gl.GLProgram;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static defrac.lang.Preconditions.checkArgument;

/**
 *
 */
public final class PixelateFilter implements Filter {
  private static final float DEFAULT_STRENGTH = 2.0f;
  private static final float DEFAULT_DYNAMIC_RANGE = 256.0f;

  private float strengthX = 1.0f;
  private float strengthY = 1.0f;
  private float dynamicRange = 256.0f;

  public PixelateFilter() {
    this(DEFAULT_STRENGTH, DEFAULT_DYNAMIC_RANGE);
  }

  public PixelateFilter(final float strength) {
    this(strength, DEFAULT_DYNAMIC_RANGE);
  }

  public PixelateFilter(final float strength, final float dynamicRange) {
    final float normalizedStrength = normalize(strength);
    this.strengthX = normalizedStrength;
    this.strengthY = normalizedStrength;
    this.dynamicRange = dynamicRange;
  }

  public PixelateFilter(final float strengthX, final float strengthY, final float dynamicRange) {
    this.strengthX = normalize(strengthX);
    this.strengthY = normalize(strengthY);
    this.dynamicRange = dynamicRange;
  }

  private float normalize(final float value) {
    checkArgument(!Float.isNaN(value), "Can't assign NaN for strength");
    checkArgument(!Float.isInfinite(value), "Can't assign Infinity for strength");
    return Math.max(value, 1.0f);
  }

  @Nonnull
  public PixelateFilter strength(final float value) {
    strengthX = strengthY = normalize(value);
    return this;
  }

  @Nonnull
   public PixelateFilter strengthX(final float value) {
    strengthX = normalize(value);
    return this;
  }

  public float strengthX() {
    return strengthX;
  }

  @Nonnull
  public PixelateFilter strengthY(final float value) {
    strengthY = normalize(value);
    return this;
  }

  public float strengthY() {
    return strengthY;
  }

  @Nonnull
  public PixelateFilter dynamicRange(final float value) {
    checkArgument(!Float.isNaN(value), "Can't assign NaN for dynamic range");
    checkArgument(!Float.isInfinite(value), "Can't assign Infinity for dynamic range");

    dynamicRange = Math.max(value, 1.0f);

    return this;
  }

  public float dynamicRange() {
    return dynamicRange;
  }

  @Nonnull
  @Override
  public Point computeExtent(final float width, final float height, @Nonnull final Point point) {
    point.x = 0.0f;
    point.y = 0.0f;
    return point;
  }

  @Override
  public void appendCode(final int pass,
                         final float width, final float height,
                         final float viewportWidth, final float viewportHeight,
                         @Nonnull final StringBuilder builder) {
    if(pass != 0) {
      return;
    }

    builder.append(
        "vec2 pixelate_uv = vec2(ceil(v_uv.x * u_pixelate.x) / u_pixelate.x, ceil(v_uv.y * u_pixelate.y) / u_pixelate.y);"+
        "color = ceil(getPixel(pixelate_uv) * u_pixelate.z) / u_pixelate.z;");
  }

  @Override
  public void appendUniforms(final int pass,
                             final float width, final float height,
                             final float viewportWidth, final float viewportHeight,
                             @Nonnull final StringBuilder builder) {
    if(pass != 0) {
      return;
    }

    builder.
        append("uniform vec3 u_pixelate;");
  }

  @Override
  public void applyUniforms(final int pass,
                            final float width, final float height,
                            final float viewportWidth, final float viewportHeight,
                            @Nonnull final GL gl,
                            @Nonnull final GLProgram program,
                            @Nonnull final GLUniformLocationCache uniforms) {
    if(pass != 0) {
      return;
    }

    gl.uniform3f(
        uniforms.get(gl, program, "u_pixelate"),
        width / strengthX,
        height / strengthY,
        dynamicRange);

  }

  @Override
  public int numPasses() {
    return 1;
  }

  @Nullable
  @Override
  public TextureData[] inputs(final int pass) {
    return null;
  }

  @Override
  public boolean isSampling(final int pass) {
    return pass == 0;
  }

  @Override
  public boolean preserveOriginal(final int pass) {
    return false;
  }

  @Override
  public boolean mustRunExclusive() {
    return false;
  }

  @Override
  public int modStamp() {
    return 0;
  }

  @Override
  public String toString() {
    return "[PixelateFilter strengthX: "+strengthX+", strengthY: "+strengthY+", dynamicRange: "+dynamicRange+']';
  }
}
