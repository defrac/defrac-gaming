package defrac.display.layout;

import defrac.concurrent.Future;
import defrac.concurrent.Futures;
import defrac.display.BlendMode;
import defrac.display.DisplayObject;
import defrac.geom.Rectangle;
import defrac.json.JSON;
import defrac.json.JSONObject;
import defrac.lang.Void;

import javax.annotation.Nonnull;

import static defrac.display.layout.LayoutConstants.*;

/**
 *
 */
public abstract class DisplayObjectInflater {

  @Nonnull
  protected abstract DisplayObject newInstance(@Nonnull final LayoutContext context,
                                               final float width,
                                               final float height);

  @Nonnull
  protected Future<Void> inflate(@Nonnull final LayoutContext context,
                                 @Nonnull final JSONObject properties,
                                 @Nonnull final DisplayObject displayObject) {
    final float parentWidth = context.parentScope().width();
    final float parentHeight = context.parentScope().height();

    JSON property;

    property = context.resolveProperty(properties, KEY_X);
    if(property == null) property = context.resolveProperty(properties, KEY_LEFT);
    if(null != property) displayObject.x(context.resolvePercentage(parentWidth, property));

    property = context.resolveProperty(properties, KEY_Y);
    if(property == null) property = context.resolveProperty(properties, KEY_TOP);
    if(null != property) displayObject.y(context.resolvePercentage(parentHeight, property));

    property = context.resolveProperty(properties, KEY_RIGHT);
    if(null != property) displayObject.x(context.resolvePercentage(parentWidth, property) - displayObject.width());

    property = context.resolveProperty(properties, KEY_BOTTOM);
    if(null != property) displayObject.y(context.resolvePercentage(parentHeight, property) - displayObject.height());

    property = context.resolveProperty(properties, KEY_SCALE_X);
    if(null != property) displayObject.scaleX(context.resolveValue(property));

    property = context.resolveProperty(properties, KEY_SCALE_Y);
    if(null != property) displayObject.scaleY(context.resolveValue(property));

    property = context.resolveProperty(properties, KEY_SCALE);
    if(null != property) displayObject.scaleTo(context.resolveValue(property));

    property = context.resolveProperty(properties, KEY_REG_X);
    if(null != property) displayObject.registrationPointX(context.resolveValue(property));

    property = context.resolveProperty(properties, KEY_REG_Y);
    if(null != property) displayObject.registrationPointY(context.resolveValue(property));

    property = context.resolveProperty(properties, KEY_PIVOT);
    if(null != property) {
      final float value = context.resolveValue(property);
      displayObject.registrationPoint(value, value);
    }

    property = context.resolveProperty(properties, KEY_VISIBLE);
    if(null != property) displayObject.visible(context.resolveValue(property) != 0.0f);

    property = context.resolveProperty(properties, KEY_ROTATION);
    if(null != property) displayObject.rotation(context.resolveValue(property));

    property = context.resolveProperty(properties, KEY_ALPHA);
    if(null != property) displayObject.alpha(context.resolveValue(property));

    property = context.resolveProperty(properties, KEY_BLEND_MODE);
    if(null != property) {
      final BlendMode blendMode = context.resolveBlendMode(property.stringValue());
      displayObject.blendMode(blendMode);
    }

    property = context.resolveProperty(properties, KEY_SCROLL_RECT);

    if(null != property && property.isObject()) {
      final JSONObject scrollRect = (JSONObject)property;
      final float x = context.resolvePercentage(parentWidth, context.resolveProperty(scrollRect, KEY_X));
      final float y = context.resolvePercentage(parentHeight, context.resolveProperty(scrollRect, KEY_Y));
      final float width = context.resolvePercentage(parentWidth, context.resolveProperty(scrollRect, KEY_WIDTH));
      final float height = context.resolvePercentage(parentHeight, context.resolveProperty(scrollRect, KEY_HEIGHT));
      displayObject.scrollRect(new Rectangle(x, y, width, height));
    }

    return Futures.success(Void.INSTANCE);
  }
}
