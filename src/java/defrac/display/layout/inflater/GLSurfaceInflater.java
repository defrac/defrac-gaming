package defrac.display.layout.inflater;

import defrac.display.DisplayObject;
import defrac.display.GLSurface;
import defrac.display.layout.DisplayObjectInflater;
import defrac.display.layout.LayoutContext;
import defrac.lang.Preconditions;

import javax.annotation.Nonnull;

/**
 *
 */
public class GLSurfaceInflater extends DisplayObjectInflater {
  public GLSurfaceInflater() {}

  /** {@inheritDoc} */
  @Nonnull
  @Override
  protected DisplayObject newInstance(@Nonnull final LayoutContext context, final float width, final float height) {
    Preconditions.checkArgument(width >= 0, "width < 0");
    Preconditions.checkArgument(height >= 0, "height < 0");
    return new GLSurface(width, height);
  }
}
