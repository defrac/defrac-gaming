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

import defrac.display.TextureDataFormat;
import defrac.display.TextureDataRepeat;
import defrac.display.TextureDataSmoothing;
import defrac.lang.Maps;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * @hide
 */
final class LibgdxAtlasPage {
  @Nonnull
  public final String path;

  @Nonnull
  public  final TextureDataFormat format;

  @Nonnull
  public  final TextureDataRepeat repeat;

  @Nonnull
  public  final TextureDataSmoothing smoothing;

  @Nonnull
  public final Map<String, LibgdxAtlasRegion> mapping;

  public LibgdxAtlasPage(@Nonnull final String path,
                         @Nonnull final TextureDataFormat format,
                         @Nonnull final TextureDataRepeat repeat,
                         @Nonnull final TextureDataSmoothing smoothing) {
    this.path = path;
    this.format = format;
    this.repeat = repeat;
    this.smoothing = smoothing;
    this.mapping = Maps.newHashMap();
  }

  public void add(@Nonnull final String name,
                  @Nonnull final LibgdxAtlasRegion region) {
    final LibgdxAtlasRegion existing = mapping.put(name, region);

    if(existing != null) {
      region.next = existing;
    }
  }
}
