package defrac.display.layout.inflater;

import defrac.display.DisplayObject;
import defrac.display.Stats;
import defrac.display.layout.DisplayObjectInflater;
import defrac.display.layout.LayoutContext;

import javax.annotation.Nonnull;

/**
 *
 */
public class StatsInflater extends DisplayObjectInflater {
  public StatsInflater() {}

  /** {@inheritDoc} */
  @Nonnull
  @Override
  protected DisplayObject newInstance(@Nonnull final LayoutContext context, final float width, final float height) {
    return new Stats();
  }
}
