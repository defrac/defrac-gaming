package defrac.display.layout;

import defrac.lang.Sets;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 *
 */
public final class LayoutConstants {
  @Nonnull public static final String KEY_TYPE = "type";
  @Nonnull public static final String KEY_CHILDREN = "children";
  @Nonnull public static final String KEY_WIDTH = "width";
  @Nonnull public static final String KEY_HEIGHT = "height";
  @Nonnull public static final String KEY_ID = "id";
  @Nonnull public static final String KEY_REPEAT = "repeat";

  @Nonnull public static final String KEY_X = "x";
  @Nonnull public static final String KEY_Y = "y";
  @Nonnull public static final String KEY_TOP = "top";
  @Nonnull public static final String KEY_RIGHT = "right";
  @Nonnull public static final String KEY_BOTTOM = "bottom";
  @Nonnull public static final String KEY_LEFT = "left";
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
  
  @Nonnull public static final Set<String> DEFAULT_PROPERTIES = Sets.of(
      KEY_TYPE,
      KEY_CHILDREN,
      KEY_WIDTH,
      KEY_HEIGHT,
      KEY_ID,
      KEY_REPEAT,
      KEY_X,
      KEY_Y,
      KEY_TOP,
      KEY_RIGHT,
      KEY_BOTTOM,
      KEY_LEFT,
      KEY_SCALE_X,
      KEY_SCALE_Y,
      KEY_SCALE,
      KEY_REG_X,
      KEY_REG_Y,
      KEY_PIVOT,
      KEY_VISIBLE,
      KEY_ROTATION,
      KEY_ALPHA,
      KEY_BLEND_MODE,
      KEY_SCROLL_RECT);

  @Nonnull public static final String VARIANT_ANDROID = "android";
  @Nonnull public static final String VARIANT_IOS = "ios";
  @Nonnull public static final String VARIANT_JVM = "jvm";
  @Nonnull public static final String VARIANT_WEB = "web";

  @Nonnull public static final String VARIABLE_REPEAT_INDEX = "repeatIndex";

  private LayoutConstants() {}
}
