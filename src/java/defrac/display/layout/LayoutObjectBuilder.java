package defrac.display.layout;

import defrac.display.BlendMode;
import defrac.display.DisplayObject;
import defrac.geom.Rectangle;
import defrac.json.JSON;
import defrac.json.JSONNumber;
import defrac.json.JSONObject;
import defrac.lang.Strings;

import javax.annotation.Nonnull;

import static defrac.display.layout.LayoutConstants.*;

/**
 *
 */
public abstract class LayoutObjectBuilder<D extends DisplayObject> {
  @Nonnull
  private static final JSONNumber NEGATIVE_ONE = JSONNumber.of(-1.0f);

  LayoutObjectBuilder() {}

  @Nonnull
  protected abstract D newInstance(@Nonnull final LayoutContext context,
                                   final float width,
                                   final float height);

  protected void applyProperties(@Nonnull final LayoutContext context,
                                 @Nonnull final JSONObject properties,
                                 @Nonnull final D displayObject) {
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
  }

  @Nonnull
  public D build(@Nonnull final LayoutContext context,
                 @Nonnull final JSONObject properties) {
    final JSON jsonWidth = context.resolveProperty(properties, KEY_WIDTH, NEGATIVE_ONE);
    final JSON jsonHeight = context.resolveProperty(properties, KEY_HEIGHT, NEGATIVE_ONE);
    final String id = context.resolveString(properties, KEY_ID);
    final float parentWidth = context.parentScope().width();
    final float parentHeight = context.parentScope().height();
    final float width = context.resolvePercentage(parentWidth, jsonWidth);
    final float height = context.resolvePercentage(parentHeight, jsonHeight);
    final D displayObject = newInstance(context, width, height);

    context.currentScope(displayObject);

    if(!Strings.isNullOrEmpty(id)) {
      context.storeDisplayObjectForId(id, displayObject);
      displayObject.name(id);
    }

    applyProperties(context, properties, displayObject);

    return displayObject;
  }

  public static class Functional<D extends DisplayObject> extends LayoutObjectBuilder<D> {
    @Nonnull
    private final Factory<D> factory;

    public Functional(@Nonnull final Factory<D> factory) {
      this.factory = factory;
    }

    @Nonnull
    @Override
    protected D newInstance(@Nonnull final LayoutContext context,
                            final float width,
                            final float height) {
      return factory.newInstance(width, height);
    }

    public interface Factory<D> {
      D newInstance(final float width, final float height);
    }
  }
}
