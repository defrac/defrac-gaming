package defrac.display.layout;

import javax.annotation.Nonnull;

import static defrac.display.layout.EvalTokens.*;

/**
 *
 */
final class EvalParser {
  @Nonnull
  private final EvalScanner scanner;

  private int token = -1;

  public EvalParser() {
    scanner = new EvalScanner();
  }

  private int currentToken() {
    if(token == -1) {
      do {
        token = scanner.nextToken();
      } while(token == WHITESPACE);
    }

    return token;
  }

  private void consumeToken() {
    token = -1;
  }

  private void expectToken(final int token) {
    if(currentToken() == token) {
      consumeToken();
    } else {
      throw new LayoutException("Expected token "+EvalTokens.toString(token)+", got "+EvalTokens.toString(this.token)+". Expression: "+scanner.input);
    }
  }

  @Nonnull
  public EvalNode parse(@Nonnull final String input) {
    // Precedence Climbing algorithm
    // https://www.engr.mun.ca/~theo/Misc/exp_parsing.htm#climbing
    scanner.reset(input);
    final EvalNode node = exp(0);
    expectToken(EOL);
    return node;
  }

  @Nonnull
  private EvalNode exp(final int parentPrecedence) {
    EvalNode lhs = P();

    while(isBinary(currentToken()) && precedenceOf(currentToken()) >= parentPrecedence) {
      final int binaryOperator = currentToken();
      consumeToken();
      final int newParentPrecedence = 1 + precedenceOf(binaryOperator);
      final EvalNode rhs = exp(newParentPrecedence);
      lhs = EvalNode.binary(lhs, binaryOperator, rhs);
    }

    return lhs;
  }

  @Nonnull
  private EvalNode P() {
    final int token = currentToken();

    if(token == SUB) {
      consumeToken();
      final EvalNode expression = exp(precedenceOf(token));
      return EvalNode.unary(token, expression);
    }

    if(token == LPAREN) {
      consumeToken();
      final EvalNode expression = exp(0);
      expectToken(RPAREN);
      return expression;
    }

    if(token == NUMBER) {
      final EvalNode literal = EvalNode.literal(Float.parseFloat(scanner.tokenValue()));
      consumeToken();
      return literal;
    }

    if(token == IDENTIFIER) {
      final String receiver = scanner.tokenValue();
      consumeToken();

      if("pi".equalsIgnoreCase(receiver) || "π".equalsIgnoreCase(receiver)) {
        return EvalNode.literal((float)Math.PI);
      }

      expectToken(DOT);

      if(currentToken() != IDENTIFIER) {
        throw new LayoutException("Expected identifier. Expression: "+scanner.input);
      }

      final String member = scanner.tokenValue();
      consumeToken();

      return EvalNode.fieldGet(receiver, member);
    }

    throw new LayoutException("Expected literal, identifier or paren expression; got "+token+". Expression: "+scanner.input);
  }

  private static boolean isBinary(final int token) {
    return token == ADD
        || token == SUB
        || token == MUL
        || token == DIV
        || token == MOD;
  }

  private static int precedenceOf(final int token) {
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
