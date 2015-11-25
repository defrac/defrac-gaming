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

import defrac.geom.Point;
import defrac.util.Array;
import defrac.util.MathUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static defrac.lang.Preconditions.checkNotNull;

public final class IkConstraint {
  @Nonnull
  private static final Point TEMP_POSITION = new Point();

  @Nonnull
  final IkConstraintData data;

  @Nonnull
  final Array<Bone> bones;

  @Nullable
  Bone target;

  float mix = 1.0f;
  int bendDirection;

  public IkConstraint(@Nonnull final IkConstraintData data,
                      @Nonnull final Skeleton skeleton) {
    this.data = data;
    this.mix = data.mix;
    this.bendDirection = data.bendDirection;
    this.bones = new Array<>(data.bones.size());

    for(final BoneData boneData : data.bones) {
      bones.push(skeleton.findBone(boneData.name));
    }

    target = skeleton.findBone(data.target.name);
  }

  /** Copy constructor. */
  public IkConstraint(@Nonnull final IkConstraint ikConstraint,
                      @Nonnull final Array<Bone> bones,
                      @Nullable final Bone target) {
    this.data = ikConstraint.data;
    this.bones = bones; //TODO(joa): this isn't a copy?
    this.target = target;
    this.mix = ikConstraint.mix;
    this.bendDirection = ikConstraint.bendDirection;
  }

  public void apply() {
    final Bone target = checkNotNull(this.target);
    final Array<Bone> bones = this.bones;

    switch(bones.size()) {
      case 1: apply(bones.get(0), target.worldX, target.worldY, mix); break;
      case 2: apply(bones.get(0), bones.get(1), target.worldX, target.worldY, bendDirection, mix); break;
    }
  }

  /** Adjusts the bone rotation so the tip is as close to the target position as possible. The target is specified in the world
   * coordinate system. */
  static public void apply(@Nonnull final Bone bone,
                           final float targetX,
                           final float targetY,
                           final float alpha) {
    final float parentRotation = (!bone.data.inheritRotation || bone.parent == null) ? 0.0f : bone.parent.worldRotation;
    final float rotation = bone.rotation;

    float rotationIK =
        (float)Math.atan2(
            targetY - bone.worldY,
            targetX - bone.worldX) * MathUtil.RAD_TO_DEG;

    if(bone.worldFlipX == bone.worldFlipY) {
      rotationIK = -rotationIK;
    }

    rotationIK -= parentRotation;
    bone.rotationIK = rotation + (rotationIK - rotation) * alpha;
  }

  /** Adjusts the parent and child bone rotations so the tip of the child is as close to the target position as possible. The
   * target is specified in the world coordinate system.
   * @param child Any descendant bone of the parent. */
  static public void apply(@Nonnull final Bone parent,
                           @Nonnull final Bone child,
                           float targetX,
                           float targetY,
                           final int bendDirection,
                           final float alpha) {
    final float childRotation = child.rotation, parentRotation = parent.rotation;

    if(alpha == 0.0f) {
      child.rotationIK = childRotation;
      parent.rotationIK = parentRotation;
      return;
    }

    final Point position = TEMP_POSITION;
    final Bone parentParent = parent.parent;

    if(parentParent != null) {
      parentParent.worldToLocal(position.set(targetX, targetY));
      targetX = (position.x - parent.x) * parentParent.worldScaleX;
      targetY = (position.y - parent.y) * parentParent.worldScaleY;
    } else {
      targetX -= parent.x;
      targetY -= parent.y;
    }

    if(child.parent == parent) {
      position.set(child.x, child.y);
    } else {
      parent.worldToLocal(child.parent.localToWorld(position.set(child.x, child.y)));
    }

    final float childX = position.x * parent.worldScaleX;
    final float childY = position.y * parent.worldScaleY;
    final float offset = (float)Math.atan2(childY, childX);
    final float len1 = (float)Math.sqrt(childX * childX + childY * childY);
    final float len2 = child.data.length * child.worldScaleX;
    // Based on code by Ryan Juckett with permission: Copyright (c) 2008-2009 Ryan Juckett, http://www.ryanjuckett.com/
    final float cosDenom = 2.0f * len1 * len2;
    if(cosDenom < 0.0001f) {
      child.rotationIK =
          childRotation +
              (
                  (float)Math.atan2(targetY, targetX) * MathUtil.RAD_TO_DEG - parentRotation - childRotation
              ) * alpha;
      return;
    }
    final float cos = MathUtil.clamp(
        (targetX * targetX + targetY * targetY - len1 * len1 - len2 * len2) / cosDenom,
        -1.0f, 1.0f);
    final float childAngle = (float)Math.acos(cos) * bendDirection;
    final float adjacent = len1 + len2 * cos;
    final float opposite = len2 * (float)Math.sin(childAngle);
    final float parentAngle = (float)Math.atan2(targetY * adjacent - targetX * opposite, targetX * adjacent + targetY * opposite);

    float rotation = (parentAngle - offset) * MathUtil.RAD_TO_DEG - parentRotation;

    if(rotation > 180) {
      rotation -= 360;
    } else if(rotation < -180) {
      rotation += 360;
    }

    parent.rotationIK = parentRotation + rotation * alpha;

    rotation = (childAngle + offset) * MathUtil.RAD_TO_DEG - childRotation;

    if(rotation > 180) {
      rotation -= 360;
    } else if(rotation < -180) {
      rotation += 360;
    }

    child.rotationIK = childRotation + (rotation + parent.worldRotation - child.parent.worldRotation) * alpha;
  }

  @Nonnull
  public Array<Bone> bones() {
    return bones;
  }

  @Nullable
  public Bone target() {
    return target;
  }

  public void target(@Nullable final Bone value) {
    target = value;
  }

  public float mix() {
    return mix;
  }

  public void mix(final float value) {
    mix = value;
  }

  public int bendDirection() {
    return bendDirection;
  }

  public void bendDirection(final int value) {
    bendDirection = value;
  }

  @Nonnull
  public IkConstraintData data() {
    return data;
  }

  @Override
  @Nonnull
  public String toString() {
    return data.name;
  }
}
