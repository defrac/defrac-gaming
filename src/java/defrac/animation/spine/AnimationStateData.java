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

import defrac.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

import static defrac.lang.Preconditions.checkArgument;

/** Stores mixing times between animations. */
public final class AnimationStateData {
  @Nonnull
  private static final Key LOOKUP_KEY = new Key();

  @Nonnull
  final Map<Key, Float> animationToMixTime = new HashMap<>();

  @Nonnull
  private final SkeletonData skeletonData;

  float defaultMix;

  public AnimationStateData(@Nonnull final SkeletonData skeletonData) {
    this.skeletonData = skeletonData;
  }

  @Nonnull
  public SkeletonData skeletonData() {
    return skeletonData;
  }

  public void setMix(@Nonnull final String fromName,
                     @Nonnull final String toName,
                     final float duration) {
    final Animation from = skeletonData.findAnimation(fromName);
    final Animation to = skeletonData.findAnimation(toName);

    checkArgument(from != null, "Animation not found: " + fromName);
    checkArgument(to != null, "Animation not found: " + toName);

    //noinspection ConstantConditions
    setMix(from, to, duration);
  }

  public void setMix(@Nonnull final Animation from,
                     @Nonnull final Animation to,
                     final float duration) {
    final Key key = new Key();
    key.from = from;
    key.to = to;
    animationToMixTime.put(key, duration);
  }

  public float getMix(@Nullable final Animation from,
                      @Nullable final Animation to) {
    LOOKUP_KEY.from = from;
    LOOKUP_KEY.to = to;

    final Float result = animationToMixTime.get(LOOKUP_KEY);

    return result == null
        ? defaultMix
        : result;
  }

  public float defaultMix() {
    return defaultMix;
  }

  public void defaultMix(final float value) {
    this.defaultMix = value;
  }

  static class Key {
    Animation from, to;

    @Override
    public int hashCode() {
      return 31 * (31 + from.hashCode()) + to.hashCode();
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(@Nullable Object obj) {
      if(this == obj) {
        return true;
      }

      if(obj == null) {
        return false;
      }

      final Key that = (Key)obj;

      return Objects.equal(this.from, that.from)
          && Objects.equal(this.to, that.to);
    }
  }
}
