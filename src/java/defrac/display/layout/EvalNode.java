package defrac.display.layout;

import javax.annotation.Nonnull;

/**
 *
 */
abstract class EvalNode {
  @Nonnull
  public static EvalNode binary(final @Nonnull EvalNode left, final int operator, final @Nonnull EvalNode right) {
    return new Binary(left, operator, right);
  }

  @Nonnull
  public static EvalNode unary(final int operator, final @Nonnull EvalNode right) {
    return new Unary(operator, right);
  }

  @Nonnull
  public static EvalNode literal(final float value) {
    return new Literal(value);
  }

  @Nonnull
  public static EvalNode fieldGet(@Nonnull final String receiver,
                                  @Nonnull final String symbol) {
    return new FieldGet(receiver, symbol);
  }

  protected EvalNode() {}

  public abstract float evaluate(@Nonnull final LayoutContext context);

  private static final class Binary extends EvalNode {
    @Nonnull
    public final EvalNode left;

    public final int operator;

    @Nonnull
    public final EvalNode right;

    public Binary(final @Nonnull EvalNode left, final int operator, final @Nonnull EvalNode right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }

    @Override
    public float evaluate(@Nonnull final LayoutContext context) {
      final float lhs = left.evaluate(context);
      final float rhs = right.evaluate(context);

      switch(operator) {
        case EvalTokens.ADD: return lhs + rhs;
        case EvalTokens.SUB: return lhs - rhs;
        case EvalTokens.MUL: return lhs * rhs;
        case EvalTokens.DIV: return lhs / rhs;
        case EvalTokens.MOD: return lhs % rhs;
      }

      throw new IllegalStateException();
    }
  }

  private static final class Unary extends EvalNode {
    public final int operator;

    @Nonnull
    public final EvalNode right;

    public Unary(final int operator, final @Nonnull EvalNode right) {
      this.operator = operator;
      this.right = right;
    }

    @Override
    public float evaluate(@Nonnull final LayoutContext context) {
      switch(operator) {
        case '-': return -right.evaluate(context);
      }

      throw new IllegalStateException();
    }
  }

  private static final class Literal extends EvalNode {
    public final float value;

    public Literal(final float value) {
      this.value = value;
    }

    @Override
    public float evaluate(@Nonnull final LayoutContext context) {
      return value;
    }
  }

  private static final class FieldGet extends EvalNode {
    @Nonnull public final String receiver;
    @Nonnull public final String symbol;

    public FieldGet(@Nonnull final String receiver,
                    @Nonnull final String symbol) {
      this.receiver = receiver;
      this.symbol = symbol;
    }

    @Override
    public float evaluate(@Nonnull final LayoutContext context) {
      return context.fieldGet(receiver, symbol);
    }
  }
}
