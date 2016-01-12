package defrac.display.layout;

import defrac.display.BlendMode;
import defrac.display.DisplayObject;
import defrac.json.JSON;
import defrac.json.JSONObject;
import defrac.json.JSONString;
import defrac.lang.Preconditions;
import defrac.util.ArrayUtil;
import defrac.util.Dictionary;
import defrac.util.Platform;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static defrac.display.layout.LayoutConstants.*;

/**
 *
 */
public class LayoutContext {

  @Nonnull
  private final Dictionary<LayoutObjectBuilder<?>> builders = new Dictionary<>();

  @Nonnull
  private final Dictionary<DisplayObject> displayObjects = new Dictionary<>();

  @Nonnull
  private final Dictionary<String> variables = new Dictionary<>();

  @Nonnull
  private final StringInterpolator stringInterpolator = new StringInterpolator(variables);

  @Nonnull
  private DisplayObject[] scopeStack = DisplayObject.ARRAY_FACTORY.create(256);

  private int stackSize = 0;

  @Nullable
  private String variant;

  @Nonnull
  private final EvalParser parser = new EvalParser();

  public LayoutContext() {
    if(Platform.isAndroid()) {
      variant = LayoutConstants.VARIANT_ANDROID;
    } else if(Platform.isIOS()) {
      variant = LayoutConstants.VARIANT_IOS;
    } else if(Platform.isJVM()) {
      variant = LayoutConstants.VARIANT_JVM;
    } else if(Platform.isWeb()) {
      variant = LayoutConstants.VARIANT_WEB;
    }

    registerDefaultBuilders();
  }

  @Nullable
  public String variant() {
    return variant;
  }

  @Nonnull
  public LayoutContext variant(@Nonnull final String value) {
    this.variant = value;
    return this;
  }

  protected void registerDefaultBuilders() {
    LayoutObjectBuilders.registerDefaults(this);
  }

  public void registerBuilder(@Nonnull final String type,
                              @Nonnull final LayoutObjectBuilder<?> factory) {
    builders.put(type, factory);
  }

  public void defineVariable(@Nonnull final String symbol,
                             @Nonnull final String value) {
    variables.put(symbol, value);
  }

  @Nullable
  public DisplayObject findDisplayObjectById(@Nonnull final String id) {
    return displayObjects.get(id);
  }

  @Nullable
  public LayoutObjectBuilder<?> findBuilderByType(@Nonnull final String type) {
    return builders.get(type);
  }

  @Nonnull
  private String interpolateString(@Nonnull final String input) {
    return stringInterpolator.interpolate(input);
  }

  @Nullable
  JSON resolveProperty(@Nonnull final JSONObject json, final String key) {
    return resolveProperty(json, key, null);
  }

  JSON resolveProperty(@Nonnull final JSONObject json, final String key, final JSON defaultValue) {
    if(variant == null) {
      return json.opt(key, defaultValue);
    }

    final JSON resultForVariant = json.opt(key+"."+variant, defaultValue);

    if(resultForVariant == defaultValue) {
      return json.opt(key, defaultValue);
    }

    return resultForVariant;
  }

  float resolvePercentage(final float parentValue, @Nullable final JSON childValue) {
    if(childValue == null) {
      return Float.NaN;
    }

    if(childValue.isString()) {
      final String stringValue = interpolateString(childValue.stringValue());
      final int indexOfLastChar = stringValue.length() - 1;

      if(indexOfLastChar < 0) {
        throw new LayoutException("Empty string value");
      }

      final int firstChar = stringValue.charAt(0);

      if(stringValue.indexOf('%') == indexOfLastChar && firstChar >= '0' && firstChar <= '9') {
        return parentValue * 0.01f * Float.parseFloat(stringValue.substring(0, indexOfLastChar));
      }
    }

    return resolveValue(childValue);
  }

