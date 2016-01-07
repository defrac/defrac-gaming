/*
 * Copyright 2015 defrac inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package defrac.display.particle;

import defrac.animation.Animatable;
import defrac.display.BlendMode;
import defrac.display.DisplayObjectFlags;
import defrac.display.render.RenderContent;
import defrac.display.render.Renderer;
import defrac.gl.GLMatrix;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 *
 */
public interface ParticleStrategy extends Animatable {
  boolean emitParticle();

  default int emitParticles(final int numParticles) {
    for(int i = 0; i < numParticles; ++i) {
      if(!emitParticle()) {
        return i;
      }
    }

    return numParticles;
  }

  void clearRenderContent();

  /** Creates and returns render content for the display list */
  @Nullable
  RenderContent render(
      @Nonnull final GLMatrix projectionMatrix,
      @Nonnull final GLMatrix modelViewMatrix,
      @Nonnull final Renderer renderer,
      @Nonnull final BlendMode parentBlendMode,
      final float parentAlpha,
      final float pixelRatio);

  /** Whether or not the particle system is active */
  boolean active();

  ParticleStrategy active(final boolean value);

  /** The invalidation flags for the display list while this particle system is active */
  default int invalidationFlags() {
    return DisplayObjectFlags.RENDERLIST_MATRIX_DIRTY;
  }
}
