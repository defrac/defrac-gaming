package defrac.display.layout.inflater;

import defrac.display.DisplayObject;
import defrac.display.Scale3Image;
import defrac.display.layout.DisplayObjectInflater;
import defrac.display.layout.LayoutContext;

import javax.annotation.Nonnull;

/**
 *
 */
public class Scale3ImageInfalter extends DisplayObjectInflater {
  public Scale3ImageInfalter() {}

  /** {@inheritDoc} */
  @Nonnull
  @Override
  protected DisplayObject newInstance(@Nonnull final LayoutContext context, final float width, final float height) {
    final Scale3Image image = new Scale3Image();

    if(width >= 0.0f) {
      image.width(width);
    }

    if(height >= 0.0f) {
      image.height(height);
    }

    return image;
  }
}
