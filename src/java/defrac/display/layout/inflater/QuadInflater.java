package defrac.display.layout.inflater;

import defrac.concurrent.Future;
import defrac.display.DisplayObject;
import defrac.display.Quad;
import defrac.display.layout.DisplayObjectInflater;
import defrac.display.layout.LayoutContext;
import defrac.json.JSON;
import defrac.json.JSONObject;
import defrac.lang.Void;
import defrac.util.Color;

import javax.annotation.Nonnull;

/**
 *
 */
public class QuadInflater extends DisplayObjectInflater {
  public QuadInflater() {}

  /** {@inheritDoc} */
  @Nonnull
  @Override
  protected DisplayObject newInstance(@Nonnull final LayoutContext context, final float width, final float height) {
    final Quad quad = new Quad();

    if(width >= 0.0f) {
      quad.width(width);
    }

    if(height >= 0.0f) {
      quad.height(height);
    }

    return quad;
  }

  /** {@inheritDoc} */
  @Nonnull
  @Override
  protected Future<Void> inflate(@Nonnull final LayoutContext context,
                                 @Nonnull final JSONObject properties,
                                 @Nonnull final DisplayObject displayObject) {
    return super.inflate(context, properties, displayObject).
        proceed(theVoid -> applyColor(context, properties, (Quad)displayObject), context.dispatcher());
  }

  protected void applyColor(@Nonnull final LayoutContext context,
                            @Nonnull final JSONObject properties,
                            @Nonnull final Quad quad) {
    final JSON property = context.resolveProperty(properties, "color");

    if(null != property) {
      if(property.isString()) {
        quad.color(Color.valueOf(context.interpolateString(property.stringValue())));
      } else {
        quad.color(property.intValue());
      }
    }
  }
}
