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

package defrac.display.particle.particleDesigner;

import defrac.animation.AnimationSystem;
import defrac.display.BlendMode;
import defrac.display.particle.ParticleSystemStrategy;
import defrac.display.render.RenderContent;
import defrac.display.render.Renderer;
import defrac.event.EventDispatcher;
import defrac.gl.GLMatrix;
import defrac.util.MathUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The ParticleDesignerStrategy class is a strategy for ParticleDesigner particle systems
 */
public final class ParticleDesignerStrategy implements ParticleSystemStrategy {
  private static final int COORDINATES_EACH_PARTICLE = 4;
  private static final int VERTICES_EACH_PARTICLE = COORDINATES_EACH_PARTICLE * 2;
  private static final int COLORS_EACH_PARTICLE = COORDINATES_EACH_PARTICLE * 4;
  private static final int UVS_EACH_PARTICLE = COORDINATES_EACH_PARTICLE * 2;
  private static final int TRIANGLES_EACH_PARTICLE = 2;

  @Nonnull
  private final Particle[] particles;

  @Nonnull
  private final float[] vertices;

  @Nonnull
  private final float[] uvs;

  @Nonnull
  private final float[] colors;

  @Nonnull
  private final short[] indices;

  private int particleCount;

  private final int numVertices;

  private final int numTriangles;

  @Nonnull
  private final ParticleDesignerSettings settings;

  @Nullable
  private RenderContent content;

  private boolean active = true;

  private float emissionRate = 0.0f;

  private float totalTimeSec = 0.0f;

  private float emitCounter = 0.0f;

  @Nonnull
  private final EventDispatcher<ParticleSystemStrategy> onStop = EventDispatcher.create();

  public ParticleDesignerStrategy(@Nonnull final ParticleDesignerSettings settings) {
    final int particleCount = settings.maxParticles;

    this.settings = settings;
    this.particles = new Particle[particleCount];

    emissionRate = settings.emissionRate();

    numVertices = particleCount * COORDINATES_EACH_PARTICLE;
    numTriangles = particleCount * TRIANGLES_EACH_PARTICLE;

    vertices = new float[numVertices * 2];
    uvs = new float[particleCount * UVS_EACH_PARTICLE];
    colors = new float[particleCount * COLORS_EACH_PARTICLE];
    indices = new short[numTriangles * 3];

    int uvIndex = 0;
    int indicesIndex = 0;
    int vertexOffset = 0;

    final float uv00u = settings.texture.uv00u;
    final float uv00v = settings.texture.uv00v;
    final float uv10u = settings.texture.uv10u;
    final float uv10v = settings.texture.uv10v;
    final float uv11u = settings.texture.uv11u;
    final float uv11v = settings.texture.uv11v;
    final float uv01u = settings.texture.uv01u;
    final float uv01v = settings.texture.uv01v;

    for(int i = 0; i < particleCount; ++i) {
      particles[i] = new Particle();

      uvs[uvIndex++] = uv00u;
      uvs[uvIndex++] = uv00v;

      uvs[uvIndex++] = uv10u;
      uvs[uvIndex++] = uv10v;

      uvs[uvIndex++] = uv11u;
      uvs[uvIndex++] = uv11v;

      uvs[uvIndex++] = uv01u;
      uvs[uvIndex++] = uv01v;

      indices[indicesIndex++] = (short)(vertexOffset);
      indices[indicesIndex++] = (short)(vertexOffset + 1);
      indices[indicesIndex++] = (short)(vertexOffset + 3);

      indices[indicesIndex++] = (short)(vertexOffset + 1);
      indices[indicesIndex++] = (short)(vertexOffset + 2);
      indices[indicesIndex++] = (short)(vertexOffset + 3);

      vertexOffset += 4;
    }
  }

  @Override
  public void clearRenderContent() {
    content = null;
  }

  @Nullable
  @Override
  public RenderContent render(@Nonnull GLMatrix projectionMatrix,
                              @Nonnull GLMatrix modelViewMatrix,
                              @Nonnull Renderer renderer,
                              @Nonnull BlendMode parentBlendMode,
                              float parentAlpha,
                              final float pixelRatio) {
    if(content != null) {
      return content;
    }

    content = renderer.drawTexture(
        projectionMatrix,
        modelViewMatrix,
        parentAlpha,
        settings.blendMode.inherit(parentBlendMode),
        settings.texture.textureData,
        vertices, 0,
        uvs, 0,
        colors, 0,
        indices, 0,
        numVertices,
        numTriangles);

    return content;
  }

  @Override
  public boolean active() {
    return active;
  }

  @Nonnull
  @Override
  public ParticleSystemStrategy active(boolean value) {
    active = value;
    return this;
  }

  @Nonnull
  public ParticleSystemStrategy reset() {
    totalTimeSec = 0.0f;
    emitCounter = 0;
    active(true);


    for(final Particle particle : particles) {
      particle.timeToLive = 0.0f;
    }

    return this;
  }

  @Nonnull
  @Override
  public EventDispatcher<ParticleSystemStrategy> onStop() {
    return onStop;
  }

