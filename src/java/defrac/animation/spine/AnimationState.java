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

import defrac.event.Listeners;
import defrac.pool.ObjectPool;
import defrac.pool.ObjectPools;
import defrac.util.Array;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static defrac.lang.Preconditions.checkArgument;

/** Stores state for an animation and automatically mixes between animations. */
public final class AnimationState {
  @Nonnull
  private static final ObjectPool<TrackEntry> TRACK_ENTRY_POOL =
      ObjectPools.newPool(TrackEntry::new);

  @Nonnull
  private final AnimationStateData data;

  @Nonnull
  private final Array<Event> events = new Array<>();

  @Nonnull
  private final Listeners<Listener> listeners = Listeners.create();

  @Nonnull
  private final Array<TrackEntry> tracks = new Array<>();

  private float timeScale = 1.0f;

  public AnimationState(@Nonnull final AnimationStateData data) {
    this.data = data;
  }

  /**
   * @param delta Elapsed time since last update in seconds
   */
  public void update(float delta) {
    delta *= timeScale;

    final int trackCount = tracks.size();

    for(int trackIndex = 0; trackIndex < trackCount; trackIndex++) {
      final TrackEntry current = tracks.get(trackIndex);

      if(current == null) {
        continue;
      }

      current.time += delta * current.timeScale;

      if(current.previous != null) {
        final float previousDelta = delta * current.previous.timeScale;
        current.previous.time += previousDelta;
        current.mixTime += previousDelta;
      }

      final TrackEntry next = current.next;

      if(next != null) {
        next.time = current.lastTime - next.delay;

        if(next.time >= 0) {
          setCurrent(trackIndex, next);
        }
      } else {
        // End non-looping animation when it reaches its end time and there is no next entry.
        if(!current.loop && current.lastTime >= current.endTime) {
          clearTrack(trackIndex);
        }
      }
    }
  }

  public void apply(@Nonnull final Skeleton skeleton) {
    final Array<Event> events = this.events;
    final int trackCount = tracks.size();

    for(int trackIndex = 0; trackIndex < trackCount; trackIndex++) {
      final TrackEntry current = tracks.get(trackIndex);

      if(current == null) {
        continue;
      }

      events.clear();

      float time = current.time;
      final float lastTime = current.lastTime;
      final float endTime = current.endTime;
      final boolean loop = current.loop;

      if(!loop && time > endTime) {
        time = endTime;
      }

      final TrackEntry previous = current.previous;

      if(previous == null) {
        assert current.animation != null;
        current.animation.mix(skeleton, lastTime, time, loop, events, current.mix);
      } else {
        float previousTime = previous.time;

        if(!previous.loop && previousTime > previous.endTime) {
          previousTime = previous.endTime;
        }

        assert previous.animation != null;
        previous.animation.apply(skeleton, previousTime, previousTime, previous.loop, null);

        float alpha = current.mixTime / current.mixDuration * current.mix;

        if(alpha >= 1.0f) {
          alpha = 1.0f;
          TRACK_ENTRY_POOL.ret(previous);
          current.previous = null;
        }

        assert current.animation != null;
        current.animation.mix(skeleton, lastTime, time, loop, events, alpha);
      }

      for(final Event event : events) {
        if(current.listener != null) {
          current.listener.event(trackIndex, event);
        }

        for(final Listener listener : listeners) {
          listener.event(trackIndex, event);
        }
      }

      // Check if completed the animation or a loop iteration.
      if(loop ? (lastTime % endTime > time % endTime) : (lastTime < endTime && time >= endTime)) {
        final int count = (int)(time / endTime);

        if(current.listener != null) {
          current.listener.complete(trackIndex, count);
        }

        for(final Listener listener : listeners) {
          listener.complete(trackIndex, count);
        }
      }

      current.lastTime = current.time;
    }
  }

  public void clearTracks() {
    final int trackCount = tracks.size();

    for(int trackIndex = 0; trackIndex < trackCount; trackIndex++) {
      clearTrack(trackIndex);
    }

    tracks.clear();
  }

