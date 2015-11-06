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

import defrac.util.Array;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class SkeletonData {
  @Nonnull
  final Array<BoneData> bones = new Array<>(); // Ordered parents first.
  @Nonnull
  final Array<SlotData> slots = new Array<>(); // Setup pose draw order.
  @Nonnull
  final Array<Skin> skins = new Array<>();
  @Nonnull
  final Array<EventData> events = new Array<>();
  @Nonnull
  final Array<Animation> animations = new Array<>();
  @Nonnull
  final Array<IkConstraintData> ikConstraints = new Array<>();
  @Nullable
  String name;
  @Nullable
  Skin defaultSkin;
  float width, height;
  String version, hash, imagesPath;

  // --- Bones.

  @Nonnull
  public Array<BoneData> bones() {
    return bones;
  }

  /** @return May be null. */
  @Nullable
  public BoneData findBone(@Nonnull final String boneName) {
    final int index = findBoneIndex(boneName);
    return index == -1
        ? null
        : bones.get(index);
  }

  /** @return -1 if the bone was not found. */
  public int findBoneIndex(@Nonnull final String boneName) {
    int index = 0;

    for(final BoneData bone : bones) {
      if(boneName.equals(bone.name)) {
        return index;
      }

      ++index;
    }

    return -1;
  }

  // --- Slots.

  @Nonnull
  public Array<SlotData> slots() {
    return slots;
  }

  /** @return May be null. */
  public SlotData findSlot(@Nonnull final String slotName) {
    final int index = findSlotIndex(slotName);
    return index == -1
        ? null
        : slots.get(index);
  }

  /** @return -1 if the bone was not found. */
  public int findSlotIndex(@Nonnull final  String slotName) {
    int index = 0;

    for(final SlotData slot : slots) {
      if(slotName.equals(slot.name)) {
        return index;
      }

      ++index;
    }

    return -1;
  }

  // --- Skins.

  /** @return May be null. */
  @Nullable
  public Skin defaultSkin() {
    return defaultSkin;
  }

  /** @param value May be null. */
  public void defaultSkin(@Nullable final Skin value) {
    defaultSkin = value;
  }

  /** @return May be null. */
  @Nullable
  public Skin findSkin(@Nonnull final String skinName) {
    for(final Skin skin : skins) {
      if(skinName.equals(skin.name)) {
        return skin;
      }
    }

    return null;
  }

  /** Returns all skins, including the default skin. */
  @Nonnull
  public Array<Skin> skins() {
    return skins;
  }

  // --- Events.

  /** @return May be null. */
  @Nullable
  public EventData findEvent(@Nonnull final String eventDataName) {
    for(final EventData eventData : events) {
      if(eventDataName.equals(eventData.name)) {
        return eventData;
      }
    }

    return null;
  }

  @Nonnull
  public Array<EventData> events() {
    return events;
  }

  // --- Animations.

  @Nonnull
  public Array<Animation> animations() {
    return animations;
  }

  /** @return May be null. */
  @Nullable
  public Animation findAnimation(@Nonnull final String animationName) {
    for(final Animation animation : animations) {
      if(animationName.equals(animation.name)) {
        return animation;
      }
    }

    return null;
  }

  // --- IK

  @Nonnull
  public Array<IkConstraintData> ikConstraints() {
    return ikConstraints;
  }

  /** @return May be null. */
  @Nullable
  public IkConstraintData findIkConstraint(@Nonnull final String ikConstraintName) {
    for(final IkConstraintData ikConstraintData : ikConstraints) {
      if(ikConstraintName.equals(ikConstraintData.name)) {
        return ikConstraintData;
      }
    }

    return null;
  }

  // ---

  /** @return May be null. */
  @Nullable
  public String name() {
    return name;
  }

  /** @param value May be null. */
  public void name(@Nullable final String value) {
    name = value;
  }

  public float width() {
    return width;
  }

  public void width(final float value) {
    width = value;
  }

  public float height() {
    return height;
  }

  public void height(final float value) {
    height = value;
  }

  /** Returns the Spine version used to export this data, or null. */
  @Nullable
  public String version() {
    return version;
  }

  /** @param value May be null. */
  public void version(@Nullable final String value) {
    version = value;
  }

  /** @return May be null. */
  @Nullable
  public String hash() {
    return hash;
  }

  /** @param value May be null. */
  public void setHash(@Nullable final String value) {
    hash = value;
  }

  /** @return May be null. */
  @Nullable
  public String imagesPath() {
    return imagesPath;
  }

  /** @param value May be null. */
  public void imagesPath(@Nullable final String value) {
    imagesPath = value;
  }

  public String toString() {
    return name != null ? name : super.toString();
  }
}