  @Override
  public boolean advanceTime(@Nonnull final AnimationSystem system,
                             final double deltaTimeSec) {
    final float dt = (float)deltaTimeSec;

    if(active && emissionRate != 0.0f) {
      final float rate = 1.0f / emissionRate;

      if(particleCount < settings.maxParticles) {
        emitCounter += dt;
      }

      while(particleCount < settings.maxParticles && emitCounter > rate) {
        emitParticle();
        emitCounter -= rate;
      }

      totalTimeSec += dt;

      if(settings.duration > 0 && totalTimeSec >= settings.duration) {
        active(false);
        onStop.dispatch(this);
      }
    }

    for(int i = 0; i < particleCount;) {
      final Particle currentParticle = particles[i];

      currentParticle.timeToLive -= dt;

      if(currentParticle.timeToLive > 0.0f) {
        updateParticle(currentParticle, i, dt);
        ++i;
      } else {
        final int lastParticleIndex = particleCount - 1;

        if(i != lastParticleIndex) {
          // Two options here we should benchmark:
          //
          // (1) Scramble the array and simply swap particles[i] with particles[lastParticleIndex]
          // (2) Assign particle[i] the values from particles[lastParticleIndex]
          //
          // Option (1) sounds good at first but bashes the locality
          // whereas option (2) needs more assignments but keeps locality
          particles[i].copyFrom(particles[lastParticleIndex]);
        }

        --particleCount;
      }
    }

    return true;
  }

  private void updateParticle(@Nonnull final Particle particle, final int index, final float dt) {
    { // UPDATE
      if(settings.emitterType == EmitterType.RADIAL) {
        particle.angleRad += particle.radiansPerSecond * dt;
        particle.radius += particle.radiusDelta * dt;

        particle.posX = settings.source.x - MathUtil.cos(particle.angleRad) * particle.radius;
        particle.posY = settings.source.y - MathUtil.sin(particle.angleRad) * particle.radius;

        if (particle.radius < settings.minRadius) {
          particle.timeToLive = 0;
        }
      } else {
        final float dx = particle.posX - particle.startX;
        final float dy = particle.posY - particle.startY;
        float d = (float)Math.sqrt(dx * dx + dy * dy);
        final float nx;
        final float ny;

        if(d < 0.001f) {
          d = 0.001f;
        }

        nx = dx / d;
        ny = dy / d;

        float tx = ny * particle.tangentAccel, ty = nx * particle.tangentAccel;
        float rx = nx * particle.radialAccel , ry = ny * particle.radialAccel;

        particle.velX += dt * (settings.gravity.x + rx + tx);
        particle.velY += dt * (settings.gravity.y + ry + ty);
        particle.posX += particle.velX * dt;
        particle.posY += particle.velY * dt;
      }

      particle.colorR += particle.colorDeltaR * dt;
      particle.colorG += particle.colorDeltaG * dt;
      particle.colorB += particle.colorDeltaB * dt;
      particle.colorA += particle.colorDeltaA * dt;

      particle.size += particle.sizeDelta * dt;

      if(particle.size < 0f) {
        particle.size = 0.0f;
      }

      particle.rotationRad += particle.rotationDelta * dt;
    }

    { // RENDER
      int vertexIndex = index * VERTICES_EACH_PARTICLE;
      int colorIndex  = index * COLORS_EACH_PARTICLE;

      final float a = particle.colorA;
      final float r = particle.colorR * a;
      final float g = particle.colorG * a;
      final float b = particle.colorB * a;

      for (int j = 0; j < 4; ++j) {
        colors[colorIndex++] = r;
        colors[colorIndex++] = g;
        colors[colorIndex++] = b;
        colors[colorIndex++] = a;
      }

      final float halfSize = particle.size * 0.5f;
      final float rotation = particle.rotationRad;
      final float x = particle.posX;
      final float y = particle.posY;

      if(rotation != 0.0f) {
        final float tL = -halfSize;
        @SuppressWarnings("UnnecessaryLocalVariable")
        final float bR =  halfSize;
        final float cos = MathUtil.cos(rotation);
        final float sin = MathUtil.sin(rotation);

        vertices[vertexIndex++] = tL * cos - tL * sin + x;
        vertices[vertexIndex++] = tL * sin + tL * cos + y;

        vertices[vertexIndex++] = bR * cos - tL * sin + x;
        vertices[vertexIndex++] = bR * sin + tL * cos + y;

        vertices[vertexIndex++] = bR * cos - bR * sin + x;
        vertices[vertexIndex++] = bR * sin + bR * cos + y;

        vertices[vertexIndex++] = tL * cos - bR * sin + x;
        vertices[vertexIndex  ] = tL * sin + bR * cos + y;
      } else {
        final float top = y - halfSize;
        final float right = x + halfSize;
        final float bottom = y + halfSize;
        final float left = x - halfSize;

        vertices[vertexIndex++] = left;
        vertices[vertexIndex++] = top;

        vertices[vertexIndex++] = right;
        vertices[vertexIndex++] = top;

        vertices[vertexIndex++] = right;
        vertices[vertexIndex++] = bottom;

        vertices[vertexIndex++] = left;
        vertices[vertexIndex  ] = bottom;
      }
    }
  }

