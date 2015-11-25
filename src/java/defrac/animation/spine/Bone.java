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

import defrac.geom.Matrix;
import defrac.geom.Point;
import defrac.util.MathUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class Bone {
  @Nonnull
  final BoneData data;

  @Nonnull
  final Skeleton skeleton;

  @Nullable
  final Bone parent;

  float x, y;
  float rotation, rotationIK;
  float scaleX, scaleY;
  boolean flipX, flipY;

  float m00, m01, worldX; // a b x
  float m10, m11, worldY; // c d y
  float worldRotation;
  float worldScaleX, worldScaleY;
  boolean worldFlipX, worldFlipY;

  /** @param parent May be null. */
  public Bone(@Nonnull final BoneData data,
              @Nonnull final Skeleton skeleton,
              @Nullable final Bone parent) {
    this.data = data;
    this.skeleton = skeleton;
    this.parent = parent;

    setToSetupPose();
  }

  /** Copy constructor.
   * @param parent May be null. */
  public Bone(@Nonnull final Bone bone,
              @Nonnull final Skeleton skeleton,
              @Nullable final Bone parent) {
    this.skeleton = skeleton;
    this.parent = parent;
    data = bone.data;
    x = bone.x;
    y = bone.y;
    rotation = bone.rotation;
    rotationIK = bone.rotationIK;
    scaleX = bone.scaleX;
    scaleY = bone.scaleY;
    flipX = bone.flipX;
    flipY = bone.flipY;
  }

  /** Computes the world SRT using the parent bone and the local SRT. */
  public void updateWorldTransform() {
    final Skeleton skeleton = this.skeleton;
    final Bone parent = this.parent;
    final float x = this.x;
    final float y = this.y;

    if(parent != null) {
      worldX = x * parent.m00 + y * parent.m01 + parent.worldX;
      worldY = x * parent.m10 + y * parent.m11 + parent.worldY;

      if(data.inheritScale) {
        worldScaleX = parent.worldScaleX * scaleX;
        worldScaleY = parent.worldScaleY * scaleY;
      } else {
        worldScaleX = scaleX;
        worldScaleY = scaleY;
      }

      worldRotation = data.inheritRotation ? parent.worldRotation + rotationIK : rotationIK;
      worldFlipX = parent.worldFlipX != flipX;
      worldFlipY = parent.worldFlipY != flipY;
    } else {
      final boolean skeletonFlipX = skeleton.flipX, skeletonFlipY = skeleton.flipY;

      worldX = skeletonFlipX ? -x : x;
      worldY = skeletonFlipY ? y : -y;
      worldScaleX = scaleX;
      worldScaleY = scaleY;
      worldRotation = rotationIK;
      worldFlipX = skeletonFlipX != flipX;
      worldFlipY = skeletonFlipY != flipY;
    }

    final float radians = MathUtil.degToRad(worldRotation);
    final float cos = (float)Math.cos(radians);
    final float sin = (float)Math.sin(radians);

    if(worldFlipX) {
      m00 = -cos * worldScaleX;
      m01 =  sin * worldScaleY;
    } else {
      m00 =  cos * worldScaleX;
      m01 = -sin * worldScaleY;
    }

    if(worldFlipY) {
      m10 = sin * worldScaleX;
      m11 = cos * worldScaleY;
    } else {
      m10 = -sin * worldScaleX;
      m11 = -cos * worldScaleY;
    }
  }

  public void setToSetupPose() {
    x = data.x;
    y = data.y;
    rotation = data.rotation;
    rotationIK = rotation;
    scaleX = data.scaleX;
    scaleY = data.scaleY;
    flipX = data.flipX;
    flipY = data.flipY;
  }

  public BoneData data() {
    return data;
  }

  public Skeleton skeleton() {
    return skeleton;
  }

  @Nullable
  public Bone parent() {
    return parent;
  }

  public float x() {
    return x;
  }

  public void x(final float value) {
    x = value;
  }

  public float y() {
    return y;
  }

  public void y(final float value) {
    y = value;
  }

  public void moveTo(final float x, final float y) {
    this.x = x;
    this.y = y;
  }

  /** Returns the forward kinetics rotation. */
  public float rotation() {
    return rotation;
  }

  public void rotation(final float value) {
    rotation = value;
  }

  /** Returns the inverse kinetics rotation, as calculated by any IK constraints. */
  public float rotationIk() {
    return rotationIK;
  }

  public void rotationIk(final float value) {
    rotationIK = value;
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

  public void scaleTo(final float value) {
    scaleX = value;
    scaleY = value;
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

  public float m00() {
    return m00;
  }

  public float m01() {
    return m01;
  }

  public float m10() {
    return m10;
  }

  public float m11() {
    return m11;
  }

  public float worldX() {
    return worldX;
  }

  public float worldY() {
    return worldY;
  }

  public float worldRotation() {
    return worldRotation;
  }

  public float worldScaleX() {
    return worldScaleX;
  }

  public float worldScaleY() {
    return worldScaleY;
  }

  public boolean worldFlipX() {
    return worldFlipX;
  }

  public boolean worldFlipY() {
    return worldFlipY;
  }

  @Nonnull
  public Matrix worldTransform(@Nonnull final Matrix target) {
    target.set(m01, m01, m10, m11, worldX, worldY);
    return target;
  }

  @Nonnull
  public Point worldToLocal(@Nonnull final Point world) {
    final float dx = world.x - worldX;
    final float dy = world.y - worldY;

    float m00 = this.m00, m10 = this.m10, m01 = this.m01, m11 = this.m11;

    if(worldFlipX == worldFlipY) {
      m00 = -m00;
      m11 = -m11;
    }

    final float invDet = 1.0f / (m00 * m11 - m01 * m10);

    world.x = (dx * m00 * invDet - dy * m01 * invDet);
    world.y = (dy * m11 * invDet - dx * m10 * invDet);

    return world;
  }

  @Nonnull
  public Point localToWorld(@Nonnull final Point local) {
    float x = local.x, y = local.y;
    local.x = x * m00 + y * m01 + worldX;
    local.y = x * m10 + y * m11 + worldY;
    return local;
  }

  public String toString() {
    return data.name;
  }
}
