package defrac.display.layout.inflater;

import defrac.display.DisplayObject;
import defrac.display.MovieClip;
import defrac.display.layout.DisplayObjectInflater;
import defrac.display.layout.LayoutContext;

import javax.annotation.Nonnull;

/**
 *
 */
public class MovieClipInflater extends DisplayObjectInflater {
  public MovieClipInflater() {}

  /** {@inheritDoc} */
  @Nonnull
  @Override
  protected DisplayObject newInstance(@Nonnull final LayoutContext context, final float width, final float height) {
    final MovieClip movieClip = new MovieClip();

    if(width >= 0.0f) {
      movieClip.width(width);
    }

    if(height >= 0.0f) {
      movieClip.height(height);
    }

    return movieClip;
  }
}
