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

public final class IkConstraintData {
  @Nonnull
  final String name;

  @Nonnull
  final Array<BoneData> bones = new Array<>();

  BoneData target;
  int bendDirection = 1;
  float mix = 1;

  public IkConstraintData(@Nonnull final String name) {
    this.name = name;
  }

  @Nonnull
  public String name() {
    return name;
  }

  @Nonnull
  public Array<BoneData> bones() {
    return bones;
  }

  public BoneData target() {
    return target;
  }

  public void target(final BoneData value) {
    target = value;
  }

  public int bendDirection() {
    return bendDirection;
  }

  public void bendDirection(final int value) {
    bendDirection = value;
  }

  public float mix() {
    return mix;
  }

  public void mix(final float value) {
    mix = value;
  }

  @Override
  @Nonnull
  public String toString() {
    return name;
  }
}