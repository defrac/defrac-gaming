package defrac.display.layout.inflater;

import defrac.display.DisplayObject;
import defrac.display.Label;
import defrac.display.layout.DisplayObjectInflater;
import defrac.display.layout.LayoutContext;

import javax.annotation.Nonnull;

/**
 *
 */
public class LabelInflater extends DisplayObjectInflater {
  public LabelInflater() {}

  /** {@inheritDoc} */
  @Nonnull
  @Override
  protected DisplayObject newInstance(@Nonnull final LayoutContext context, final float width, final float height) {
    final Label label = new Label();

    if(width >= 0.0f) {
      label.width(width);
    }

    if(height >= 0.0f) {
      label.height(height);
    }

    return label;
  }
}
