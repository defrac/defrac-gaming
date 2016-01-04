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

/**
 *
 */
public interface Animatable {
  /**
   * Advances the time of the animation
   *
   * @param deltaTimeSec The elapsed time in seconds
   * @return {@literal true} if the animation completed; {@literal false} otherwise
   */
  boolean advanceTime(double deltaTimeSec);
}