  float resolveValue(@Nonnull final JSON value) {
    if(value.isString()) {
      return parser.parse(interpolateString(value.stringValue())).evaluate(this);
    }

    return value.floatValue();
  }

  @Nonnull
  String resolveString(final JSONObject properties,
                       final String key) {
    return resolveString(properties, key, "");
  }

  @Nonnull
  String resolveString(final JSONObject properties,
                       final String key,
                       final String defaultValue) {
    return interpolateString(
        resolveProperty(
            properties,
            key,
            defaultValue.isEmpty() ? JSONString.EMPTY : JSONString.of(defaultValue)
        ).stringValue());
  }

  @Nullable
  DisplayObject currentScope() {
    Preconditions.checkState(stackSize > 0);
    return scopeStack[stackSize - 1];
  }

  @Nonnull
  DisplayObject parentScope() {
    Preconditions.checkState(stackSize > 0);
    return scopeStack[stackSize - 2];
  }

  void pushScope(@Nullable final DisplayObject displayObject) {
    scopeStack = ArrayUtil.append(
        scopeStack,
        stackSize++,
        displayObject,
        DisplayObject.ARRAY_FACTORY);
  }

  void currentScope(@Nonnull final DisplayObject displayObject) {
    Preconditions.checkState(scopeStack[stackSize - 1] == null);
    scopeStack[stackSize - 1] = displayObject;
  }

  void popScope() {
    Preconditions.checkState(stackSize > 0);
    scopeStack[--stackSize] = null;
  }

  void storeDisplayObjectForId(@Nonnull final String identifier,
                               @Nonnull final DisplayObject displayObject) {
    Preconditions.checkArgument(!"parent".equals(identifier), "\"parent\" is a reserved identifier");
    Preconditions.checkArgument(!"this".equals(identifier), "\"this\" is a reserved identifier");

    if(null != displayObjects.put(identifier, displayObject)) {
      throw new LayoutException("Duplicate identifier \""+identifier+'"');
    }
  }

  @Nonnull
  BlendMode resolveBlendMode(@Nonnull final String value) {
    switch(value.toLowerCase()) {
      case "add": return BlendMode.ADD;
      case "multiply": return BlendMode.MULTIPLY;
      case "normal": return BlendMode.NORMAL;
      case "opaque": return BlendMode.OPAQUE;
      case "screen": return BlendMode.SCREEN;
      case "inherit":
      default: return BlendMode.INHERIT;
    }
  }

  float fieldGet(@Nonnull final String receiver,
                 @Nonnull final String symbol) {
    final DisplayObject displayObject;

    switch(receiver) {
      case "this": displayObject = currentScope(); break;
      case "parent": displayObject = parentScope(); break;
      default:
        if(receiver.charAt(0) == '#') {
          displayObject = findDisplayObjectById(receiver.substring(1));
        } else {
          displayObject = findDisplayObjectById(receiver);
        }
    }

    if(null == displayObject) {
      throw new LayoutException("DisplayObject \""+receiver+"\" doesn't exist");
    }

    switch(symbol) {
      case KEY_LEFT:
      case KEY_X: return displayObject.x();
      case KEY_TOP:
      case KEY_Y: return displayObject.y();
      case KEY_WIDTH: return displayObject.width();
      case KEY_HEIGHT: return displayObject.height();
      case KEY_RIGHT: return displayObject.x() + displayObject.width();
      case KEY_BOTTOM: return displayObject.y() + displayObject.height();
      case KEY_ALPHA: return displayObject.alpha();
      case KEY_SCALE_X: return displayObject.scaleX();
      case KEY_SCALE_Y: return displayObject.scaleY();
      case KEY_REG_X: return displayObject.registrationPointX();
      case KEY_REG_Y: return displayObject.registrationPointY();
      case KEY_VISIBLE: return displayObject.visible() ? 1.0f : 0.0f;
      default: throw new LayoutException("Unsupported symbol \""+symbol+'"');
    }
  }
}
