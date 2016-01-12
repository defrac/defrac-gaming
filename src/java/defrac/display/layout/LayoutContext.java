package defrac.display.layout;

import defrac.display.BlendMode;
import defrac.display.DisplayObject;
import defrac.display.DisplayObjectContainer;
import defrac.json.JSON;
import defrac.json.JSONObject;
import defrac.lang.Preconditions;
import defrac.util.ArrayUtil;
import defrac.util.Dictionary;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 *
 */
public class LayoutContext {
  @Nonnull public static final String KEY_TYPE = "type";
  @Nonnull public static final String KEY_CHILDREN = "children";
  @Nonnull public static final String KEY_WIDTH = "width";
  @Nonnull public static final String KEY_HEIGHT = "height";
  @Nonnull public static final String KEY_ID = "id";

  @Nonnull
  private final Dictionary<LayoutObjectFactory<?>> factories = new Dictionary<>();

  @Nonnull
  private final Dictionary<DisplayObject> displayObjects = new Dictionary<>();

  @Nonnull
  private DisplayObject[] scopeStack = DisplayObject.ARRAY_FACTORY.create(256);

  private int stackSize = 0;

  @Nonnull
  private final EvalParser parser = new EvalParser();

  public LayoutContext() {
    registerFactories();
  }

  protected void registerFactories() {
    LayoutObjectFactories.registerDefaults(this);
  }

  public void registerFactory(@Nonnull final String type,
                              @Nonnull final LayoutObjectFactory<?> factory) {
    if(null != factories.put(type, factory)) {
      throw new IllegalStateException("Duplicate factory for type \""+type+'"');
    }
  }

  @Nullable
  public DisplayObject findDisplayObjectById(@Nonnull final String id) {
    return displayObjects.get(id);
  }

  public void build(@Nonnull final DisplayObjectContainer root,
                    @Nonnull final JSON layout) {
    displayObjects.clear();

    pushScope(root);

    final DisplayObject layoutRoot = build(layout.asObject());

    if(layoutRoot != null) {
      root.addChild(layoutRoot);
    }

    popScope();
  }

  @Nullable
  DisplayObject build(@Nullable final JSONObject json) {
    if(json == null) {
      return null;
    }

    final String type = json.getString(KEY_TYPE);
    final LayoutObjectFactory<?> layoutObjectFactory = factories.get(type);

    if(layoutObjectFactory == null) {
      return null;
    }

    pushScope(null);
    final DisplayObject result = layoutObjectFactory.build(this, json);
    popScope();

    return result;
  }

  float resolvePercentage(final float parentValue, @Nonnull final JSON childValue) {
    if(childValue.isString()) {
      final String stringValue = childValue.stringValue();
      final int indexOfLastChar = stringValue.length() - 1;
      final int firstChar = stringValue.charAt(0);

      if(stringValue.indexOf('%') == indexOfLastChar && firstChar >= '0' && firstChar <= '9') {
        return parentValue * 0.01f * Float.parseFloat(stringValue.substring(0, indexOfLastChar));
      }
    }

    return resolveValue(childValue);
  }

  float resolveValue(@Nonnull final JSON value) {
    if(value.isString()) {
      return parser.parse(value.stringValue()).evaluate(this);
    }

    return value.floatValue();
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

  void setThis(@Nonnull final DisplayObject displayObject) {
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
      throw new IllegalStateException("Duplicate identifier \""+identifier+'"');
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

  public float fieldGet(final String receiver,
                        final String symbol) {
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
      throw new IllegalArgumentException("DisplayObject \""+receiver+"\" doesn't exist");
    }

    switch(symbol) {
      case "x": return displayObject.x();
      case "y": return displayObject.y();
      case "width": return displayObject.width();
      case "height": return displayObject.height();
      case "alpha": return displayObject.alpha();
      case "scaleX": return displayObject.scaleX();
      case "scaleY": return displayObject.scaleY();
      case "regX": return displayObject.registrationPointX();
      case "regY": return displayObject.registrationPointY();
      case "visible": return displayObject.visible() ? 1.0f : 0.0f;
      default: throw new IllegalArgumentException("Unsupported symbol \""+symbol+'"');
    }
  }
}
