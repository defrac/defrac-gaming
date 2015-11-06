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

/** Stores state for an animation and automatically mixes between animations. */
public final class AnimationState {
  private final AnimationStateData data;
  private final Array<Event> events = new Array<>();
  private final Listeners<AnimationStateListener> listeners = Listeners.create();
  private Array<TrackEntry> tracks = new Array<>();
  private float timeScale = 1;

  private ObjectPool<TrackEntry> trackEntryPool = ObjectPools.newPool(TrackEntry::new);

  public AnimationState(AnimationStateData data) {
    if (data == null) throw new IllegalArgumentException("data cannot be null.");
    this.data = data;
  }

  public void update(float delta) {
    delta *= timeScale;
    for (int i = 0; i < tracks.size(); i++) {
      TrackEntry current = tracks.get(i);
      if (current == null) continue;

      current.time += delta * current.timeScale;
      if (current.previous != null) {
        float previousDelta = delta * current.previous.timeScale;
        current.previous.time += previousDelta;
        current.mixTime += previousDelta;
      }

      TrackEntry next = current.next;
      if (next != null) {
        next.time = current.lastTime - next.delay;
        if (next.time >= 0) setCurrent(i, next);
      } else {
        // End non-looping animation when it reaches its end time and there is no next entry.
        if (!current.loop && current.lastTime >= current.endTime) clearTrack(i);
      }
    }
  }

  public void apply(Skeleton skeleton) {
    Array<Event> events = this.events;
    int listenerCount = listeners.size();

    for (int i = 0; i < tracks.size(); i++) {
      TrackEntry current = tracks.get(i);
      if (current == null) continue;

      //FIXME: evaluate whats going on here and why they set size = 0
      events.clear();

      float time = current.time;
      float lastTime = current.lastTime;
      float endTime = current.endTime;
      boolean loop = current.loop;
      if (!loop && time > endTime) time = endTime;

      TrackEntry previous = current.previous;
      if (previous == null)
        current.animation.mix(skeleton, lastTime, time, loop, events, current.mix);
      else {
        float previousTime = previous.time;
        if (!previous.loop && previousTime > previous.endTime) previousTime = previous.endTime;
        previous.animation.apply(skeleton, previousTime, previousTime, previous.loop, null);

        float alpha = current.mixTime / current.mixDuration * current.mix;
        if (alpha >= 1) {
          alpha = 1;
          trackEntryPool.ret(previous);
          current.previous = null;
        }
        current.animation.mix(skeleton, lastTime, time, loop, events, alpha);
      }

      for (int ii = 0, nn = events.size(); ii < nn; ii++) {
        Event event = events.get(ii);
        if (current.listener != null) current.listener.event(i, event);
        for (final AnimationStateListener listener : listeners)
          listener.event(i, event);
      }

      // Check if completed the animation or a loop iteration.
      if (loop ? (lastTime % endTime > time % endTime) : (lastTime < endTime && time >= endTime)) {
        int count = (int) (time / endTime);
        if (current.listener != null) current.listener.complete(i, count);
        for (final AnimationStateListener listener : listeners)
          listener.complete(i, count);
      }

      current.lastTime = current.time;
    }
  }

  public void clearTracks() {
    for (int i = 0, n = tracks.size(); i < n; i++)
      clearTrack(i);
    tracks.clear();
  }

  public void clearTrack(int trackIndex) {
    if (trackIndex >= tracks.size()) return;
    TrackEntry current = tracks.get(trackIndex);
    if (current == null) return;

    if (current.listener != null) current.listener.end(trackIndex);
    for (final AnimationStateListener listener : listeners)
      listener.end(trackIndex);

    tracks.set(trackIndex, null);

    freeAll(current);
    if (current.previous != null) trackEntryPool.ret(current.previous);
  }

  private void freeAll(TrackEntry entry) {
    while (entry != null) {
      TrackEntry next = entry.next;
      trackEntryPool.ret(entry);
      entry = next;
    }
  }

  private TrackEntry expandToIndex(int index) {
    if (index < tracks.size()) return tracks.get(index);
    tracks.size(index + 1);
    return null;
  }

  private void setCurrent(int index, TrackEntry entry) {
    TrackEntry current = expandToIndex(index);
    if (current != null) {
      TrackEntry previous = current.previous;
      current.previous = null;

      if (current.listener != null) current.listener.end(index);
      for (final AnimationStateListener listener : listeners)
        listener.end(index);

      entry.mixDuration = data.getMix(current.animation, entry.animation);
      if (entry.mixDuration > 0) {
        entry.mixTime = 0;
        // If a mix is in progress, mix from the closest animation.
        if (previous != null && current.mixTime / current.mixDuration < 0.5f) {
          entry.previous = previous;
          previous = current;
        } else
          entry.previous = current;
      } else
        trackEntryPool.ret(current);

      if (previous != null) trackEntryPool.ret(previous);
    }

    tracks.set(index, entry);

    if (entry.listener != null) entry.listener.start(index);
    for (final AnimationStateListener listener : listeners)
      listener.start(index);
  }

