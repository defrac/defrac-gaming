package defrac.display.layout;

/**
 *
 */
public final class LayoutException extends RuntimeException {
  public LayoutException() {}

  public LayoutException(final String detailMessage) {
    super(detailMessage);
  }

  public LayoutException(final String detailMessage, final Throwable throwable) {
    super(detailMessage, throwable);
  }

  public LayoutException(final Throwable throwable) {
    super(throwable);
  }
}
