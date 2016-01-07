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

import defrac.concurrent.Future;
import defrac.concurrent.Futures;
import defrac.concurrent.Promise;
import defrac.concurrent.Promises;
import defrac.display.*;
import defrac.geom.Point;
import defrac.gl.GL;
import defrac.lang.Strings;
import defrac.util.Color;
import defrac.xml.XML;
import defrac.xml.XMLAttribute;
import defrac.xml.XMLElement;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * The SystemOptions class represents the configuration of a PartcileDesigner particle system
 *
 * <p>ParticleDesigner system options are usually stored in {@code *.pex} files. Those files
 * may be loaded via the {@link defrac.xml.XML} class.
 *
 * <p><strong>Textures:</strong> The SystemOptions class will <strong>not</strong> load base-64 encoded
 * and gzipped textures embedded in a PEX file. You must either provide the texture manually or
 * a {@code <texture name="myfile.png"/>} node exists inside the PEX file. If such a node exists a
 * given {@link TextureDataSupply} is asked to supply the texture.
 */
public final class SystemOptions {
  private static final int DEFAULT_MAX_PARTICLES = 10;
  private static final float DEFAULT_PARTICLE_LIFESPAN = 1.0f;
  private static final float DEFAULT_PARTICLE_LIFESPAN_VARIANCE = 0.0f;
  private static final float DEFAULT_ANGLE = 0.0f;
  private static final float DEFAULT_ANGLE_VARIANCE = 0.0f;
  private static final float DEFAULT_START_PARTICLE_SIZE = 20.0f;
  private static final float DEFAULT_START_PARTICLE_SIZE_VARIANCE = 0.0f;
  private static final float DEFAULT_FINISH_PARTICLE_SIZE = 20.0f;
  private static final float DEFAULT_FINISH_PARTICLE_SIZE_VARIANCE = 0.0f;
  private static final float DEFAULT_ROTATION_START = 0.0f;
  private static final float DEFAULT_ROTATION_START_VARIANCE = 0.0f;
  private static final float DEFAULT_ROTATION_END = 0.0f;
  private static final float DEFAULT_ROTATION_END_VARIANCE = 0.0f;
  private static final float DEFAULT_DURATION = 2.0f;
  private static final float DEFAULT_SPEED = 50.0f;
  private static final float DEFAULT_SPEED_VARIANCE = 0.0f;
  private static final float DEFAULT_RADIAL_ACCELERATION = 0.0f;
  private static final float DEFAULT_RADIAL_ACCELERATION_VARIANCE = 0.0f;
  private static final float DEFAULT_TANGENTIAL_ACCELERATION = 0.0f;
  private static final float DEFAULT_TANGENTIAL_ACCELERATION_VARIANCE = 0.0f;
  private static final float DEFAULT_MAX_RADIUS = 100.0f;
  private static final float DEFAULT_MAX_RADIUS_VARIANCE = 0.0f;
  private static final float DEFAULT_MIN_RADIUS = 0.0f;
  private static final float DEFAULT_MIN_RADIUS_VARIANCE = 0.0f;
  private static final float DEFAULT_ROTATE_PER_SECOND = 0.0f;
  private static final float DEFAULT_ROTATE_PER_SECOND_VARIANCE = 0.0f;
  private static final boolean DEFAULT_EXACT_AABB = false;

  public EmitterType emitterType = EmitterType.GRAVITY;

  int maxParticles = DEFAULT_MAX_PARTICLES;

  @Nonnull
  public final Point source = new Point();

  @Nonnull
  public final Point sourceVariance = new Point();

  public float particleLifespan = DEFAULT_PARTICLE_LIFESPAN;
  public float particleLifeSpanVariance = DEFAULT_PARTICLE_LIFESPAN_VARIANCE;
  public float angle = DEFAULT_ANGLE;
  public float angleVariance = DEFAULT_ANGLE_VARIANCE;

  public float startParticleSize = DEFAULT_START_PARTICLE_SIZE;
  public float startParticleSizeVariance = DEFAULT_START_PARTICLE_SIZE_VARIANCE;
  public float finishParticleSize = DEFAULT_FINISH_PARTICLE_SIZE;
  public float finishParticleSizeVariance = DEFAULT_FINISH_PARTICLE_SIZE_VARIANCE;
  public float rotationStart = DEFAULT_ROTATION_START;
  public float rotationStartVariance = DEFAULT_ROTATION_START_VARIANCE;
  public float rotationEnd = DEFAULT_ROTATION_END;
  public float rotationEndVariance = DEFAULT_ROTATION_END_VARIANCE;

  public float duration = DEFAULT_DURATION;

  @Nonnull
  public final Point gravity = new Point();

  public float speed = DEFAULT_SPEED;
  public float speedVariance = DEFAULT_SPEED_VARIANCE;