  public void clearTrack(final int trackIndex) {
    if(trackIndex < 0 || trackIndex >= tracks.size()) {
      return;
    }

    final TrackEntry current = tracks.get(trackIndex);

    if(current == null) {
      return;
    }

    if(current.listener != null) {
      current.listener.end(trackIndex);
    }

    for(final Listener listener : listeners) {
      listener.end(trackIndex);
    }

    tracks.set(trackIndex, null);

    freeAll(current);

    if(current.previous != null) {
      TRACK_ENTRY_POOL.ret(current.previous);
    }
  }

  private void freeAll(@Nullable TrackEntry entry) {
    while(entry != null) {
      final TrackEntry next = entry.next;
      TRACK_ENTRY_POOL.ret(entry);
      entry = next;
    }
  }

  @Nullable
  private TrackEntry expandToIndex(final int index) {
    if(index < tracks.size()) {
      return tracks.get(index);
    }

    tracks.size(index + 1);

    return null;
  }

  private void setCurrent(final int index, @Nonnull  final TrackEntry entry) {
    final TrackEntry current = expandToIndex(index);

    if(current != null) {
      TrackEntry previous = current.previous;
      current.previous = null;

      if(current.listener != null) {
        current.listener.end(index);
      }

      for(final Listener listener : listeners) {
        listener.end(index);
      }

      entry.mixDuration = data.getMix(current.animation, entry.animation);

      if(entry.mixDuration > 0) {
        entry.mixTime = 0;

        // If a mix is in progress, mix from the closest animation.
        if(previous != null && current.mixTime / current.mixDuration < 0.5f) {
          entry.previous = previous;
          previous = current;
        } else {
          entry.previous = current;
        }
      } else {
        TRACK_ENTRY_POOL.ret(current);
      }

      if(previous != null) {
        TRACK_ENTRY_POOL.ret(previous);
      }
    }

    tracks.set(index, entry);

    if(entry.listener != null) {
      entry.listener.start(index);
    }

    for(final Listener listener : listeners) {
      listener.start(index);
    }
  }

  /** @see #setAnimation(int, Animation, boolean) */
  @Nonnull
  public TrackEntry setAnimation(final int trackIndex,
                                 @Nonnull final String animationName,
                                 final boolean loop) {
    final Animation animation = data.skeletonData().findAnimation(animationName);

    checkArgument(animation != null, "Animation not found: " + animationName);

    //noinspection ConstantConditions
    return setAnimation(trackIndex, animation, loop);
  }

  /** Set the current animation. Any queued animations are cleared. */
  @Nonnull
  public TrackEntry setAnimation(final int trackIndex,
                                 @Nonnull final Animation animation,
                                 final boolean loop) {
    final TrackEntry current = expandToIndex(trackIndex);

    if(current != null) {
      freeAll(current.next);
    }

    final TrackEntry entry = TRACK_ENTRY_POOL.get();

    entry.animation = animation;
    entry.loop = loop;
    entry.endTime = animation.duration();

    setCurrent(trackIndex, entry);

    return entry;
  }

  /** {@link #addAnimation(int, Animation, boolean, float)} */
  public TrackEntry addAnimation(final int trackIndex,
                                 @Nonnull final String animationName,
                                 final boolean loop,
                                 final float delay) {
    final Animation animation = data.skeletonData().findAnimation(animationName);

    checkArgument(animation != null, "Animation not found: " + animationName);

    //noinspection ConstantConditions
    return addAnimation(trackIndex, animation, loop, delay);
  }

  /** Adds an animation to be played delay seconds after the current or last queued animation.
   * @param delay May be <= 0 to use duration of previous animation minus any mix duration plus the negative delay. */
  @Nonnull
  public TrackEntry addAnimation(final int trackIndex,
                                 @Nonnull final Animation animation,
                                 final boolean loop,
                                 float delay) {
    final TrackEntry entry = TRACK_ENTRY_POOL.get();

    entry.animation = animation;
    entry.loop = loop;
    entry.endTime = animation.duration();

    TrackEntry last = expandToIndex(trackIndex);

    if(last != null) {
      while (last.next != null) {
        last = last.next;
      }

      last.next = entry;
    } else {
      tracks.set(trackIndex, entry);
    }

    if(delay <= 0.0f) {
      if(last != null) {
        delay += last.endTime - data.getMix(last.animation, animation);
      } else {
        delay = 0.0f;
      }
    }

    entry.delay = delay;

    return entry;
  }

