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

package defrac.animation.spine.attachments;

import defrac.animation.spine.Slot;
import defrac.display.Texture;
import defrac.util.MathUtil;

import javax.annotation.Nonnull;

import static defrac.lang.Preconditions.checkState;

/**
 * Attachment that displays various texture regions over time.
 */
public final class RegionSequenceAttachment extends RegionAttachment {
  private Mode mode;
  private float frameTime;
  private Texture[] regions;

  public RegionSequenceAttachment(String name) {
    super(name);
  }

  @Override
  public void updateWorldVertices(final float skeletonX,
                                  final float skeletonY,
                                  @Nonnull final Slot slot) {

    checkState(regions != null, "Regions have not been set");

    int frameIndex = (int)(slot.attachmentTime() / frameTime);

    switch(mode) {
      case FORWARD:
        frameIndex = Math.min(regions.length - 1, frameIndex);
        break;
      case FORWARD_LOOP:
        frameIndex = frameIndex % regions.length;
        break;
      case PING_PONG:
        frameIndex = frameIndex % (regions.length * 2);
        if (frameIndex >= regions.length) frameIndex = regions.length - 1 - (frameIndex - regions.length);
        break;
      case RANDOM:
        frameIndex = MathUtil.random(regions.length);
        break;
      case BACKWARD:
        frameIndex = Math.max(regions.length - frameIndex - 1, 0);
        break;
      case BACKWARD_LOOP:
        frameIndex = frameIndex % regions.length;
        frameIndex = regions.length - frameIndex - 1;
        break;
    }

    region(regions[frameIndex]);

    super.updateWorldVertices(skeletonX, skeletonY, slot);
  }

  @Nonnull
  public Texture[] regions() {
    checkState(regions != null, "Regions have not been set");
    return regions;
  }

  public void regions(@Nonnull final Texture[] regions) {
    this.regions = regions;
  }

  /**
   * Sets the time in seconds each frame is shown.
   */
  public void frameTime(final float frameTime) {
    this.frameTime = frameTime;
  }

  public void mode(@Nonnull final Mode mode) {
    this.mode = mode;
  }

  public enum Mode {
    FORWARD,
    BACKWARD,
    FORWARD_LOOP,
    BACKWARD_LOOP,
    PING_PONG,
    RANDOM
  }
}