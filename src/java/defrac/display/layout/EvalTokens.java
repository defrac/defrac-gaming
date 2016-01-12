package defrac.display.layout;

import javax.annotation.Nonnull;

/**
 *
 */
final class EvalTokens {
  public static final int EOL = 0;
  public static final int ERROR = 1;
  public static final int ADD = 2;
  public static final int SUB = 3;
  public static final int MUL = 4;
  public static final int DIV = 5;
  public static final int MOD = 6;
  public static final int DOT = 7;
  public static final int IDENTIFIER = 8;
  public static final int NUMBER = 9;
  public static final int LPAREN = 10;
  public static final int RPAREN = 11;
  public static final int WHITESPACE = 12;

  @Nonnull
  public static String toString(final int token) {
    switch(token) {
      case EOL: return "EOL";
      case ERROR: return "ERROR";
      case ADD: return "ADD";
      case SUB: return "SUB";
      case MUL: return "MUL";
      case DIV: return "DIV";
      case MOD: return "MOD";
      case DOT: return "DOT";
      case IDENTIFIER: return "IDENTIFIER";
      case NUMBER: return "NUMBER";
      case LPAREN: return "LPAREN";
      case RPAREN: return "RPAREN";
      case WHITESPACE: return "WHITESPACE";
    }

    return "<unknown>";
  }
  private EvalTokens() {}
}
