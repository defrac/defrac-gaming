/******************************************************************************
 * Spine Runtimes Software License
 * Version 2.3
 * <p>
 * Copyright (c) 2013-2015, Esoteric Software
 * All rights reserved.
 * <p>
 * You are granted a perpetual, non-exclusive, non-sublicensable and
 * non-transferable license to use, install, execute and perform the Spine
 * Runtimes Software (the "Software") and derivative works solely for personal
 * or internal use. Without the written permission of Esoteric Software (see
 * Section 2 of the Spine Software License Agreement), you may not (a) modify,
 * translate, adapt or otherwise create derivative works, improvements of the
 * Software or develop new applications using the Software or (b) remove,
 * delete, alter or obscure any trademarks or any copyright, trademark, patent
 * or other intellectual property or proprietary rights notices on or in the
 * Software, including any copy thereof. Redistributions in binary or source
 * form must include this license and terms.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY ESOTERIC SOFTWARE "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL ESOTERIC SOFTWARE BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *****************************************************************************/

// Ported from the Spine runtime by defrac 2015.

package defrac.animation.spine;

import defrac.util.Color;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class BoneData {
  @Nonnull
  final String name;

  @Nullable
  final BoneData parent;

  float length;
  float x, y;
  float rotation;
  float scaleX = 1, scaleY = 1;
  boolean flipX, flipY;
  boolean inheritScale = true, inheritRotation = true;

  // Nonessential.
  int color = Color.rgba(0.61f, 0.61f, 0.61f, 1);

  /** @param parent May be null. */
  public BoneData(@Nonnull final String name,
                  @Nullable final BoneData parent) {
    this.name = name;
    this.parent = parent;
  }

  /** Copy constructor.
   * @param parent May be null. */
  public BoneData(@Nonnull final BoneData bone, @Nullable final BoneData parent) {
    this.parent = parent;

    name = bone.name;
    length = bone.length;
    x = bone.x;
    y = bone.y;
    rotation = bone.rotation;
    scaleX = bone.scaleX;
    scaleY = bone.scaleY;
    flipX = bone.flipX;
    flipY = bone.flipY;
  }

  /** @return May be null. */
  @Nullable
  public BoneData parent() {
    return parent;
  }

  @Nonnull
  public String name() {
    return name;
  }

  public float length() {
    return length;
  }

  public void length(final float value) {
    length = value;
  }

  public float x() {
    return x;
  }

  public void x(float value) {
    x = value;
  }

  public float y() {
    return y;
  }

  public void y(float value) {
    y = value;
  }

  public void moveTo(float x, float y) {
    this.x = x;
    this.y = y;
  }

  /**
   * @return The rotation in degrees
   */
  public float rotation() {
    return rotation;
  }

  public void rotation(float value) {
    rotation = value;
  }

  public float scaleX() {
    return scaleX;
  }

  public void scaleX(final float value) {
    scaleX = value;
  }

  public float scaleY() {
    return scaleY;
  }

  public void scaleY(final float value) {
    scaleY = value;
  }

  public void scaleTo(final float scaleX, final float scaleY) {
    this.scaleX = scaleX;
    this.scaleY = scaleY;
  }

  public boolean flipX() {
    return flipX;
  }

  public void flipX(final boolean value) {
    flipX = value;
  }

  public boolean flipY() {
    return flipY;
  }

  public void flipY(final boolean value) {
    flipY = value;
  }

  public boolean inheritScale() {
    return inheritScale;
  }

  public void inheritScale(boolean value) {
    inheritScale = value;
  }

  public boolean inheritRotation() {
    return inheritRotation;
  }

  public void inheritRotation(final boolean value) {
    inheritRotation = value;
  }

  public int getColor() {
    return color;
  }

  public void setColor(int color) {
    this.color = color;
  }

  @Nonnull
  @Override
  public String toString() {
    return name;
  }
}
