package defrac.display.layout.inflater;

import defrac.display.Canvas;
import defrac.display.DisplayObject;
import defrac.display.layout.DisplayObjectInflater;
import defrac.display.layout.LayoutContext;
import defrac.display.layout.LayoutException;

import javax.annotation.Nonnull;

/**
 *
 */
public class CanvasInflater extends DisplayObjectInflater {
  public CanvasInflater() {}

  /** {@inheritDoc} */
  @Nonnull
  @Override
  protected DisplayObject newInstance(@Nonnull final LayoutContext context, final float width, final float height) {
    if(width < 0) throw new LayoutException("\"width\" property is required for defrac.display.Canvas");
    if(height < 0) throw new LayoutException("\"height\" property is required for defrac.display.Canvas");
    return new Canvas(width, height);
  }
}