  /** @return May be null. */
  @Nullable
  public TrackEntry current(final int trackIndex) {
    return (trackIndex < 0 || trackIndex >= tracks.size())
        ? null
        : tracks.get(trackIndex);
  }

  /** Adds a listener to receive events for all animations. */
  public void addListener(@Nonnull final Listener listener) {
    listeners.add(listener);
  }

  /** Removes the listener added with {@link #addListener(Listener)}. */
  public void removeListener(@Nullable Listener listener) {
    if(listener == null) {
      return;
    }

    listeners.remove(listener);
  }

  public float timeScale() {
    return timeScale;
  }

  public void timeScale(final float value) {
    timeScale = value;
  }

  @Nonnull
  public AnimationStateData data() {
    return data;
  }

  /** Returns the list of tracks that have animations, which may contain nulls. */
  @Nonnull
  public Array<TrackEntry> tracks() {
    return tracks;
  }

  @Override
  @Nonnull
  public String toString() {
    final StringBuilder buffer = new StringBuilder(64).append("[AnimationState tracks: {");

    boolean isFirst = true;
    for(final TrackEntry entry : tracks) {
      if(entry == null) {
        continue;
      }

      if(!isFirst) {
        buffer.append(", ");
      } else {
        isFirst = false;
      }

      buffer.append(entry.toString());
    }

    return buffer.append("}]").toString();
  }

  public static class TrackEntry {
    @Nullable
    TrackEntry next;

    @Nullable
    TrackEntry previous;

    @Nullable
    Animation animation;

    @Nullable
    Listener listener;

    boolean loop;
    float delay, time, lastTime = -1, endTime, timeScale = 1;
    float mixTime, mixDuration;
    float mix = 1;

    public void reset() {
      next = null;
      previous = null;
      animation = null;
      listener = null;
      timeScale = 1;
      lastTime = -1; // Trigger events on frame zero.
      time = 0;
    }

    @Nullable
    public Animation animation() {
      return animation;
    }

    public void animation(@Nullable final Animation value) {
      animation = value;
    }

    public boolean loop() {
      return loop;
    }

    public void loop(final boolean value) {
      loop = value;
    }

    public float delay() {
      return delay;
    }

    public void delay(final float value) {
      delay = value;
    }

    public float time() {
      return time;
    }

    public void time(final float value) {
      time = value;
    }

    public float endTime() {
      return endTime;
    }

    public void endTime(final float value) {
      endTime = value;
    }

    @Nullable
    public Listener listener() {
      return listener;
    }

    public void listener(@Nullable final Listener value) {
      listener = value;
    }

    public float lastTime() {
      return lastTime;
    }

    public void lastTime(final float value) {
      lastTime = value;
    }

    public float mix() {
      return mix;
    }

    public void mix(final float mix) {
      this.mix = mix;
    }

    public float timeScale() {
      return timeScale;
    }

    public void timeScale(final float value) {
      timeScale = value;
    }

    @Nullable
    public TrackEntry next() {
      return next;
    }

    public void next(@Nullable final TrackEntry next) {
      this.next = next;
    }

    /** Returns true if the current time is greater than the end time, regardless of looping. */
    public boolean isComplete() {
      return time >= endTime;
    }

    @Nonnull
    @Override
    public String toString() {
      return animation == null ? "[TrackEntry]" : "[TrackEntry animation: "+animation.name+']';
    }
  }

  public interface Listener {
    /** Invoked when the current animation triggers an event. */
    void event(int trackIndex, Event event);

    /** Invoked when the current animation has completed.
     * @param loopCount The number of times the animation reached the end. */
    void complete(int trackIndex, int loopCount);

    /** Invoked just after the current animation is set. */
    void start(int trackIndex);

    /** Invoked just before the current animation is replaced. */
    void end(int trackIndex);
  }

  public static abstract class SimpleListener implements Listener {
    /** {@inheritDoc} */
    @Override
    public void event(int trackIndex, Event event) {}

    /** {@inheritDoc} */
    @Override
    public void complete(int trackIndex, int loopCount) {}

    /** {@inheritDoc} */
    @Override
    public void start(int trackIndex) {}

    /** {@inheritDoc} */
    @Override
    public void end(int trackIndex) {}
  }
}
