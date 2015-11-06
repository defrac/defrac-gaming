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
import defrac.pool.ObjectPool;
import defrac.pool.ObjectPools;
import defrac.util.Array;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

import static defrac.lang.Preconditions.checkArgument;

/** Stores attachments by slot index and attachment name. */
public final class Skin {
  @Nonnull
  private static final Key LOOKUP_KEY = new Key();

  @Nonnull
  private static final ObjectPool<Key> KEY_POOL = ObjectPools.newPool(64, Key::new);

  @Nonnull
  final String name;

  final Map<Key, Attachment> attachments = new HashMap<>();


  public Skin(@Nonnull final  String name) {
    this.name = name;
  }

  public void addAttachment(final int slotIndex,
                            @Nonnull final String name,
                            @Nonnull final Attachment attachment) {
    checkArgument(slotIndex >= 0, "slotIndex < 0");
    attachments.put(KEY_POOL.get().set(slotIndex, name), attachment);
  }

  /** @return May be null. */
  @Nullable
  public Attachment getAttachment(final int slotIndex, String name) {
    checkArgument(slotIndex >= 0, "slotIndex < 0");
    LOOKUP_KEY.set(slotIndex, name);
    return attachments.get(LOOKUP_KEY);
  }

  public void findNamesForSlot(final int slotIndex, @Nonnull final Array<String> target) {
    checkArgument(slotIndex >= 0, "slotIndex < 0");

    for(final Key key : attachments.keySet()) {
      if(key.slotIndex == slotIndex) {
        target.push(key.name);
      }
    }
  }

  public void findAttachmentsForSlot(final int slotIndex, @Nonnull final Array<Attachment> target) {
    checkArgument(slotIndex >= 0, "slotIndex < 0");

    for(final Map.Entry<Key, Attachment> entry : this.attachments.entrySet()) {
      if(entry.getKey().slotIndex == slotIndex) {
        target.push(entry.getValue());
      }
    }
  }

  public void clear() {
    KEY_POOL.retAll(attachments.keySet());
    attachments.clear();
  }

  @Nonnull
  public String name() {
    return name;
  }

  /** {@inheritDoc} */
  @Override
  @Nonnull
  public String toString() {
    return name;
  }

  /** Attach each attachment in this skin if the corresponding attachment in the old skin is currently attached. */
  void attachAll(@Nonnull final Skeleton skeleton,
                 @Nonnull final Skin oldSkin) {
    for (Map.Entry<Key, Attachment> entry : oldSkin.attachments.entrySet()) {
      int slotIndex = entry.getKey().slotIndex;
      Slot slot = skeleton.slots.get(slotIndex);
      if (slot.attachment == entry.getValue()) {
        Attachment attachment = getAttachment(slotIndex, entry.getKey().name);
        if (attachment != null) slot.attachment(attachment);
      }
    }
  }

  private static final class Key {
    @Nonnull String name = "";
    int slotIndex;
    int hashCode;

    @Nonnull
    public Key set(final int slotIndex, @Nonnull final String name) {
      this.slotIndex = slotIndex;
      this.name = name;
      hashCode = 31 * (31 + name.hashCode()) + slotIndex;
      return this;
    }

    @Override
    public int hashCode () {
      return hashCode;
    }

    @Override
    public boolean equals(Object object) {
      if(!(object instanceof Key)) {
        return false;
      }

      final Key other = (Key)object;

      return slotIndex == other.slotIndex
          && name.equals(other.name);
    }

    @Override
    public String toString () {
      return slotIndex+":"+name;
    }
  }
}
