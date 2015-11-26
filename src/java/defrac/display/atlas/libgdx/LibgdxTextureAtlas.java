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

import defrac.concurrent.Future;
import defrac.concurrent.Futures;
import defrac.display.TextureAtlas;
import defrac.display.TextureData;
import defrac.display.TextureDataSupply;
import defrac.lang.Closeables;
import defrac.resource.StringResource;
import defrac.util.Array;

import javax.annotation.Nonnull;
import javax.annotation.WillNotClose;
import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.Map;

/**
 *
 */
public final class LibgdxTextureAtlas {
  @Nonnull
  public static Future<TextureAtlas> load(@Nonnull final StringResource formatResource,
                                          @Nonnull final TextureDataSupply supply) {
    return load(formatResource.load(), supply);
  }

  @Nonnull
  public static Future<TextureAtlas> load(@Nonnull final Future<String> format,
                                          @Nonnull final TextureDataSupply supply) {
    return format.flatMap(data -> load(data, supply));
  }

  @Nonnull
  public static Future<TextureAtlas> load(@Nonnull final String format,
                                          @Nonnull final TextureDataSupply supply) {
    Reader reader = null;

    try {
      reader = new StringReader(format);
      return load(reader, supply);
    } finally {
      Closeables.closeQuietly(reader);
    }
  }

  @Nonnull
  public static Future<TextureAtlas> load(@WillNotClose @Nonnull final Reader reader,
                                          @Nonnull final TextureDataSupply supply) {
    final LibgdxAtlasScanner scanner = LibgdxAtlasScanner.create(reader);
    final LibgdxAtlasParser parser = LibgdxAtlasParser.create(scanner);
    final Array<LibgdxAtlasPage> listOfAtlasPages = parser.parseAtlas();

    @SuppressWarnings("unchecked")
    final Future<TextureData>[] futuresOfTextureData = new Future[listOfAtlasPages.size()];
    int pageIndex = 0;

    for(final LibgdxAtlasPage page : listOfAtlasPages) {
      futuresOfTextureData[pageIndex++] =
          supply.get(page.path, page.format, page.repeat, page.smoothing, /*persistent=*/false);
    }

    return Futures.joinAll(futuresOfTextureData).map(listOfTextureData -> {
      final TextureAtlas.Builder builder = TextureAtlas.newBuilder();
      final Iterator<TextureData> textureDataIter = listOfTextureData.iterator();

      for(final LibgdxAtlasPage page : listOfAtlasPages) {
        assert textureDataIter.hasNext();
        final TextureData textureData = textureDataIter.next();

        convertPage(builder, page, textureData);
      }

      return builder.build();
    });
  }

  @Nonnull
  public static TextureAtlas parse(@Nonnull final String format,
                                   @Nonnull final Map<String, TextureData> supply) {
    Reader reader = null;

    try {
      reader = new StringReader(format);
      return parse(reader, supply);
    } finally {
      Closeables.closeQuietly(reader);
    }
  }

  @Nonnull
  public static TextureAtlas parse(@WillNotClose @Nonnull final Reader reader,
                                   @Nonnull final Map<String, TextureData> supply) {
    final LibgdxAtlasScanner scanner = LibgdxAtlasScanner.create(reader);
    final LibgdxAtlasParser parser = LibgdxAtlasParser.create(scanner);
    final Array<LibgdxAtlasPage> listOfAtlasPages = parser.parseAtlas();
    final TextureAtlas.Builder builder = TextureAtlas.newBuilder();

    for(final LibgdxAtlasPage page : listOfAtlasPages) {
      final TextureData textureData = supply.get(page.path);
      convertPage(builder, page, textureData);
    }

    return builder.build();
  }

  private static void convertPage(@Nonnull final TextureAtlas.Builder builder,
                                  @Nonnull final  LibgdxAtlasPage page,
                                  @Nonnull final  TextureData textureData) {
    for(final Map.Entry<String, LibgdxAtlasRegion> entry : page.mapping.entrySet()) {
      final String name = entry.getKey();
      LibgdxAtlasRegion region = entry.getValue();

      while(region != null) {
        final int index = region.index();

        if(index == -1) {
          builder.add(name, region.toTexture(textureData));
        } else {
          builder.add(name, region.toTexture(textureData), index);
        }

        region = region.next;
      }
    }
  }

  private LibgdxTextureAtlas() {}
}
