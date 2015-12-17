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

package defrac.display;

import defrac.display.event.raw.EnterFrameEvent;
import defrac.display.particle.ParticleStrategy;
import defrac.display.render.RenderContent;
import defrac.display.render.Renderer;
import defrac.gl.GLMatrix;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 *
 */
public class ParticleSystem extends DisplayObject implements OnEnterFrameReceiver {
  @Nullable
  private ParticleStrategy strategy;

  public ParticleSystem(@Nonnull final ParticleStrategy strategy) {
    this.strategy(strategy);
  }

  @Override
  protected void onAttachToStage() {
    super.onAttachToStage();

    final Stage stage = stage();

    assert stage != null;

    initAABB(0.0f, 0.0f, stage.width(), stage.height());
  }

  @Nullable
  public ParticleStrategy strategy() {
    return strategy;
  }

  @Nonnull
  public ParticleSystem strategy(@Nullable final ParticleStrategy value) {
    final ParticleStrategy oldValue = strategy;

    if(value == oldValue) {
      return this;
    }

    strategy = value;

    invalidate(DisplayObjectFlags.RENDERLIST_DIRTY);

    return this;
  }

  /** {@inheritDoc} */
  @Override
  public void onEnterFrame(@Nonnull final EnterFrameEvent event) {
    if(strategy == null) {
      return;
    }

    strategy.update(event);

    invalidate(DisplayObjectFlags.RENDERLIST_MATRIX_DIRTY);
  }

  /** {@inheritDoc} */
  @Nullable
  @Override
  public RenderContent render(@Nonnull final GLMatrix projectionMatrix,
                              @Nonnull final GLMatrix modelViewMatrix,
                              @Nonnull final Renderer renderer,
                              @Nonnull final BlendMode parentBlendMode,
                              final float parentAlpha) {
    return strategy == null
        ? null
        : strategy.render(
              projectionMatrix,
              modelViewMatrix,
              renderer,
              blendMode.inherit(parentBlendMode),
              parentAlpha * alpha);
  }
}
