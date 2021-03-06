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
import defrac.util.Array;

import javax.annotation.Nonnull;

import static defrac.display.atlas.libgdx.LibgdxAtlasTokens.*;

/**
 *
 */
final class LibgdxAtlasParser {
  //TODO(joa): implement peek/poll and don't advance in nextToken

  @Nonnull
  public static LibgdxAtlasParser create(@Nonnull final LibgdxAtlasScanner scanner) {
    return new LibgdxAtlasParser(scanner);
  }

  @Nonnull
  private final LibgdxAtlasScanner scanner;

  private int _0, _1;

  private LibgdxAtlasParser(@Nonnull final LibgdxAtlasScanner scanner) {
    this.scanner = scanner;
  }

  @Nonnull
  public Array<LibgdxAtlasPage> parseAtlas() {
    // The LibGDX TextureAtlas format comes with a little disadvantage for us.
    // It goes like this:
    //
    // page0
    //   region0
    //   ...
    // page1
    // ...
    // pageN
    //   ...
    //   regionN
    //
    // We don't know all pages to load in advance which means we'll have to
    // parse all regions before knowing the next page. Therefore we create
    // additional memory resources like LibgdxAtlasPage and LibgdxAtlasRegion
    // which wouldn't be necessary if we'd know the files in advance.

    final Array<LibgdxAtlasPage> pages = new Array<>(1);

    int token = nextToken();

    if(token == T_LINETERMINATOR) {
      token = nextToken();
    }

    do {
      if(token == T_IDENTIFIER) {
        pages.push(parsePage());
      } else if(token == T_EOF) {
        return pages;
      } else {
        unexpected(token);
      }

      token = nextToken();
    } while(true);
  }

  @Nonnull
  private LibgdxAtlasPage parsePage() {
    final String pageName = scanner.stringValue();
    expect(T_LINETERMINATOR);

    int token = nextToken();
    if(token == T_SIZE) {
      expect(T_COLON);
      parseInt();
      expect(T_COMMA);
      parseInt();
      expect(T_LINETERMINATOR);
      expect(T_FORMAT);
    } else {
      expect(token, T_FORMAT);
    }

    final TextureDataFormat format = parseFormat();
    expect(T_LINETERMINATOR);

    final TextureDataSmoothing filter = parseFilter();
    expect(T_LINETERMINATOR);

    final TextureDataRepeat repeat = parseRepeat();
    final int afterRepeat = nextToken();

    final LibgdxAtlasPage page =
        new LibgdxAtlasPage(pageName, format, repeat, filter);

    if(afterRepeat == T_EOF) {
      return page;
    } else if(afterRepeat != T_LINETERMINATOR) {
      unexpected(afterRepeat);
    }

    do {
      token = nextTokenGenerous();

      if(token == T_LINETERMINATOR || token == T_EOF) {
        break;
      } else if(token != T_IDENTIFIER) {
        unexpected(token);
        break;
      }

      if(parseRegion(page)) break;
    } while(true);

    return page;
  }

  /**
   * @return {@literal true} if EOF is reached; {@literal false} otherwise
   */
  private boolean parseRegion(@Nonnull final LibgdxAtlasPage page) {
    int token;

    final String name = scanner.stringValue();
    expect(T_LINETERMINATOR);

    expectKey(T_ROTATE);
    final boolean rotate = parseBoolean();
    expect(T_LINETERMINATOR);

    parsePair(T_XY);
    final int x = _0;
    final int y = _1;
    expect(T_LINETERMINATOR);

    parsePair(T_SIZE);
    final int width = _0;
    final int height = _1;
    expect(T_LINETERMINATOR);

    // split?
    token = nextToken();
    if(token == T_SPLIT) {
      // ignored
      while(token != T_LINETERMINATOR) {
        token = nextToken();
      }
    }

    // pad?
    if(token == T_PAD) {
      // ignored
      while(token != T_LINETERMINATOR) {
        token = nextToken();
      }
    }

    int originalWidth = -1;
    int originalHeight = -1;
    if(token == T_ORIG) {
      expect(T_COLON);
      originalWidth = parseInt();
      expect(T_COMMA);
      originalHeight = parseInt();
    } else {
      expect(T_ORIG, token);
    }
    expect(T_LINETERMINATOR);

    parsePair(T_OFFSET);
    final int offsetX = _0;
    final int offsetY = _1;
    expect(T_LINETERMINATOR);

    expectKey(T_INDEX);
    final int index = parseInt();

    token = nextToken();

    if(token == T_EOF) {
      return true;
    } else if(token != T_LINETERMINATOR) {
      unexpected(token);
    }

    page.add(
        name,
        new LibgdxAtlasRegion(
            rotate,
            x, y,
            width, height,
            originalWidth, originalHeight,
            offsetX, offsetY,
            index));

    return false;
  }

