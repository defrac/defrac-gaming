package defrac.display.layout;

import javax.annotation.Nonnull;

import static defrac.display.layout.EvalTokens.*;

/**
 *
 */
final class EvalParser {
  // See https://www.engr.mun.ca/~theo/Misc/exp_parsing.htm

  @Nonnull
  private final EvalScanner scanner;

  private int current = -1;

  public EvalParser() {
    scanner = new EvalScanner();
  }

  private int next() {
    if(current == -1) {
      do {
        current = scanner.nextToken();
      } while(current == WHITESPACE);
    }

    return current;
  }

  private void consume() {
    current = -1;
  }

  private void expect(final int token) {
    if(next() == token) {
      consume();
    } else {
      throw new RuntimeException("Expected token "+token+", got "+current);
    }
  }

  @Nonnull
  public EvalNode parse(@Nonnull final String input) {
    scanner.reset(input);
    final EvalNode node = exp(0);
    expect(EOL);
    return node;
  }

  @Nonnull
  private EvalNode exp(final int parentPrecedence) {
    EvalNode lhs = P();

    while(isBinary(next()) && precedenceOf(next()) >= parentPrecedence) {
      final int binaryOperator = next();
      consume();
      final int newParentPrecedence = 1 + precedenceOf(binaryOperator);
      final EvalNode rhs = exp(newParentPrecedence);
      lhs = EvalNode.binary(lhs, binaryOperator, rhs);
    }

    return lhs;
  }

  @Nonnull
  private EvalNode P() {
    final int token = next();

    if(token == SUB) {
      consume();
      final EvalNode expression = exp(precedenceOf(token));
      return EvalNode.unary(token, expression);
    }

    if(token == LPAREN) {
      consume();
      final EvalNode expression = exp(0);
      expect(RPAREN);
      return expression;
    }

    if(token == NUMBER) {
      final EvalNode literal = EvalNode.literal(Float.parseFloat(scanner.tokenValue()));
      consume();
      return literal;
    }

    if(token == IDENTIFIER) {
      final String receiver = scanner.tokenValue();
      consume();

      if("pi".equalsIgnoreCase(receiver) || "π".equalsIgnoreCase(receiver)) {
        return EvalNode.literal((float)Math.PI);
      }

      expect(DOT);
      if(next() != IDENTIFIER) {
        throw new RuntimeException("Expected identifier");
      }
      final String member = scanner.tokenValue();
      consume();
      return EvalNode.fieldGet(receiver, member);
    }

    throw new RuntimeException("Expected literal or paren expression, got "+token);
  }

  private boolean isBinary(final int token) {
    return token == ADD
        || token == SUB
        || token == MUL
        || token == DIV
        || token == MOD;
  }

  private int precedenceOf(final int token) {
    switch(token) {
      case ADD:
      case SUB: return 1;
      case MUL:
      case DIV:
      case MOD: return 2;
      default: return -1;
    }
  }
}
