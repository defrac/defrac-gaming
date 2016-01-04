/*
 * Copyright 2016 defrac inc.
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

package defrac.animation;

import defrac.display.Stage;
import defrac.display.event.raw.EnterFrameEvent;
import defrac.display.event.raw.Events;
import defrac.event.EventBinding;
import defrac.lang.Clock;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 * The Juggler class represents an animation system
 *
 * <p>Each Juggler instance can advance individual {@link Animatable} components in time,
 * which can be a Juggler itself. When an animatable completes by returning {@literal true}
 * it will be removed from the juggler.
 *
 * <p>The Juggler implements the {@link Clock} interface so it can be used with {@link Animation animations} and
 * other components from the defrac standard library that rely on the {@link Clock} interface. The local time
 * of the juggler will be the time accumulated by all the calls to {@link #advanceTime(double)}.
 *
 * <p>It is possible to bind a Juggler to the EnterFrame event of a {@link Stage} by simply instantiating a new
 * juggler object and calling {@link #bindToEnterFrame(Juggler, Stage)}. The resulting event binding can be paused
 * and resumed like any other binding.
 *
 * <p>The Juggler has its roots in the <a href="http://starling-framework.org">Starling</a> framework.
 */
public final class Juggler implements Animatable, Clock {
  /**
   * Creates and returns a new Juggler object for the stage
   *
   * <p>The Juggler object will be immediately bound to the EnterFrame event of the
   * stage. It won't be possible to pause the binding. If you required this feature:
   * create a juggler manually and {@link #bindToEnterFrame(Juggler, Events) bind} it to the
   * EnterFrame event instead.
   *
   * @param stage The stage to use
   * @return The new juggler bound to the stage
   */
  public static Juggler forStage(@Nonnull final Stage stage) {
    final Juggler juggler = new Juggler();
    bindToEnterFrame(juggler, stage);
    return juggler;
  }

  /**
   * Binds a juggler to the EnterFrame event of a stage
   *
   * @param juggler The juggler to bind to the stage
   * @param stage The stage providing the EnterFrame event
   * @return The resulting event binding
   */
  @Nonnull
  public static EventBinding<EnterFrameEvent> bindToEnterFrame(@Nonnull final Juggler juggler,
                                                               @Nonnull final Stage stage) {
    return bindToEnterFrame(juggler, stage.globalEvents());
  }

  /**
   * Binds a juggler to the EnterFrame event of an event system
   *
   * @param juggler The juggler to bind to the stage
   * @param events The event system providing the EnterFrame event
   * @return The resulting event binding
   */
  @Nonnull
  public static EventBinding<EnterFrameEvent> bindToEnterFrame(@Nonnull final Juggler juggler,
                                                               @Nonnull final Events events) {
    return events.onEnterFrame.add(event -> juggler.advanceTime(event.deltaTimeSec));
  }

  private static final double SEC_TO_MS = 1e3;
  private static final double SEC_TO_NS = 1e9;

  private double localTimeSec = 0.0;

  @Nonnull
  private final HashSet<Animatable> animatables = new HashSet<>();

  public Juggler() {}

  /**
   * Adds an animatable object to the juggler
   *
   * @param animatable The animatable object to add
   * @return {@literal true} if the object has been added; {@literal false} if it is already being animated by the juggler
   */
  public boolean add(@Nonnull final Animatable animatable) {
    return this != animatable && animatables.add(animatable);
  }

  /**
   * Removes an animatable object from the juggler
   *
   * @param animatable The animatable object to remove
   * @return {@literal true} if the object has been removed; {@literal false} if isn't contained in this juggler
   */
  public boolean remove(@Nonnull final Animatable animatable) {
    return this != animatable && animatables.remove(animatable);
  }

  /**
   * Whether or not the juggler contains the animatable object
   *
   * @param animatable The animatable object
   * @return {@literal true} if the object is contained; {@literal false} otherwise
   */
  public boolean contains(@Nonnull final Animatable animatable) {
    return animatables.contains(animatable);
  }

  /**
   * Removes all objects from the juggler
   *
   * <p>Do not call this method when inside {@link #advanceTime(double)} since it will
   * cause a {@link java.util.ConcurrentModificationException}
   *
   * @return This juggler
   */
  @Nonnull
  public Juggler clear() {
    animatables.clear();
    return this;
  }

  /** {@inheritDoc} */
  public boolean advanceTime(final double deltaTimeSec) {
    localTimeSec += deltaTimeSec;

    final Iterator<Animatable> iter = animatables.iterator();

    while(iter.hasNext()) {
      final Animatable animatable = iter.next();

      if(!animatable.advanceTime(deltaTimeSec)) {
        iter.remove();
      }
    }

    return true;
  }

  /** {@inheritDoc} */
  @Override
  public long timeNs() {
    return Math.round(localTimeSec * SEC_TO_NS);
  }

  /** {@inheritDoc} */
  @Override
  public long timeMs() {
    return Math.round(localTimeSec * SEC_TO_MS);
  }

  /** {@inheritDoc} */
  @Override
  public double timeSec() {
    return localTimeSec;
  }

  /** {@inheritDoc} */
  @Override
  public long time(@Nonnull TimeUnit timeUnit) {
    return timeUnit.convert(Math.round(localTimeSec), TimeUnit.SECONDS);
  }
}
