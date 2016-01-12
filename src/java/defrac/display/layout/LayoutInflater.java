package defrac.display.layout;

import defrac.display.DisplayObject;
import defrac.display.DisplayObjectContainer;
import defrac.json.JSON;
import defrac.json.JSONArray;
import defrac.json.JSONObject;
import defrac.lang.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static defrac.display.layout.LayoutConstants.*;

/**
 *
 */
public final class LayoutInflater {
  @Nonnull
  private final LayoutContext context;

  public LayoutInflater(@Nonnull final LayoutContext context) {
    this.context = context;
  }

  public void inflate(@Nonnull final DisplayObjectContainer root,
                      @Nonnull final JSON layout) {
    context.pushScope(root);

    inflate(root, layout.asObject());

    addChildren(root, layout.asArray(JSONArray.EMPTY));

    context.popScope();
  }

  private void inflate(@Nonnull final DisplayObjectContainer container,
                       @Nullable final JSONObject json) {
    if(json == null) {
      return;
    }

    final int repeatCount = json.optInt(KEY_REPEAT, 1);

    for(int repeatIndex = 0; repeatIndex < repeatCount; ++repeatIndex) {
      inflate(container, json, repeatIndex);
    }
  }


  private void inflate(@Nonnull final DisplayObjectContainer parent,
                       @Nonnull final JSONObject json,
                       final int repeatIndex) {
    context.defineVariable(VARIABLE_REPEAT_INDEX, String.valueOf(repeatIndex));

    final String type = json.getString(KEY_TYPE);

    if(Strings.isNullOrEmpty(type)) {
      return;
    }

    final LayoutObjectBuilder<?> builder =
        context.findBuilderByType(type);

    if(builder == null) {
      return;
    }

    context.pushScope(null);

    final DisplayObject displayObject =
        builder.build(context, json);

    parent.addChild(displayObject);

    if(displayObject instanceof DisplayObjectContainer) {
      final DisplayObjectContainer container = (DisplayObjectContainer)displayObject;
      addChildren(container, json.optArray(KEY_CHILDREN));
    }

    context.popScope();
  }

  private void addChildren(@Nonnull final DisplayObjectContainer container,
                           @Nonnull final JSONArray children) {
    for(final JSON child : children) {
      inflate(container, child.asObject());
    }
  }
}
