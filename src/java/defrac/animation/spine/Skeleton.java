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

import defrac.animation.spine.attachments.Attachment;
import defrac.util.Array;
import defrac.util.Color;
import defrac.util.MathUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static defrac.lang.Preconditions.checkArgument;

public final class Skeleton {
  @Nonnull
  final SkeletonData data;

  @Nonnull
  final Array<Bone> bones;

  @Nonnull
  final Array<Slot> slots;

  @Nonnull
  final Array<IkConstraint> ikConstraints;

  @Nonnull
  private final Array<Array<Bone>> boneCache = new Array<>();

  @Nonnull
  Array<Slot> drawOrder;

  @Nullable
  Skin skin;

  float time;
  boolean flipX, flipY;
  float x, y;

  public float r, g, b, a;

  public Skeleton(@Nonnull final SkeletonData data) {
    this.data = data;

    bones = new Array<>(data.bones.size());
    for(final BoneData boneData : data.bones) {
      final Bone parent = boneData.parent == null ? null : bones.get(data.bones.identityIndexOf(boneData.parent));
      bones.push(new Bone(boneData, this, parent));
    }

    slots = new Array<>(data.slots.size());
    drawOrder = new Array<>(data.slots.size());
    for(final SlotData slotData : data.slots) {
      final Bone bone = bones.get(data.bones.identityIndexOf(slotData.boneData));
      final Slot slot = new Slot(slotData, bone);
      slots.push(slot);
      drawOrder.push(slot);
    }

    ikConstraints = new Array<>(data.ikConstraints.size());
    for(final IkConstraintData ikConstraintData : data.ikConstraints) {
      ikConstraints.push(new IkConstraint(ikConstraintData, this));
    }

    r = 1.0f;
    g = 1.0f;
    b = 1.0f;
    a = 1.0f;

    bones.trimToSize();
    slots.trimToSize();
    drawOrder.trimToSize();
    ikConstraints.trimToSize();

    updateCache();
  }

  /** Copy constructor. */
  public Skeleton(@Nonnull final Skeleton skeleton) {
    data = skeleton.data;

    bones = new Array<>(skeleton.bones.size());
    for(final Bone bone : skeleton.bones) {
      final Bone parent = bone.parent == null ? null : bones.get(skeleton.bones.identityIndexOf(bone.parent));
      bones.push(new Bone(bone, this, parent));
    }

    slots = new Array<>(skeleton.slots.size());
    for(final Slot slot : skeleton.slots) {
      final Bone bone = bones.get(skeleton.bones.identityIndexOf(slot.bone));
      slots.push(new Slot(slot, bone));
    }

    drawOrder = new Array<>(slots.size());
    for(final Slot slot : skeleton.drawOrder) {
      drawOrder.push(slots.get(skeleton.slots.identityIndexOf(slot)));
    }

    ikConstraints = new Array<>(skeleton.ikConstraints.size());
    for(final IkConstraint ikConstraint : skeleton.ikConstraints) {
      final Bone target = bones.get(skeleton.bones.identityIndexOf(ikConstraint.target));
      final Array<Bone> ikBones = new Array<>(ikConstraint.bones.size());
      for(final Bone bone : ikConstraint.bones) {
        ikBones.push(bones.get(skeleton.bones.identityIndexOf(bone)));
      }
      ikBones.trimToSize();
      ikConstraints.push(new IkConstraint(ikConstraint, ikBones, target));
    }

    skin = skeleton.skin;
    r = skeleton.r;
    g = skeleton.g;
    b = skeleton.b;
    a = skeleton.a;
    time = skeleton.time;
    flipX = skeleton.flipX;
    flipY = skeleton.flipY;


    bones.trimToSize();
    slots.trimToSize();
    drawOrder.trimToSize();
    ikConstraints.trimToSize();

    updateCache();
  }