  public float radialAcceleration = DEFAULT_RADIAL_ACCELERATION;
  public float radialAccelerationVariance = DEFAULT_RADIAL_ACCELERATION_VARIANCE;
  public float tangentialAcceleration = DEFAULT_TANGENTIAL_ACCELERATION;
  public float tangentialAccelerationVariance = DEFAULT_TANGENTIAL_ACCELERATION_VARIANCE;

  public float maxRadius = DEFAULT_MAX_RADIUS;
  public float maxRadiusVariance = DEFAULT_MAX_RADIUS_VARIANCE;
  public float minRadius = DEFAULT_MIN_RADIUS;
  public float minRadiusVariance = DEFAULT_MIN_RADIUS_VARIANCE;
  public float rotatePerSecond = DEFAULT_ROTATE_PER_SECOND;
  public float rotatePerSecondVariance = DEFAULT_ROTATE_PER_SECOND_VARIANCE;

  @Nonnull
  public final Color startColor = new Color(0.0f, 0.0f, 0.0f, 0.0f);
  @Nonnull
  public final Color startColorVariance = new Color(0.0f, 0.0f, 0.0f, 0.0f);
  @Nonnull
  public final Color finishColor = new Color(0.0f, 0.0f, 0.0f, 0.0f);
  @Nonnull
  public final Color finishColorVariance = new Color(0.0f, 0.0f, 0.0f, 0.0f);

  public BlendMode blendMode = BlendMode.INHERIT;

  public Texture texture;

  public boolean exactAABB = DEFAULT_EXACT_AABB;

  @Nonnull
  public static Future<SystemOptions> fromPEX(@Nonnull final Future<XML> pex,
                                              @Nonnull final TextureDataSupply textureDataSupply) {
    return pex.flatMap(xml -> SystemOptions.fromPEX(xml, textureDataSupply));
  }

  @Nonnull
  public static Future<SystemOptions> fromPEX(@Nonnull final XML pex,
                                              @Nonnull final TextureDataSupply textureDataSupply) {
    final XMLElement textureElement = pex.root().firstChild("texture");

    if(textureElement == null) {
      return Futures.failure(new IOException("Missing <texture/> element"));
    }

    if(textureElement.attribute("data") != null) {
      return Futures.failure(new UnsupportedOperationException("Gzipped texture isn't supported"));
    }

    final XMLAttribute name = textureElement.attribute("name");

    if(name == null || Strings.isNullOrEmpty(name.stringValue())) {
      return Futures.failure(new IOException("Missing name of texture"));
    }

    final Promise<SystemOptions> promise = Promises.create();

    try {
      final Future<TextureData> textureDataFuture =
          textureDataSupply.get(
              name.stringValue(),
              TextureDataFormat.RGBA,
              TextureDataRepeat.NO_REPEAT,
              TextureDataSmoothing.LINEAR,
              false);

      promise.
          completeWith(
              textureDataFuture.map(textureData -> fromPEX(pex, new Texture(textureData))));
    } catch(final Exception exception) {
      promise.failure(exception);
    }

    return promise.future();
  }

  @SuppressWarnings("ConstantConditions")
  @Nonnull
  public static SystemOptions fromPEX(@Nonnull final XML pex,
                                      @Nonnull final Texture texture) {
    return fromPEX((XMLElement)pex.root(), texture);
  }

