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
import defrac.util.FloatArray;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class Slot {
  @Nonnull final SlotData data;
  @Nonnull final Bone bone;

  @Nullable Attachment attachment;
  private float attachmentTime;

  public float r = 1.0f, g = 1.0f, b = 1.0f, a = 1.0f;

  @Nonnull
  public FloatArray attachmentVertices = new FloatArray();

  public Slot(@Nonnull final SlotData data,@Nonnull final Bone bone) {
    this.data = data;
    this.bone = bone;
    setToSetupPose();
  }

  /** Copy constructor. */
  public Slot(@Nonnull final Slot slot, @Nonnull final Bone bone) {
    this.data = slot.data;
    this.bone = bone;
    this.r = slot.r;
    this.g = slot.g;
    this.b = slot.b;
    this.attachment = slot.attachment;
    this.attachmentTime = slot.attachmentTime;
  }

  @Nonnull
  public SlotData data() {
    return data;
  }

  @Nonnull
  public Bone bone() {
    return bone;
  }

  public Skeleton skeleton() {
    return bone.skeleton;
  }

  @Nullable
  public Attachment attachment() {
    return attachment;
  }

  /** Sets the attachment, resets {@link #attachmentTime()}, and clears {@link #attachmentVertices()}.
   * @param value May be null. */
  public void attachment(@Nullable final Attachment value) {
    if(attachment == value) {
      return;
    }

    attachment = value;
    attachmentTime = bone.skeleton.time;
    attachmentVertices.clear();
  }

  public float attachmentTime() {
    return bone.skeleton.time - attachmentTime;
  }

  public void attachmentTime(final float value) {
    attachmentTime = bone.skeleton.time - value;
  }

  @Nonnull
  public FloatArray attachmentVertices() {
    return attachmentVertices;
  }

  public void attachmentVertices(@Nonnull final FloatArray value) {
    attachmentVertices = value;
  }

  void setToSetupPose(int slotIndex) {
    this.r = data.r;
    this.g = data.g;
    this.b = data.b;
    this.a = data.a;
    attachment(data.attachmentName == null ? null : bone.skeleton.getAttachment(slotIndex, data.attachmentName));
  }

  public void setToSetupPose() {
    setToSetupPose(bone.skeleton.data.slots.indexOf(data));
  }

  public String toString() {
    return data.name;
  }
}
