package defrac.display.layout;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static defrac.display.layout.EvalTokens.*;

/**
 *
 */
final class EvalScanner {
  private static final int SINGLE_CHAR_TOKENS_LENGTH = 128;
  private static final char[] SINGLE_CHAR_TOKENS = new char[SINGLE_CHAR_TOKENS_LENGTH];

  @Nullable
  private String input;

  @Nonnull
  private final StringBuilder buffer = new StringBuilder();

  private int maxIndex;

  private int index = 0;

  static {
    SINGLE_CHAR_TOKENS['+'] = ADD;
    SINGLE_CHAR_TOKENS['-'] = SUB;
    SINGLE_CHAR_TOKENS['*'] = MUL;
    SINGLE_CHAR_TOKENS['/'] = DIV;
    SINGLE_CHAR_TOKENS['%'] = MOD;
    SINGLE_CHAR_TOKENS['.'] = DOT;
    SINGLE_CHAR_TOKENS['('] = LPAREN;
    SINGLE_CHAR_TOKENS[')'] = RPAREN;
    SINGLE_CHAR_TOKENS[' '] = WHITESPACE;
    SINGLE_CHAR_TOKENS['\t'] = WHITESPACE;
    SINGLE_CHAR_TOKENS['\r'] = WHITESPACE;
    SINGLE_CHAR_TOKENS['\n'] = WHITESPACE;
  }

  public EvalScanner() {}

  public void reset(@Nonnull final String input) {
    this.buffer.setLength(0);
    this.input = input;
    this.index = 0;
    this.maxIndex = input.length();
  }

  public int nextToken() {
    if(index >= maxIndex) {
      return EOL;
    }

    final int currentChar = currentChar();

    if(currentChar >= 0 && currentChar < SINGLE_CHAR_TOKENS_LENGTH) {
      final int result = SINGLE_CHAR_TOKENS[currentChar];

      if(result != 0) {
        advance();
        return result;
      }
    }

    if(isIdentifierStart(currentChar)) {
      return identifier();
    } else if(currentChar >= '0' && currentChar <= '9') {
      return number();
    }

    return ERROR;
  }

  @Nonnull
  public String tokenValue() {
    return buffer.toString();
  }

  private int currentChar() {
    if(index >= maxIndex || input == null) {
      return -1;
    }

    return input.charAt(index);
  }

  private void advance() {
    ++index;
  }

  private boolean isIdentifierStart(final int c) {
    return c >= 'a' && c <= 'z'
        || c >= 'A' && c <= 'Z'
        || c == '_'
        || c == '$'
        || c == '#';
  }

  private int identifier() {
    buffer.setLength(0);

    int c = currentChar();
    advance();

    buffer.append((char)c);

    int nextChar = currentChar();

    while(isIdentifierStart(nextChar) || (nextChar >= '0' && nextChar <= '9')) {
      buffer.append((char)nextChar);
      advance();
      nextChar = currentChar();
    }

    return IDENTIFIER;
  }

  private int number() {
    buffer.setLength(0);

    int c = currentChar();

    if(c == '0') {
      buffer.append('0');
      advance();
      c = currentChar();
    } else if(c >= '1' && c <= '9') {
      buffer.append((char)c);
      advance();

      int nextDigit = currentChar();
      while(nextDigit >= '0' && nextDigit <= '9') {
        buffer.append((char)nextDigit);
        advance();
        nextDigit = currentChar();
      }
      c = nextDigit;
    }

    // fraction part
    if(c == '.') {
      buffer.append('.');
      advance();

      int nextDigit = currentChar();
      while(nextDigit >= '0' && nextDigit <= '9') {
        buffer.append((char)nextDigit);
        advance();
        nextDigit = currentChar();
      }
      c = nextDigit;
    }

    // exponent part
    if(c == 'e' || c == 'E') {
      buffer.append((char)c);

      advance();

      int cc = currentChar();

      if(cc == '-' || cc == '+') {
        buffer.append((char)cc);
        advance();
        cc = currentChar();
      }

      while(cc >= '0' && cc <= '9') {
        buffer.append((char)cc);
        advance();
        cc = currentChar();
      }
    }

    return NUMBER;
  }
}