  @SuppressWarnings("ConstantConditions")
  @Nonnull
  public static SystemOptions fromPEX(@Nonnull final XMLElement pex,
                                      @Nonnull final Texture texture) {
    final SystemOptions result = new SystemOptions();

    result.texture = texture;

    readPoint(result.source, pex.firstChild("sourcePosition"));
    readPoint(result.sourceVariance, pex.firstChild("sourcePositionVariance"));

    result.speed = readFloat(pex, "speed", DEFAULT_SPEED);
    result.speedVariance = readFloat(pex, "speedVariance", DEFAULT_SPEED_VARIANCE);

    result.particleLifespan = readFloat(pex, "particleLifeSpan", DEFAULT_PARTICLE_LIFESPAN);
    result.particleLifeSpanVariance = readFloat(pex, "particleLifespanVariance", DEFAULT_PARTICLE_LIFESPAN_VARIANCE);

    result.angle = readFloat(pex, "angle", DEFAULT_ANGLE);
    result.angleVariance = readFloat(pex, "angleVariance", DEFAULT_ANGLE_VARIANCE);

    readPoint(result.gravity, pex.firstChild("gravity"));

    result.radialAcceleration = readFloat(pex, "radialAcceleration", DEFAULT_RADIAL_ACCELERATION);
    result.radialAccelerationVariance = readFloat(pex, "radialAccelVariance", DEFAULT_RADIAL_ACCELERATION_VARIANCE);

    result.tangentialAcceleration = readFloat(pex, "tangentialAcceleration", DEFAULT_TANGENTIAL_ACCELERATION);
    result.tangentialAccelerationVariance = readFloat(pex, "tangentialAccelVariance", DEFAULT_TANGENTIAL_ACCELERATION_VARIANCE);

    readColor(result.startColor, pex.firstChild("startColor"));
    readColor(result.startColorVariance, pex.firstChild("startColorVariance"));

    readColor(result.finishColor, pex.firstChild("finishColor"));
    readColor(result.finishColorVariance, pex.firstChild("finishColorVariance"));

    result.maxParticles = pex.firstChild("maxParticles").attribute("value").intValue();

    result.startParticleSize = readFloat(pex, "startParticleSize", DEFAULT_START_PARTICLE_SIZE);
    result.startParticleSizeVariance = readFloat(pex, "startParticleSizeVariance", DEFAULT_START_PARTICLE_SIZE_VARIANCE);

    if(pex.firstChild("finishParticleSize") != null) {
      result.finishParticleSize = readFloat(pex, "finishParticleSize", DEFAULT_FINISH_PARTICLE_SIZE);
    } else {
      // Startling *.pex files contain "FinishParticleSize" instead of "finishParticleSize"
      result.finishParticleSize = readFloat(pex, "FinishParticleSize", DEFAULT_FINISH_PARTICLE_SIZE);
    }
    result.finishParticleSizeVariance = readFloat(pex, "finishParticleSizeVariance", DEFAULT_FINISH_PARTICLE_SIZE_VARIANCE);

    result.duration = readFloat(pex, "duration", DEFAULT_DURATION);

    result.emitterType = EmitterType.valueOf(pex.firstChild("emitterType").attribute("value").intValue());

    result.maxRadius = readFloat(pex, "maxRadius", DEFAULT_MAX_RADIUS);
    result.maxRadiusVariance = readFloat(pex, "maxRadiusVariance", DEFAULT_MAX_RADIUS_VARIANCE);

    result.minRadius = readFloat(pex, "minRadius", DEFAULT_MIN_RADIUS);
    result.minRadiusVariance = readFloat(pex, "minRadiusVariance", DEFAULT_MIN_RADIUS_VARIANCE);

    result.rotatePerSecond = readFloat(pex, "rotatePerSecond", DEFAULT_ROTATE_PER_SECOND);
    result.rotatePerSecondVariance = readFloat(pex, "rotatePerSecondVariance", DEFAULT_ROTATE_PER_SECOND_VARIANCE);

    final int blendFuncSource = pex.firstChild("blendFuncSource").attribute("value").intValue();
    final int blendFuncDestination = pex.firstChild("blendFuncDestination").attribute("value").intValue();

    result.blendMode = mapToBlendMode(blendFuncSource, blendFuncDestination);

    result.rotationStart = readFloat(pex, "rotationStart", DEFAULT_ROTATION_START);
    result.rotationStartVariance = readFloat(pex, "rotationStartVariance", DEFAULT_ROTATION_START_VARIANCE);

    result.rotationEnd = readFloat(pex, "rotationEnd", DEFAULT_ROTATION_END);
    result.rotationEndVariance = readFloat(pex, "rotationEndVariance", DEFAULT_ROTATION_END_VARIANCE);

    return result;
  }

  private static float readFloat(@Nonnull final XMLElement pex,
                                 @Nonnull final String name,
                                 final float defaultValue) {
    final XMLElement element = pex.firstChild(name);

    if(element == null) {
      return defaultValue;
    }

    final XMLAttribute attr = element.attribute("value");

    if(attr == null) {
      return defaultValue;
    }

    try {
      return attr.floatValue();
    } catch(final NumberFormatException numberFormatException) {
      return defaultValue;
    }
  }

  @SuppressWarnings("ConstantConditions")
  private static void readPoint(@Nonnull final Point target, @Nonnull final XMLElement element) {
    target.x = element.attribute("x").floatValue();
    target.y = element.attribute("y").floatValue();
  }

  @SuppressWarnings("ConstantConditions")
  private static void readColor(@Nonnull final Color target,
                                @Nonnull final XMLElement element) {
    target.r = element.attribute("red").floatValue();
    target.g = element.attribute("green").floatValue();
    target.b = element.attribute("blue").floatValue();
    target.a = element.attribute("alpha").floatValue();
  }

  @Nonnull
  private static BlendMode mapToBlendMode(final int src, final int dst) {
    if(src == GL.ONE) {
      if(dst == GL.ONE_MINUS_SRC_ALPHA) {
        return BlendMode.NORMAL;
      } else if(dst == GL.ONE) {
        return BlendMode.ADD;
      } else if(dst == GL.ONE_MINUS_SRC_COLOR) {
        return BlendMode.SCREEN;
      }
    } else if(src == GL.DST_COLOR) {
      if(dst == GL.ONE_MINUS_SRC_ALPHA) {
        return BlendMode.MULTIPLY;
      }
    }

    return new BlendMode(src, dst);
  }

  public SystemOptions() {}

  public float emissionRate() {
    return maxParticles / particleLifespan;
  }
}