  /** Caches information about bones and IK constraints. Must be called if bones or IK constraints are added or removed. */
  public void updateCache() {
    final Array<Bone> bones = this.bones;
    final Array<Array<Bone>> boneCache = this.boneCache;
    final Array<IkConstraint> ikConstraints = this.ikConstraints;
    final int ikConstraintsCount = ikConstraints.size();
    final int arrayCount = ikConstraintsCount + 1;

    for(final Array<Bone> cachedBones : boneCache) {
      cachedBones.clear();
    }

    while(boneCache.size() < arrayCount) {
      boneCache.push(new Array<>());
    }

    final Array<Bone> nonIkBones = boneCache.get(0);

    outer:
    for(final Bone bone : bones) {
      Bone current = bone;

      do {
        int boneCacheIndex = 0;

        for(final IkConstraint ikConstraint : ikConstraints) {
          final Bone parent = ikConstraint.bones.firstElement();
          Bone child = ikConstraint.bones.lastElement();

          while(true) {
            if(current == child) {
              boneCache.get(boneCacheIndex).push(bone);
              boneCache.get(boneCacheIndex + 1).push(bone);
              continue outer;
            }

            if(child == parent) {
              break;
            }

            child = child.parent;
          }

          ++boneCacheIndex;
        }

        current = current.parent;
      } while (current != null);

      nonIkBones.push(bone);
    }
  }

  /** Updates the world transform for each bone and applies IK constraints. */
  public void updateWorldTransform() {
    for(final Bone bone : bones) {
      bone.rotationIK = bone.rotation;
    }

    Array<Array<Bone>> boneCache = this.boneCache;
    Array<IkConstraint> ikConstraints = this.ikConstraints;

    int i = 0;
    int last = boneCache.size() - 1;

    while(true) {
      for(final Bone bone : boneCache.get(i)) {
        bone.updateWorldTransform();
      }

      if(i == last) {
        break;
      }

      ikConstraints.get(i).apply();
      ++i;
    }
  }

  /** Sets the bones and slots to their setup pose values. */
  public void setToSetupPose() {
    setBonesToSetupPose();
    setSlotsToSetupPose();
  }

  public void setBonesToSetupPose() {
    for(final Bone bone : bones) {
      bone.setToSetupPose();
    }

    for(final IkConstraint ikConstraint : ikConstraints) {
      ikConstraint.bendDirection = ikConstraint.data.bendDirection;
      ikConstraint.mix = ikConstraint.data.mix;
    }
  }

  public void setSlotsToSetupPose() {
    int drawOrderIndex = 0;

    for(final Slot slot : slots) {
      drawOrder.set(drawOrderIndex++, slot);
      slot.setToSetupPose();
    }
  }

  @Nonnull
  public SkeletonData data() {
    return data;
  }

  @Nonnull
  public Array<Bone> bones() {
    return bones;
  }

  /** @return May return null. */
  @Nullable
  public Bone rootBone() {
    return bones.isEmpty() ? null : bones.firstElement();
  }

  /** @return May be null. */
  public Bone findBone(@Nonnull final String boneName) {
    final int index = findBoneIndex(boneName);

    return index == -1
        ? null
        : bones.get(index);
  }

  /** @return -1 if the bone was not found. */
  public int findBoneIndex(@Nonnull final String boneName) {
    for(int boneIndex = 0, boneCount = bones.size(); boneIndex < boneCount; boneIndex++) {
      if(bones.get(boneIndex).data.name.equals(boneName)) {
        return boneIndex;
      }
    }

    return -1;
  }

  @Nonnull
  public Array<Slot> slots() {
    return slots;
  }

  /** @return May be null. */
  @Nullable
  public Slot findSlot(@Nonnull  String slotName) {
    final int index = findSlotIndex(slotName);

    return index == -1
        ? null
        : slots.get(index);
  }

  /** @return -1 if the bone was not found. */
  public int findSlotIndex(@Nonnull final String slotName) {
    for(int slotIndex = 0, slotCount = slots.size(); slotIndex < slotCount; slotIndex++) {
      if(slots.get(slotIndex).data.name.equals(slotName)) {
        return slotIndex;
      }
    }

    return -1;
  }

  /** Returns the slots in the order they will be drawn. The returned array may be modified to change the draw order. */
  @Nonnull
  public Array<Slot> drawOrder() {
    return drawOrder;
  }

  /** Sets the slots and the order they will be drawn. */
  public void drawOrder(@Nonnull final Array<Slot> value) {
    drawOrder = value;
  }

  /** @return May be null. */
  @Nullable
  public Skin skin() {
    return skin;
  }

