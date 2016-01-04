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

import defrac.display.BlendMode;
import defrac.display.particle.ParticleStrategy;
import defrac.display.render.RenderContent;
import defrac.display.render.Renderer;
import defrac.gl.GLMatrix;
import defrac.util.MathUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;

/**
 *
 */
public final class ParticleDesignerStrategy implements ParticleStrategy {
  private static final int COORDINATES_EACH_PARTICLE = 4;
  private static final int VERTICES_EACH_PARTICLE = COORDINATES_EACH_PARTICLE * 2;
  private static final int COLORS_EACH_PARTICLE = COORDINATES_EACH_PARTICLE * 4;
  private static final int UVS_EACH_PARTICLE = COORDINATES_EACH_PARTICLE * 2;
  private static final int TRIANGLES_EACH_PARTICLE = 2;

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

    public void copyFrom(@Nonnull final Particle that) {
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
  private final SystemOptions options;

  @Nullable
  private RenderContent content;

  private boolean active = true;

  private boolean cleanDeadParticles = false;

  private float emissionRate;
  private float emitCounter = 0;

  public ParticleDesignerStrategy(@Nonnull final SystemOptions options) {
    final int particleCount = options.maxParticles;

    this.options = options;
    this.particles = new Particle[particleCount];

    emissionRate = options.emissionRate();

    numVertices = particleCount * COORDINATES_EACH_PARTICLE;
    numTriangles = particleCount * TRIANGLES_EACH_PARTICLE;

    vertices = new float[numVertices * 2];
    uvs = new float[particleCount * UVS_EACH_PARTICLE];
    colors = new float[particleCount * COLORS_EACH_PARTICLE];
    indices = new short[numTriangles * 3];

    int uvIndex = 0;
    int indicesIndex = 0;
    int vertexOffset = 0;

    final float uv00u = options.texture.uv00u;
    final float uv00v = options.texture.uv00v;
    final float uv10u = options.texture.uv10u;
    final float uv10v = options.texture.uv10v;
    final float uv11u = options.texture.uv11u;
    final float uv11v = options.texture.uv11v;
    final float uv01u = options.texture.uv01u;
    final float uv01v = options.texture.uv01v;

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

  @Nullable
  @Override
  public RenderContent render(@Nonnull GLMatrix projectionMatrix,
                              @Nonnull GLMatrix modelViewMatrix,
                              @Nonnull Renderer renderer,
                              @Nonnull BlendMode parentBlendMode,
                              float parentAlpha) {
    if(content != null) {
      return content;
    }

    content = renderer.drawTexture(
        projectionMatrix,
        modelViewMatrix,
        parentAlpha,
        options.blendMode.inherit(parentBlendMode),
        options.texture.textureData,
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

  @Override
  public ParticleStrategy active(boolean value) {
    active = value;
    return this;
  }

  @Override
  public boolean advanceTime(final double deltaTimeSec) {
    final float dt = (float)deltaTimeSec;

    if(active && emissionRate != 0.0f) {
      final float rate = 1.0f / emissionRate;

      if(particleCount < options.maxParticles) {
        emitCounter += dt;
      }

      while(particleCount < options.maxParticles && emitCounter > rate) {
        emitParticle();
        emitCounter -= rate;
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

          if(cleanDeadParticles) {
            final int vertexIndex = lastParticleIndex * VERTICES_EACH_PARTICLE;
            final int colorIndex = lastParticleIndex * COLORS_EACH_PARTICLE;
            Arrays.fill(vertices, vertexIndex, vertexIndex + 8, 0.0f);
            Arrays.fill(colors, colorIndex, colorIndex + 16, 0.0f);
          }
        }

        --particleCount;
      }
    }

    return true;
  }

  private void updateParticle(@Nonnull final Particle particle, final int index, final float dt) {
    { // UPDATE
      if(options.emitterType == EmitterType.RADIAL) {
        particle.angleRad += particle.radiansPerSecond * dt;
        particle.radius += particle.radiusDelta * dt;

        particle.posX = options.source.x - MathUtil.cos(particle.angleRad) * particle.radius;
        particle.posY = options.source.y - MathUtil.sin(particle.angleRad) * particle.radius;

        if (particle.radius < options.minRadius) {
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

        particle.velX += dt * (options.gravity.x + rx + tx);
        particle.velY += dt * (options.gravity.y + ry + ty);
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
    if(particleCount == options.maxParticles) {
      return false;
    }

    final Particle particle = particles[particleCount];

    particle.posX = options.source.x + random(options.sourceVariance.x);
    particle.posY = options.source.y + random(options.sourceVariance.y);

    particle.startX = options.source.x;
    particle.startY = options.source.y;

    final float direction = MathUtil.degToRad(options.angle + random(options.angleVariance));
    final float speed = options.speed + random(options.speedVariance);

    particle.velX = MathUtil.cos(direction) * speed;
    particle.velY = MathUtil.sin(direction) * speed;

    final float timeToLive = Math.max(0.0f, options.particleLifespan + random(options.particleLifeSpanVariance));

    particle.timeToLive = timeToLive;

    final float startRadius = options.maxRadius + random(options.maxRadiusVariance);
    final float endRadius = options.minRadius + random(options.minRadiusVariance);

    particle.radius = startRadius;
    particle.radiusDelta = (endRadius - startRadius) / timeToLive;

    particle.angleRad = MathUtil.degToRad(options.angle + random(options.angleVariance));
    particle.radiansPerSecond = MathUtil.degToRad(options.rotatePerSecond + random(options.rotatePerSecondVariance));

    particle.radialAccel = options.radialAcceleration + random(options.radialAccelerationVariance);
    particle.tangentAccel = options.tangentialAcceleration + random(options.tangentialAccelerationVariance);

    final float startSize = options.startParticleSize + random(options.startParticleSizeVariance);
    final float finishSize = options.finishParticleSize + random(options.finishParticleSizeVariance);

    particle.size = Math.max(0.0f, startSize);
    particle.sizeDelta = (finishSize - startSize) / timeToLive;

    float r0 = options.startColor.r + random(options.startColorVariance.r);
    float g0 = options.startColor.g + random(options.startColorVariance.g);
    float b0 = options.startColor.b + random(options.startColorVariance.b);
    float a0 = options.startColor.a + random(options.startColorVariance.a);

    float r1 = options.finishColor.r + random(options.finishColorVariance.r);
    float g1 = options.finishColor.g + random(options.finishColorVariance.g);
    float b1 = options.finishColor.b + random(options.finishColorVariance.b);
    float a1 = options.finishColor.a + random(options.finishColorVariance.a);

    particle.colorR = r0;
    particle.colorG = g0;
    particle.colorB = b0;
    particle.colorA = a0;

    particle.colorDeltaR = (r1 - r0) / timeToLive;
    particle.colorDeltaG = (g1 - g0) / timeToLive;
    particle.colorDeltaB = (b1 - b0) / timeToLive;
    particle.colorDeltaA = (a1 - a0) / timeToLive;

    final float rotationStart = options.rotationStart + random(options.rotationStartVariance);
    final float rotationEnd = options.rotationEnd + random(options.rotationEndVariance);

    particle.rotationRad = MathUtil.degToRad(rotationStart);
    particle.rotationDelta = MathUtil.degToRad((rotationEnd - rotationStart) / timeToLive);

    ++particleCount;

    return true;
  }

  private static float random(final float value) {
    return (float)(Math.random() * 2.0 - 1.0) * value;
  }
}