  @Nonnull
  private String parseIdentifier() {
    expect(T_IDENTIFIER);
    return scanner.stringValue();
  }

  private boolean parseBoolean() {
    final int token = nextToken();
    if(token == T_TRUE) {
      return true;
    } else if(token == T_FALSE) {
      return false;
    } else {
      unexpected(token);
      return false;
    }
  }

  private int parseInt() {
    expect(T_INTEGER);
    return Integer.parseInt(scanner.stringValue());
  }

  @Nonnull
  private TextureDataSmoothing parseFilter() {
    expectKey(T_FILTER);
    final String minFilter = parseIdentifier();
    expect(T_COMMA);
    final String magFilter = parseIdentifier();

    if(!"Linear".equals(minFilter) || !"Linear".equals(magFilter)) {
      return TextureDataSmoothing.NO_SMOOTHING;
    }

    return TextureDataSmoothing.LINEAR;
  }

  @Nonnull
  private TextureDataRepeat parseRepeat() {
    expectKey(T_REPEAT);

    final int token = nextToken();

    if(token == T_NONE) {
      return TextureDataRepeat.NO_REPEAT;
    }

    boolean repeatX = token == T_X || token == T_XY;
    boolean repeatY = token == T_Y || token == T_XY;

    return TextureDataRepeat.valueOf(repeatX, repeatY);
  }

  private void parsePair(final int token) {
    expectKey(token);
    _0 = parseInt();
    expect(T_COMMA);
    _1 = parseInt();
  }

  private TextureDataFormat parseFormat() {
    // T_FORMAT is already in there
    expect(T_COLON);

    final int token = nextToken();

    switch(token) {
      case T_RGBA8888: return TextureDataFormat.RGBA;
      case T_RGB888: return TextureDataFormat.RGB;
      case T_RGBA4444:
      case T_RGB565:
      case T_ALPHA:
        throw new UnsupportedOperationException("Unsupported texture format");
      default:
        throw new RuntimeException("Expected texture format, got "+token+'.'+positionToString());
    }
  }

  private void expectKey(final int token) {
    expect(token);
    expect(T_COLON);
  }

  private int nextToken() {
    int actualToken;

    do {
      actualToken = scanner.nextToken();
    } while(actualToken == T_WHITESPACE);

    return actualToken;
  }

  private int nextTokenGenerous() {
    int actualToken;

    do {
      actualToken = scanner.nextTokenGenerous();
    } while(actualToken == T_WHITESPACE);

    return actualToken;
  }

  private void expect(final int expectedToken) {
    expect(nextToken(), expectedToken);
  }

  private void expect(final int actualToken, final int expectedToken) {
    if(actualToken != expectedToken) {
      throw new RuntimeException("Expected "+expectedToken+", got "+actualToken+'.'+positionToString());
    }
  }

  @Nonnull
  private String positionToString() {
    return " ["+ scanner.line()+':'+ scanner.column()+']';
  }

  private void unexpected(final int token) {
    throw new RuntimeException("Unexpected token: "+token+positionToString());
  }
}

