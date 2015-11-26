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

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Reader;

import static defrac.display.atlas.libgdx.LibgdxAtlasTokens.*;

/**
 *
 */
final class LibgdxAtlasScanner {
  @Nonnull
  public static LibgdxAtlasScanner create(@Nonnull final Reader reader) {
    return new LibgdxAtlasScanner(reader);
  }

  @Nonnull
  protected final Reader in;

  @Nonnull
  protected final char[] valueBuffer = new char[0x100];

  protected int valueBufferIndex;

  protected boolean hasChar;

  protected int currentChar;

  protected int line;

  protected int column;

  private LibgdxAtlasScanner(@Nonnull final Reader reader) {
    this.in = reader;
  }

  public final int line() {
    return line;
  }

  public final int column() {
    return column;
  }

  @Nonnull
  public final String stringValue() {
    return new String(valueBuffer, 0, valueBufferIndex);
  }

  public int nextToken() {
    try {
      final int c = nextChar();

      if(c == -1) {
        return T_EOF;
      } else if(isWhitespace(c)) {
        consumeWhitespace();
        return T_WHITESPACE;
      } else if(isLineTerminator(c)) {
        consumeLineTerminator();
        return T_LINETERMINATOR;
      } else if(isIdentifierStart(c)) {
        int potentialIdentifierPart;

        advanceAndBeginBuffer(c);

        while(isIdentifierPart(potentialIdentifierPart = nextChar())) {
          advanceAndContinueBuffer(potentialIdentifierPart);
        }

        return identifierToKeyword();
      } else if(c == ',') {
        advance();
        return T_COMMA;
      } else if(c == ':') {
        advance();
        return T_COLON;
      } else if(isIntegerStart(c)) {
        int potentialIntegerPart;

        advanceAndBeginBuffer(c);

        while(isIntegerPart(potentialIntegerPart = nextChar())) {
          advanceAndContinueBuffer(potentialIntegerPart);
        }

        return T_INTEGER;
      } else {
        recover();
        return T_ERROR;
      }
    } catch(final IOException exception) {
      throw new RuntimeException(exception);
    }
  }

  protected final int nextChar() throws IOException {
    if(!hasChar) {
      hasChar = true;
      currentChar = in.read();
      ++column;
    }

    return currentChar;
  }

  protected final void advance() {
    hasChar = false;
  }

  protected final void advanceAndBeginBuffer(final int c) {
    advance();
    valueBuffer[0] = (char)c;
    valueBufferIndex = 1;
  }

  protected final void advanceAndContinueBuffer(final int c) {
    advance();
    valueBuffer[valueBufferIndex++] = (char)c;
  }

  protected final void consumeWhitespace() throws IOException {
    while(isWhitespace(nextChar())) {
      advance();
    }
  }

  protected final void consumeLineTerminator() throws IOException {
    while(isLineTerminator(nextChar())) {
      ++line;
      column = 0;
      advance();
    }
  }

  protected final boolean isIdentifierStart(final int c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || c == '.' || c == '/' || c == '\\';
  }

  protected final boolean isIdentifierPart(final int c) {
    return isIdentifierStart(c) || (c >= '0' && c <= '9') || c == '-';
  }

  protected final boolean isWhitespace(final int c) {
    return c == ' '  || c == '\t';
  }

  protected final boolean isLineTerminator(final int c) {
    return c == '\r' || c == '\n';
  }

  protected final boolean isIntegerStart(final int c) {
    return c == '-' || isIntegerPart(c);
  }

  protected final boolean isIntegerPart(final int c) {
    return c >= '0' && c <= '9';
  }

  protected final void recover() throws IOException {
    do {
      final int c = nextChar();

      if(isLineTerminator(c)) {
        break;
      } else {
        advance();
      }
    } while(true);

    consumeLineTerminator();
  }

  protected final int identifierToKeyword() {
    switch(valueBufferIndex) {
      case 1: {
        final char c0 = valueBuffer[0];

        if(c0 == 'x') {
          return T_X;
        } else if(c0 == 'y') {
          return T_Y;
        } else {
          return T_IDENTIFIER;
        }
      }

      case 2: {
        final char c0 = valueBuffer[0];
        final char c1 = valueBuffer[1];

        if(c0 == 'x' && c1 == 'y') {
          return T_XY;
        } else {
          return T_IDENTIFIER;
        }
      }

      case 3: {
        final char c0 = valueBuffer[0];
        final char c1 = valueBuffer[1];
        final char c2 = valueBuffer[2];

        if(c0 == 'p' && c1 == 'a' && c2 == 'd') {
          return T_PAD;
        } else {
          return T_IDENTIFIER;
        }
      }

      case 4: {
        final char c0 = valueBuffer[0];
        final char c1 = valueBuffer[1];
        final char c2 = valueBuffer[2];
        final char c3 = valueBuffer[3];

        if(
            c0 == 's' &&
            c1 == 'i' &&
            c2 == 'z' &&
            c3 == 'e') {
          return T_SIZE;
        } else if(
            c0 == 'o' &&
            c1 == 'r' &&
            c2 == 'i' &&
            c3 == 'g') {
          return T_ORIG;
        } else if(
            c0 == 't' &&
            c1 == 'r' &&
            c2 == 'u' &&
            c3 == 'e') {
          return T_TRUE;
        } else if(
            c0 == 'n' &&
            c1 == 'o' &&
            c2 == 'n' &&
            c3 == 'e') {
          return T_NONE;
        } else {
          return T_IDENTIFIER;
        }
      }

      case 5:
        if(
            valueBuffer[0] == 'i' &&
            valueBuffer[1] == 'n' &&
            valueBuffer[2] == 'd' &&
            valueBuffer[3] == 'e' &&
            valueBuffer[4] == 'x') {
          return T_INDEX;
        } else if(
            valueBuffer[0] == 'f' &&
            valueBuffer[1] == 'a' &&
            valueBuffer[2] == 'l' &&
            valueBuffer[3] == 's' &&
            valueBuffer[4] == 'e') {
          return T_FALSE;
        } else {
          return keywordLookup(L5_KEYWORDS, T_SPLIT);
        }

      case 6:
        if(
            valueBuffer[0] == 'r' &&
            valueBuffer[1] == 'o' &&
            valueBuffer[2] == 't' &&
            valueBuffer[3] == 'a' &&
            valueBuffer[4] == 't' &&
            valueBuffer[5] == 'e') {
          return T_ROTATE;
        } else if(
            valueBuffer[0] == 'o' &&
            valueBuffer[1] == 'f' &&
            valueBuffer[2] == 'f' &&
            valueBuffer[3] == 's' &&
            valueBuffer[4] == 'e' &&
            valueBuffer[5] == 't') {
          return T_OFFSET;
        } else {
          return keywordLookup(L6_KEYWORDS, T_FORMAT);
        }

      case 8:
        return keywordLookup(L8_KEYWORDS, T_RGBA8888);

      default:
        return T_IDENTIFIER;
    }
  }

  protected final int keywordLookup(final char[][] keywords, final int tokenOffset) {
    final int n = keywords.length;

    for(int i = 0; i < n; ++i) {
      final char[] keyword = keywords[i];
      boolean equal = true;

      for(int j = 0; j < valueBufferIndex; ++j) {
        if(keyword[j] != valueBuffer[j]) {
          equal = false;
          break;
        }
      }

      if(equal) {
        return tokenOffset + i;
      }
    }

    return T_IDENTIFIER;
  }
}
