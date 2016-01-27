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

package defrac.display.atlas.libgdx;

import defrac.display.Texture;
import defrac.display.TextureData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 *
 */
final class LibgdxAtlasRegion {
  private static final int F_X = 0;
  private static final int F_Y = 1;
  private static final int F_ORIGINAL_WIDTH = 2;
  private static final int F_ORIGINAL_HEIGHT = 3;
  private static final int F_ROTATE = 4;
  private static final int F_OFFSET_X = 5;
  private static final int F_OFFSET_Y = 6;
  private static final int F_WIDTH = 7;
  private static final int F_HEIGHT = 8;
  private static final int F_INDEX = 9;

  @Nonnull
  private final int[] fields = new int[10];

  @Nullable
  public LibgdxAtlasRegion next;

  public LibgdxAtlasRegion(final boolean rotate,
                           final int x, final int y,
                           final int width, final int height,
                           final int originalWidth, final int originalHeight,
                           final int offsetX, final int offsetY,
                           final int index) {

    fields[F_X] = x;
    fields[F_Y] = y;
    fields[F_ORIGINAL_WIDTH] = originalWidth;
    fields[F_ORIGINAL_HEIGHT] = originalHeight;
    fields[F_ROTATE] = rotate ? 1 : 0;
    fields[F_OFFSET_X] = offsetX;
    fields[F_OFFSET_Y] = offsetY;
    fields[F_WIDTH] = rotate ? height : width;
    fields[F_HEIGHT] = rotate ? width : height;
    fields[F_INDEX] = index;
  }

  public int index() {
    return fields[F_INDEX];
  }

  @Nonnull
  public Texture toTexture(@Nonnull final TextureData textureData) {
    return new Texture(
        textureData,
        fields[F_X], fields[F_Y],
        fields[F_ORIGINAL_WIDTH], fields[F_ORIGINAL_HEIGHT],
        fields[F_ROTATE],
        fields[F_OFFSET_X], fields[F_OFFSET_Y],
        fields[F_WIDTH], fields[F_HEIGHT]);
  }
}
