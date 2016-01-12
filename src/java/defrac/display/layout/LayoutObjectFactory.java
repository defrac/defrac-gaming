package defrac.display.layout;

import defrac.display.BlendMode;
import defrac.display.DisplayObject;
import defrac.display.DisplayObjectContainer;
import defrac.geom.Rectangle;
import defrac.json.JSON;
import defrac.json.JSONNumber;
import defrac.json.JSONObject;
import defrac.lang.Strings;

import javax.annotation.Nonnull;

/**
 *
 */
public abstract class LayoutObjectFactory<D extends DisplayObject> {
  @Nonnull
  private static final JSONNumber NEGATIVE_ONE = JSONNumber.of(-1.0f);

  @Nonnull public static final String KEY_X = "x";
  @Nonnull public static final String KEY_Y = "y";
  @Nonnull public static final String KEY_SCALE_X = "scaleX";
  @Nonnull public static final String KEY_SCALE_Y = "scaleY";
  @Nonnull public static final String KEY_SCALE = "scale";
  @Nonnull public static final String KEY_REG_X = "regX";
  @Nonnull public static final String KEY_REG_Y = "regY";
  @Nonnull public static final String KEY_PIVOT = "pivot";
  @Nonnull public static final String KEY_VISIBLE = "visible";
  @Nonnull public static final String KEY_ROTATION = "rotation";
  @Nonnull public static final String KEY_ALPHA = "alpha";
  @Nonnull public static final String KEY_BLEND_MODE = "blendMode";
  @Nonnull public static final String KEY_SCROLL_RECT = "scrollRect";

  LayoutObjectFactory() {}

  @Nonnull
  protected abstract D newInstance(final float width,
                                   final float height);

  protected void configure(@Nonnull final LayoutContext context,
                           @Nonnull final JSONObject properties,
                           @Nonnull final D displayObject) {
    final float parentWidth = context.parentScope().width();
    final float parentHeight = context.parentScope().height();

    JSON property;

    property = properties.opt(KEY_X, null);
    if(null != property) displayObject.x(context.resolvePercentage(parentWidth, property));

    property = properties.opt(KEY_Y, null);
    if(null != property) displayObject.y(context.resolvePercentage(parentHeight, property));

    property = properties.opt(KEY_SCALE_X, null);
    if(null != property) displayObject.scaleX(context.resolveValue(property));

    property = properties.opt(KEY_SCALE_Y, null);
    if(null != property) displayObject.scaleY(context.resolveValue(property));

    property = properties.opt(KEY_SCALE, null);
    if(null != property) displayObject.scaleTo(context.resolveValue(property));

    property = properties.opt(KEY_REG_X, null);
    if(null != property) displayObject.registrationPointX(context.resolveValue(property));

    property = properties.opt(KEY_REG_Y, null);
    if(null != property) displayObject.registrationPointY(context.resolveValue(property));

    property = properties.opt(KEY_PIVOT, null);
    if(null != property) {
      final float value = context.resolveValue(property);
      displayObject.registrationPoint(value, value);
    }

    property = properties.opt(KEY_VISIBLE, null);
    if(null != property) displayObject.visible(context.resolveValue(property) != 0.0f);

    property = properties.opt(KEY_ROTATION, null);
    if(null != property) displayObject.rotation(context.resolveValue(property));

    property = properties.opt(KEY_ALPHA, null);
    if(null != property) displayObject.alpha(context.resolveValue(property));

    property = properties.opt(KEY_BLEND_MODE, null);
    if(null != property) {
      final BlendMode blendMode = context.resolveBlendMode(property.stringValue());
      displayObject.blendMode(blendMode);
    }

    property = properties.opt(KEY_SCROLL_RECT, null);

    if(null != property && property.isObject()) {
      final JSONObject scrollRect = (JSONObject)property;
      final float x = context.resolvePercentage(parentWidth, scrollRect.get(KEY_X));
      final float y = context.resolvePercentage(parentHeight, scrollRect.get(KEY_Y));
      final float width = context.resolvePercentage(parentWidth, scrollRect.get(LayoutContext.KEY_WIDTH));
      final float height = context.resolvePercentage(parentHeight, scrollRect.get(LayoutContext.KEY_HEIGHT));
      displayObject.scrollRect(new Rectangle(x, y, width, height));
    }
  }

  @Nonnull
  public D build(@Nonnull final LayoutContext context,
                 @Nonnull final JSONObject properties) {
    final JSON jsonWidth = properties.opt(LayoutContext.KEY_WIDTH, NEGATIVE_ONE);
    final JSON jsonHeight = properties.opt(LayoutContext.KEY_HEIGHT, NEGATIVE_ONE);
    final String id = properties.optString(LayoutContext.KEY_ID, "");
    final float parentWidth = context.parentScope().width();
    final float parentHeight = context.parentScope().height();
    final float width = context.resolvePercentage(parentWidth, jsonWidth);
    final float height = context.resolvePercentage(parentHeight, jsonHeight);
    final D displayObject = newInstance(width, height);

    context.setThis(displayObject);

    if(!Strings.isNullOrEmpty(id)) {
      context.storeDisplayObjectForId(id, displayObject);
      displayObject.name(id);
    }

    configure(context, properties, displayObject);

    if(displayObject instanceof DisplayObjectContainer) {
      final DisplayObjectContainer displayObjectContainer = (DisplayObjectContainer)displayObject;

      for(final JSON json : properties.optArray(LayoutContext.KEY_CHILDREN)) {
        final DisplayObject childDisplayObject = context.build(json.asObject());

        if(childDisplayObject == null) {
          continue;
        }

        displayObjectContainer.addChild(childDisplayObject);
      }

    }

    return displayObject;
  }

  public static class Functional<D extends DisplayObject> extends LayoutObjectFactory<D> {
    @Nonnull
    private final Factory<D> factory;

    public Functional(@Nonnull final Factory<D> factory) {
      this.factory = factory;
    }

    @Nonnull
    @Override
    protected D newInstance(final float width, final float height) {
      return factory.newInstance(width, height);
    }

    public interface Factory<D> {
      D newInstance(final float width, final float height);
    }
  }
}
