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
import defrac.display.particle.ParticleSystemStrategy;
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
  private ParticleSystemStrategy strategy;

  public ParticleSystem(@Nonnull final ParticleSystemStrategy strategy) {
    this.strategy(strategy);
  }

  @Override
  protected void onAttachToStage() {
    super.onAttachToStage();

    final Stage stage = stage();

    initAABB(0.0f, 0.0f, stage.width(), stage.height());
  }

  @Override
  protected void onDetachFromStage() {
    if(strategy != null) {
      // Prevent memory leak and give the strategy an opportunity
      // to clear cached RenderContent
      strategy.clearRenderContent();
    }
  }

  @Nullable
  public ParticleSystemStrategy strategy() {
    return strategy;
  }

  @Nonnull
  public ParticleSystem strategy(@Nullable final ParticleSystemStrategy value) {
    final ParticleSystemStrategy oldValue = strategy;

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
    if(strategy == null || !strategy.active()) {
      return;
    }

    invalidate(strategy.invalidationFlags());
  }

  /** {@inheritDoc} */
  @Nullable
  @Override
  public RenderContent render(@Nonnull final GLMatrix projectionMatrix,
                              @Nonnull final GLMatrix modelViewMatrix,
                              @Nonnull final Renderer renderer,
                              @Nonnull final BlendMode parentBlendMode,
                              final float parentAlpha,
                              final float pixelRatio) {
    return strategy == null
        ? null
        : strategy.render(
              projectionMatrix,
              modelViewMatrix,
              renderer,
              blendMode.inherit(parentBlendMode),
              parentAlpha * alpha,
              pixelRatio);
  }
}
