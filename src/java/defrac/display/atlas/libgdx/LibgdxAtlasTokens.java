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

/**
 *
 */
final class LibgdxAtlasTokens {
  static final int T_ERROR = 0;
  static final int T_EOF = 1;
  static final int T_WHITESPACE = 2;
  static final int T_COLON = 3;
  static final int T_COMMA = 4;
  static final int T_TRUE = 5;
  static final int T_FALSE = 6;
  static final int T_INTEGER = 7;
  static final int T_LINETERMINATOR = 8;
  static final int T_IDENTIFIER = 9;
  static final int T_X = 10;
  static final int T_Y = 11;
  static final int T_XY = 12;
  static final int T_PAD = 13;
  static final int T_SIZE = 14;
  static final int T_ORIG = 15;
  static final int T_NONE = 16;
  static final int T_INDEX = 17;
  static final int T_ROTATE = 18;
  static final int T_OFFSET = 19;

  //
  // +++IMPORTANT+++IMPORTANT+++IMPORTANT+++IMPORTANT+++IMPORTANT+++IMPORTANT+++
  //
  // The following values are always in order. This means that if T_SPLIT, which is 20
  // is followed by T_ALPHA which is 21 the elements in the L5_KEYWORDS array must
  // be ordered { T_SPLIT, T_ALPHA, ... } because later on we make use of this fact to
  // calculate the token value.
  //
  // The actual call looks like keywordLookup(L5_KEYWORDS, T_SPLIT) with T_SPLIT being
  // the first keyword in the array.
  //
  // +++IMPORTANT+++IMPORTANT+++IMPORTANT+++IMPORTANT+++IMPORTANT+++IMPORTANT+++

  // length 5 keywords

  static final int T_SPLIT = 20;
  static final int T_ALPHA = 21;

  static final char[] C_SPLIT = "split".toCharArray();
  static final char[] C_ALPHA = "Alpha".toCharArray();

  static final char[][] L5_KEYWORDS = { C_SPLIT, C_ALPHA };

  // length 6 keywords

  static final int T_FORMAT = 22;
  static final int T_FILTER = 23;
  static final int T_REPEAT = 24;
  static final int T_RGB888 = 25;
  static final int T_RGB565 = 26;

  static final char[] C_FORMAT = "format".toCharArray();
  static final char[] C_FILTER = "filter".toCharArray();
  static final char[] C_REPEAT = "repeat".toCharArray();
  static final char[] C_RGB888 = "RGB888".toCharArray();
  static final char[] C_RGB565 = "RGB565".toCharArray();

  static final char[][] L6_KEYWORDS = { C_FORMAT, C_FILTER, C_REPEAT, C_RGB888, C_RGB565 };

  // length 8 keywords

  static final int T_RGBA8888 = 27;
  static final int T_RGBA4444 = 28;

  static final char[] C_RGBA8888 = "RGBA8888".toCharArray();
  static final char[] C_RGBA4444 = "RGBA4444".toCharArray();

  static final char[][] L8_KEYWORDS = { C_RGBA8888, C_RGBA4444 };
}