  @Override
  public boolean emitParticle() {
    if(particleCount == settings.maxParticles) {
      return false;
    }

    final Particle particle = particles[particleCount];

    particle.posX = settings.source.x + random(settings.sourceVariance.x);
    particle.posY = settings.source.y + random(settings.sourceVariance.y);

    particle.startX = settings.source.x;
    particle.startY = settings.source.y;

    final float direction = MathUtil.degToRad(settings.angle + random(settings.angleVariance));
    final float speed = settings.speed + random(settings.speedVariance);

    particle.velX = MathUtil.cos(direction) * speed;
    particle.velY = MathUtil.sin(direction) * speed;

    final float timeToLive = Math.max(0.0f, settings.particleLifespan + random(settings.particleLifeSpanVariance));

    particle.timeToLive = timeToLive;

    final float startRadius = settings.maxRadius + random(settings.maxRadiusVariance);
    final float endRadius = settings.minRadius + random(settings.minRadiusVariance);

    particle.radius = startRadius;
    particle.radiusDelta = (endRadius - startRadius) / timeToLive;

    particle.angleRad = MathUtil.degToRad(settings.angle + random(settings.angleVariance));
    particle.radiansPerSecond = MathUtil.degToRad(settings.rotatePerSecond + random(settings.rotatePerSecondVariance));

    particle.radialAccel = settings.radialAcceleration + random(settings.radialAccelerationVariance);
    particle.tangentAccel = settings.tangentialAcceleration + random(settings.tangentialAccelerationVariance);

    final float startSize = settings.startParticleSize + random(settings.startParticleSizeVariance);
    final float finishSize = settings.finishParticleSize + random(settings.finishParticleSizeVariance);

    particle.size = Math.max(0.0f, startSize);
    particle.sizeDelta = (finishSize - startSize) / timeToLive;

    float r0 = settings.startColor.r + random(settings.startColorVariance.r);
    float g0 = settings.startColor.g + random(settings.startColorVariance.g);
    float b0 = settings.startColor.b + random(settings.startColorVariance.b);
    float a0 = settings.startColor.a + random(settings.startColorVariance.a);

    float r1 = settings.finishColor.r + random(settings.finishColorVariance.r);
    float g1 = settings.finishColor.g + random(settings.finishColorVariance.g);
    float b1 = settings.finishColor.b + random(settings.finishColorVariance.b);
    float a1 = settings.finishColor.a + random(settings.finishColorVariance.a);

    particle.colorR = r0;
    particle.colorG = g0;
    particle.colorB = b0;
    particle.colorA = a0;

    particle.colorDeltaR = (r1 - r0) / timeToLive;
    particle.colorDeltaG = (g1 - g0) / timeToLive;
    particle.colorDeltaB = (b1 - b0) / timeToLive;
    particle.colorDeltaA = (a1 - a0) / timeToLive;

    final float rotationStart = settings.rotationStart + random(settings.rotationStartVariance);
    final float rotationEnd = settings.rotationEnd + random(settings.rotationEndVariance);

    particle.rotationRad = MathUtil.degToRad(rotationStart);
    particle.rotationDelta = MathUtil.degToRad((rotationEnd - rotationStart) / timeToLive);

    ++particleCount;

    return true;
  }

  private static float random(final float value) {
    return (float)(Math.random() * 2.0 - 1.0) * value;
  }

  private static class Particle {
    float posX;
    float posY;

    float velX;
    float velY;

    float startX;
    float startY;

    float colorR, colorG, colorB, colorA;
    float colorDeltaR, colorDeltaG, colorDeltaB, colorDeltaA;

    float rotationRad;
    float rotationDelta;

    float radialAccel;
    float tangentAccel;

    float radius;
    float radiusDelta;

    float angleRad;
    float radiansPerSecond;

    float size;
    float sizeDelta;

    float timeToLive;

    void copyFrom(@Nonnull final Particle that) {
      this.posX = that.posX;
      this.posY = that.posY;
      this.velX = that.velX;
      this.velY = that.velY;

      this.startX = that.startX;
      this.startY = that.startY;

      this.colorR = that.colorR;
      this.colorG = that.colorG;
      this.colorB = that.colorB;
      this.colorA = that.colorA;

      this.colorDeltaR = that.colorDeltaR;
      this.colorDeltaG = that.colorDeltaG;
      this.colorDeltaB = that.colorDeltaB;
      this.colorDeltaA = that.colorDeltaA;

      this.rotationRad = that.rotationRad;
      this.rotationDelta = that.rotationDelta;

      this.radialAccel = that.radialAccel;
      this.tangentAccel = that.tangentAccel;

      this.radius = that.radius;
      this.radiusDelta = that.radiusDelta;

      this.angleRad = that.angleRad;
      this.radiansPerSecond = that.radiansPerSecond;

      this.size = that.size;
      this.sizeDelta = that.sizeDelta;

      this.timeToLive = that.timeToLive;
    }
  }
}