  /** @see #setAnimation(int, Animation, boolean) */
  public TrackEntry setAnimation(int trackIndex, String animationName, boolean loop) {
    Animation animation = data.getSkeletonData().findAnimation(animationName);
    if (animation == null) throw new IllegalArgumentException("Animation not found: " + animationName);
    return setAnimation(trackIndex, animation, loop);
  }

  /** Set the current animation. Any queued animations are cleared. */
  public TrackEntry setAnimation(int trackIndex, Animation animation, boolean loop) {
    TrackEntry current = expandToIndex(trackIndex);
    if (current != null) freeAll(current.next);

    TrackEntry entry = trackEntryPool.get();
    entry.animation = animation;
    entry.loop = loop;
    entry.endTime = animation.getDuration();
    setCurrent(trackIndex, entry);
    return entry;
  }

  /** {@link #addAnimation(int, Animation, boolean, float)} */
  public TrackEntry addAnimation(int trackIndex, String animationName, boolean loop, float delay) {
    Animation animation = data.getSkeletonData().findAnimation(animationName);
    if (animation == null) throw new IllegalArgumentException("Animation not found: " + animationName);
    return addAnimation(trackIndex, animation, loop, delay);
  }

  /** Adds an animation to be played delay seconds after the current or last queued animation.
   * @param delay May be <= 0 to use duration of previous animation minus any mix duration plus the negative delay. */
  public TrackEntry addAnimation(int trackIndex, Animation animation, boolean loop, float delay) {
    TrackEntry entry = trackEntryPool.get();
    entry.animation = animation;
    entry.loop = loop;
    entry.endTime = animation.getDuration();

    TrackEntry last = expandToIndex(trackIndex);
    if (last != null) {
      while (last.next != null)
        last = last.next;
      last.next = entry;
    } else
      tracks.set(trackIndex, entry);

    if (delay <= 0) {
      if (last != null)
        delay += last.endTime - data.getMix(last.animation, animation);
      else
        delay = 0;
    }
    entry.delay = delay;

    return entry;
  }

  /** @return May be null. */
  public TrackEntry getCurrent(int trackIndex) {
    if (trackIndex >= tracks.size()) return null;
    return tracks.get(trackIndex);
  }

  /** Adds a listener to receive events for all animations. */
  public void addListener(AnimationStateListener listener) {
    if (listener == null) throw new IllegalArgumentException("listener cannot be null.");
    listeners.add(listener);
  }

  /** Removes the listener added with {@link #addListener(AnimationStateListener)}. */
  public void removeListener(AnimationStateListener listener) {
    listeners.remove(listener);
  }

  public float getTimeScale() {
    return timeScale;
  }

  public void setTimeScale(float timeScale) {
    this.timeScale = timeScale;
  }

  public AnimationStateData getData() {
    return data;
  }

  /** Returns the list of tracks that have animations, which may contain nulls. */
  public Array<TrackEntry> getTracks() {
    return tracks;
  }

  public String toString() {
    StringBuilder buffer = new StringBuilder(64);
    for (int i = 0, n = tracks.size(); i < n; i++) {
      TrackEntry entry = tracks.get(i);
      if (entry == null) continue;
      if (buffer.length() > 0) buffer.append(", ");
      buffer.append(entry.toString());
    }
    if (buffer.length() == 0) return "<none>";
    return buffer.toString();
  }

  public interface AnimationStateListener {
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

  static public class TrackEntry {
    TrackEntry next, previous;
    Animation animation;
    boolean loop;
    float delay, time, lastTime = -1, endTime, timeScale = 1;
    float mixTime, mixDuration;
    AnimationStateListener listener;
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

    public Animation getAnimation() {
      return animation;
    }

    public void setAnimation(Animation animation) {
      this.animation = animation;
    }

    public boolean getLoop() {
      return loop;
    }

    public void setLoop(boolean loop) {
      this.loop = loop;
    }

    public float getDelay() {
      return delay;
    }

    public void setDelay(float delay) {
      this.delay = delay;
    }

    public float getTime() {
      return time;
    }

    public void setTime(float time) {
      this.time = time;
    }

    public float getEndTime() {
      return endTime;
    }

    public void setEndTime(float endTime) {
      this.endTime = endTime;
    }

    public AnimationStateListener getListener() {
      return listener;
    }

    public void setListener(AnimationStateListener listener) {
      this.listener = listener;
    }

    public float getLastTime() {
      return lastTime;
    }

    public void setLastTime(float lastTime) {
      this.lastTime = lastTime;
    }

    public float getMix() {
      return mix;
    }

    public void setMix(float mix) {
      this.mix = mix;
    }

    public float getTimeScale() {
      return timeScale;
    }

    public void setTimeScale(float timeScale) {
      this.timeScale = timeScale;
    }

    public TrackEntry getNext() {
      return next;
    }

    public void setNext(TrackEntry next) {
      this.next = next;
    }

    /** Returns true if the current time is greater than the end time, regardless of looping. */
    public boolean isComplete() {
      return time >= endTime;
    }

    public String toString() {
      return animation == null ? "<none>" : animation.name;
    }
  }

  static public abstract class AnimationStateAdapter implements AnimationStateListener {
    public void event(int trackIndex, Event event) {
    }

    public void complete(int trackIndex, int loopCount) {
    }

    public void start(int trackIndex) {
    }

    public void end(int trackIndex) {
    }
  }
}
