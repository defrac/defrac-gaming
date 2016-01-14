package defrac.display.layout;

import defrac.concurrent.Dispatcher;
import defrac.concurrent.Dispatchers;
import defrac.display.BlendMode;
import defrac.display.DisplayObject;
import defrac.display.DisplayObjectContainer;
import defrac.display.layout.inflater.DefaultDisplayObjectInflaters;
import defrac.geom.Rectangle;
import defrac.json.JSON;
import defrac.json.JSONObject;
import defrac.json.JSONString;
import defrac.lang.Preconditions;
import defrac.util.ArrayUtil;
import defrac.util.Color;
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
  private static final Rectangle RECTANGLE = new Rectangle();

  @Nonnull
  private final Dictionary<DisplayObjectInflater> inflaters = new Dictionary<>();

  @Nonnull
  private final Dictionary<LayoutScope> displayObjects = new Dictionary<>();

  @Nonnull
  private final Dictionary<String> constants = new Dictionary<>();

  @Nonnull
  private final StringInterpolator stringInterpolator = new StringInterpolator(constants);

  @Nonnull
  private LayoutScope[] scopeStack = LayoutScope.ARRAY_FACTORY.create(256);

  @Nonnull
  private final DisplayObjectInflaterFactory inflaterFactory;

  private int stackSize = 0;

  @Nullable
  private String variant;

  @Nonnull
  private final EvalParser parser = new EvalParser();

  private Dispatcher dispatcher = Dispatchers.FOREGROUND;

  public LayoutContext() {
    this.inflaterFactory = new DisplayObjectInflaterFactory(this);

    initVariant();
    registerDefaultBuilders();
  }

  private void initVariant() {
    if(Platform.isAndroid()) {
      variant(LayoutConstants.VARIANT_ANDROID);
    } else if(Platform.isIOS()) {
      variant(LayoutConstants.VARIANT_IOS);
    } else if(Platform.isJVM()) {
      variant(LayoutConstants.VARIANT_JVM);
    } else if(Platform.isWeb()) {
      variant(LayoutConstants.VARIANT_WEB);
    }
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
    DefaultDisplayObjectInflaters.register(this);
  }

  public void registerInflater(@Nonnull final String type,
                               @Nonnull final DisplayObjectInflater factory) {
    inflaters.put(type, factory);
  }

  public void defineConstant(@Nonnull final String symbol,
                             @Nonnull final String value) {
    constants.put(symbol, value);
  }

  @Nullable
  public DisplayObject findDisplayObjectById(@Nonnull final String id) {
    final LayoutScope scope = displayObjects.get(id);
    return scope == null ? null : scope.displayObject;
  }

  @Nullable
  LayoutScope findScopeById(@Nonnull final String id) {
    return displayObjects.get(id);
  }

  void storeDisplayObjectForId(@Nonnull final String identifier,
                               @Nonnull final DisplayObject displayObject,
                               final float width, final float height) {
    Preconditions.checkArgument(!"parent".equals(identifier), "\"parent\" is a reserved identifier");
    Preconditions.checkArgument(!"this".equals(identifier), "\"this\" is a reserved identifier");

    final LayoutScope scope = LayoutScope.create(displayObject, width, height);

    if(null != displayObjects.put(identifier, scope)) {
      scope.dispose();
      throw new LayoutException("Duplicate identifier \""+identifier+'"');
    }
  }

  @Nonnull
  public DisplayObjectInflater getOrCreateInflaterByType(@Nonnull final String type) {
    final DisplayObjectInflater inflater = findInflaterByType(type);

    if(inflater == null) {
      return inflaterFactory.newInstance(type);
    }

    return inflater;
  }

  @Nullable
  public DisplayObjectInflater findInflaterByType(@Nonnull final String type) {
    return inflaters.get(type);
  }

  final boolean isVariant(@Nonnull final String property) {
    return property.endsWith("."+LayoutConstants.VARIANT_ANDROID)
        || property.endsWith("."+LayoutConstants.VARIANT_IOS)
        || property.endsWith("."+LayoutConstants.VARIANT_JVM)
        || property.endsWith("."+LayoutConstants.VARIANT_WEB);
  }

  @Nonnull
  public String interpolateString(@Nonnull final JSON input) {
    return interpolateString(input.stringValue());
  }

  @Nonnull
  public String interpolateString(@Nonnull final String input) {
    return stringInterpolator.interpolate(input);
  }

  @Nullable
  public JSON resolveProperty(@Nonnull final JSONObject json, final String key) {
    return resolveProperty(json, key, null);
  }

  JSON resolveProperty(@Nonnull final JSONObject json, final String key, final JSON defaultValue) {
    if(variant() == null) {
      return json.opt(key, defaultValue);
    }

    final JSON resultForVariant = json.opt(key+"."+variant(), defaultValue);

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
      final String stringValue = interpolateString(value.stringValue());

      if(stringValue.isEmpty()) {
        throw new LayoutException("Empty string value");
      }

      if(stringValue.charAt(0) == '#') {
        return Color.valueOf(stringValue);
      }

      return parser.parse(stringValue).evaluate(this);
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

  float resolveField(@Nonnull final String receiver,
                     @Nonnull final String symbol) {
    final LayoutScope scope;
    final DisplayObject displayObject;

    switch(receiver) {
      case "this": scope = currentScope(); break;
      case "parent": scope = parentScope(); break;
      default: scope = findScopeById(receiver);
    }

    if(null == scope) {
      throw new LayoutException("DisplayObject \""+receiver+"\" doesn't exist");
    }

    displayObject = scope.displayObject;

    switch(symbol) {
      case KEY_TOP: return displayObject.aabb(RECTANGLE).top();
      case KEY_RIGHT: return displayObject.aabb(RECTANGLE).right();
      case KEY_BOTTOM: return displayObject.aabb(RECTANGLE).bottom();
      case KEY_LEFT: return displayObject.aabb(RECTANGLE).left();
      case KEY_X: return displayObject.x();
      case KEY_Y: return displayObject.y();
      case KEY_WIDTH: return displayObject instanceof DisplayObjectContainer ? scope.width : displayObject.width();
      case KEY_HEIGHT: return displayObject instanceof DisplayObjectContainer ? scope.height : displayObject.height();
      case KEY_ALPHA: return displayObject.alpha();
      case KEY_SCALE_X: return displayObject.scaleX();
      case KEY_SCALE_Y: return displayObject.scaleY();
      case KEY_REG_X: return displayObject.registrationPointX();
      case KEY_REG_Y: return displayObject.registrationPointY();
      case KEY_VISIBLE: return displayObject.visible() ? 1.0f : 0.0f;
      default: throw new LayoutException("Unsupported symbol \""+symbol+'"');
    }
  }

  @Nonnull
  LayoutScope currentScope() {
    Preconditions.checkState(stackSize > 0);
    return scopeStack[stackSize - 1];
  }

  @Nonnull
  LayoutScope parentScope() {
    Preconditions.checkState(stackSize > 0);
    return scopeStack[stackSize - 2];
  }

  void pushScope(@Nonnull final DisplayObject displayObject,
                 final float width,
                 final float height) {
    scopeStack = ArrayUtil.append(
        scopeStack,
        stackSize++,
        LayoutScope.createPooled(displayObject, width, height),
        LayoutScope.ARRAY_FACTORY);
  }

  void popScope() {
    Preconditions.checkState(stackSize > 0);
    scopeStack[--stackSize].dispose();
    scopeStack[  stackSize] = null;
  }

  @Nonnull
  public Dispatcher dispatcher() {
    return dispatcher;
  }

  void dispatcher(@Nonnull final Dispatcher value) {
    dispatcher = value;
  }
}