  /** Sets the skin used to look up attachments before looking in the {@link SkeletonData#defaultSkin() default skin}.
   * Attachments from the new skin are attached if the corresponding attachment from the old skin was attached. If there was no
   * old skin, each slot's setup mode attachment is attached from the new skin.
   * @param value May be null. */
  public void skin(@Nullable final Skin value) {
    if(value != null) {
      if(skin != null) {
        value.attachAll(this, skin);
      } else {
        final Array<Slot> slots = this.slots;

        for(int slotIndex = 0, slotCount = slots.size(); slotIndex < slotCount; slotIndex++) {
          final Slot slot = slots.get(slotIndex);
          final String name = slot.data.attachmentName;

          if(name != null) {
            final Attachment attachment = value.getAttachment(slotIndex, name);

            if(attachment != null) {
              slot.attachment(attachment);
            }
          }
        }
      }
    }

    skin = value;
  }

  /** Sets a skin by name.
   * @see #skin(Skin) */
  public void skin(@Nonnull String skinName) {
    final Skin skin = data.findSkin(skinName);
    checkArgument(skin != null, "Skin not found: " + skinName);
    skin(skin);
  }

  /** @return May be null. */
  @Nullable
  public Attachment getAttachment(@Nonnull final String slotName,
                                  @Nonnull final String attachmentName) {
    return getAttachment(data.findSlotIndex(slotName), attachmentName);
  }

  /** @return May be null. */
  @Nullable
  public Attachment getAttachment(final int slotIndex,
                                  @Nonnull final String attachmentName) {
    if(skin != null) {
      final Attachment attachment = skin.getAttachment(slotIndex, attachmentName);

      if(attachment != null) {
        return attachment;
      }
    }

    if(data.defaultSkin != null) {
      return data.defaultSkin.getAttachment(slotIndex, attachmentName);
    }

    return null;
  }

  /** @param attachmentName May be null. */
  public void setAttachment(@Nonnull final String slotName,
                            @Nullable final String attachmentName) {
    for(int slotIndex = 0, slotCount = slots.size(); slotIndex < slotCount; slotIndex++) {
      final Slot slot = slots.get(slotIndex);

      if(slotName.equals(slot.data.name)) {
        Attachment attachment = null;

        if(attachmentName != null) {
          attachment = getAttachment(slotIndex, attachmentName);

          if(attachment == null) {
            throw new IllegalArgumentException("Attachment not found: " + attachmentName + ", for slot: " + slotName);
          }
        }

        slot.attachment(attachment);
        return;
      }
    }

    throw new IllegalArgumentException("Slot not found: " + slotName);
  }

  @Nonnull
  public Array<IkConstraint> ikConstraints() {
    return ikConstraints;
  }

  /** @return May be null. */
  public IkConstraint findIkConstraint(@Nonnull final String ikConstraintName) {
    for(final IkConstraint ikConstraint : ikConstraints) {
      if(ikConstraint.data.name.equals(ikConstraintName)) {
        return ikConstraint;
      }
    }

    return null;
  }

  public void color(final int valueARGB) {
    color(
        Color.extractRed(valueARGB),
        Color.extractGreen(valueARGB),
        Color.extractBlue(valueARGB),
        Color.extractAlpha(valueARGB));
  }

  public void color(final float r,
                    final float g,
                    final float b,
                    final float a) {
    this.r = MathUtil.clamp(r, 0.0f, 1.0f);
    this.g = MathUtil.clamp(g, 0.0f, 1.0f);
    this.b = MathUtil.clamp(b, 0.0f, 1.0f);
    this.a = MathUtil.clamp(a, 0.0f, 1.0f);
  }

  public boolean flipX() {
    return flipX;
  }

  public void flipX(final boolean value) {
    this.flipX = value;
  }

  public boolean flipY() {
    return flipY;
  }

  public void flipY(final boolean value) {
    this.flipY = value;
  }

  public void flip(final boolean flipX, boolean flipY) {
    this.flipX = flipX;
    this.flipY = flipY;
  }

  public float x() {
    return x;
  }

  public void x(final float value) {
    this.x = value;
  }

  public float y() {
    return y;
  }

  public void y(final float value) {
    this.y = value;
  }

  public void moveTo(final float x, final float y) {
    this.x = x;
    this.y = y;
  }

  public float time() {
    return time;
  }

  public void time(final float value) {
    this.time = value;
  }

  public void update(final float delta) {
    time += delta;
  }

  @Override
  public String toString() {
    return data.name != null ? data.name : super.toString();
  }
}
