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
import defrac.event.EventDispatcher;
import defrac.gl.GLMatrix;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 *
 */
public interface ParticleSystemStrategy extends Animatable {
  /**
   * Emits a single particle
   *
   * <p>Calling {@code emitParticle} has no effect if the
   * maximum capacity of the particle system has been reached.
   * In that case {@literal false} is returned.
   *
   * @return {@literal true} if the particle has been emitted; {@literal false} otherwise
   */
  boolean emitParticle();

  /**
   * Emits a given number of particles
   *
   * <p>The particle system tries to emit as many as {@code numParticles} particles
   * but it might reach its maximum capacity while doing so. In that case the return
   * value of this method is the actual number of particles emitted.
   *
   * @param numParticles The number of particles to emit
   * @return The actual number of particles emitted
   */
  default int emitParticles(final int numParticles) {
    for(int i = 0; i < numParticles; ++i) {
      if(!emitParticle()) {
        return i;
      }
    }

    return numParticles;
  }

  /**
   * Clears any cached render-content associated wit this strategy
   *
   * <p>User-code shouldn't call this method since a particle system
   * may assume that its render content is valid until {@link #render(GLMatrix, GLMatrix, Renderer, BlendMode, float, float)}
   * is being called.
   *
   * @hide
   */
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

  @Nonnull
  ParticleSystemStrategy active(final boolean value);

  /** The invalidation flags for the display list while this particle system is active */
  default int invalidationFlags() {
    return DisplayObjectFlags.RENDERLIST_MATRIX_DIRTY;
  }

  /**
   * The event when the system stops
   */
  @Nonnull
  EventDispatcher<ParticleSystemStrategy> onStop();

  /**
   * Resets the system to its initial state
   */
  @Nonnull
  ParticleSystemStrategy reset();
}
