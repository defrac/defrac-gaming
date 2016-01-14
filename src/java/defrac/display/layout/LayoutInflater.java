package defrac.display.layout;

import defrac.concurrent.Dispatcher;
import defrac.concurrent.Future;
import defrac.concurrent.Futures;
import defrac.display.DisplayObject;
import defrac.display.DisplayObjectContainer;
import defrac.display.Stage;
import defrac.json.JSON;
import defrac.json.JSONArray;
import defrac.json.JSONNumber;
import defrac.json.JSONObject;
import defrac.lang.Strings;
import defrac.lang.Void;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static defrac.display.layout.LayoutConstants.*;

/**
 *
 */
public final class LayoutInflater {
  @Nonnull
  private static final JSONNumber NEGATIVE_ONE = JSONNumber.of(-1.0f);

  @Nonnull
  private final LayoutContext context;

  public LayoutInflater(@Nonnull final LayoutContext context) {
    this.context = context;
  }

  @Nonnull
  public Future<Void> inflate(@Nonnull final Stage stage,
                              @Nonnull final JSON layout) {
    return inflate(stage, layout, stage, stage.width(), stage.height());
  }

  @Nonnull
  public Future<Void> inflate(@Nonnull final Stage stage,
                              @Nonnull final JSON layout,
                              final float availableWidth,
                              final float availableHeight) {
    return inflate(stage, layout, stage, availableWidth, availableHeight);
  }

  public Future<Void> inflate(@Nonnull final DisplayObjectContainer root,
                              @Nonnull final JSON layout,
                              @Nonnull final Dispatcher dispatcher,
                              final float availableWidth,
                              final float availableHeight) {
    if(!layout.isObject()) {
      Futures.failure(new LayoutException("Given layout isn't a JSON object"));
    }

    context.dispatcher(dispatcher);

    final JSONObject layoutObject = (JSONObject)layout;

    inflateConstants(layoutObject);

    return inflateChildren(root, layoutObject, availableWidth, availableHeight);
  }

  private void inflateConstants(final @Nonnull JSONObject layout) {
    final JSONObject constants = rootConstants(layout);

    for(final String key : constants.keySet()) {
      if(context.isVariant(key)) {
        continue;
      }

      context.defineConstant(key, context.resolveString(constants, key));
    }
  }
  @Nonnull
  private Future<Void> inflateChildren(final @Nonnull DisplayObjectContainer root,
                                       final @Nonnull JSONObject layout,
                                       final float availableWidth,
                                       final float availableHeight) {
    // We push the root so that we get a valid parent width and
    // height for top-level children
    context.pushScope(root, availableWidth, availableHeight);

    // We start inflating all the children which is an asynchronous process.
    final Future<Void> future = inflateDescriptors(root, rootChildren(layout));

    // Exit the root scope when we're done
    future.onComplete(attempt -> context.popScope(), context.dispatcher());

    return future;
  }

  /**
   * Resolves and returns the constants object in the layout root
   *
   * @param layoutRoot The layout root
   * @return The constants in the layout root
   */
  @Nonnull
  private JSONObject rootConstants(@Nonnull final JSONObject layoutRoot) {
    return context.resolveProperty(layoutRoot, KEY_CONSTANTS, JSONObject.EMPTY).asObject(JSONObject.EMPTY);
  }

  /**
   * Resolves and returns the controls array in the layout root
   *
   * @param layoutRoot The layout root
   * @return An array of controls in the layout root
   */
  @Nonnull
  private JSONArray rootChildren(@Nonnull final JSONObject layoutRoot) {
    return context.
        resolveProperty(layoutRoot, KEY_CHILDREN, JSONArray.EMPTY).
        asArray(JSONArray.EMPTY);
  }

  /**
   * Inflates a descriptor into a parent
   *
   * <p>This method handles repeated inflation of a given descriptor
   *
   * @param parent The parent
   * @param descriptor The descriptor
   *
   * @return The Future representing the completion of inflating the descriptor
   */
  @Nonnull
  private Future<Void> inflate(@Nonnull final DisplayObjectContainer parent,
                               @Nullable final JSONObject descriptor) {
    if(descriptor == null) {
      return success();
    }

    final int repeatCount = descriptor.optInt(KEY_REPEAT, 1);

    return repeatedInflate(parent, descriptor, repeatCount);
  }

  /**
   * Inflates a descriptor repeatedly into a parent
   *
   * <p>This method should only be called from {@link #inflate(DisplayObjectContainer, JSONObject)}
   * and not manually.
   *
   * @param parent The parent
   * @param descriptor The descriptor
   * @param repeatCount The number of repetitions
   *
   * @return The Future representing the completion of inflating the descriptor {@code repeatCount} times
   */
  @Nonnull
  private Future<Void> repeatedInflate(@Nonnull final DisplayObjectContainer parent,
                                       @Nonnull final JSONObject descriptor,
                                       final int repeatCount) {
    final Future<Void> future;

    if(repeatCount == 0) {
      future = success();
    } else if(repeatCount == 1) {
      future = inflateRepetition(parent, descriptor, 0);
    } else {
      future = repeatedInflateTrampoline(parent, descriptor, 0, repeatCount);
    }

    return future;
  }

  /**
   * Trampoline for repeated asynchronous inflation of a descriptor into a parent
   *
   * @param parent The parent
   * @param descriptor The descriptor
   * @param repeatIndex The current index in the repeated sequence
   * @param repeatCount The number of repetitions
   *
   * @return The Future representing the completion of inflating the descriptor {@code repeatCount - repeatIndex} times
   */
  @Nonnull
  private Future<Void> repeatedInflateTrampoline(@Nonnull final DisplayObjectContainer parent,
                                                 @Nonnull final JSONObject descriptor,
                                                 final int repeatIndex,
                                                 final int repeatCount) {
    final Future<Void> future = inflateRepetition(parent, descriptor, repeatIndex);

    return future.flatMap(theVoid -> {
      final int nextRepeatIndex = repeatIndex + 1;

      if(nextRepeatIndex == repeatCount) {
        return success();
      } else {
        return repeatedInflateTrampoline(parent, descriptor, nextRepeatIndex, repeatCount);
      }
    });
  }

  /**
   * Inflates a descriptor into the parent for the given repetition index
   *
   * @param parent The parent
   * @param descriptor The descriptor
   * @param repeatIndex The current repetition index
   *
   * @return The Future representing the completion of inflating the descriptor
   */
  @Nonnull
  private Future<Void> inflateRepetition(@Nonnull final DisplayObjectContainer parent,
                                         @Nonnull final JSONObject descriptor,
                                         final int repeatIndex) {
    final String type = descriptor.getString(KEY_TYPE);

    if(Strings.isNullOrEmpty(type)) {
      return Futures.failure(new LayoutException("Layout object without a type.\n"+JSON.stringify(descriptor, /*pretty=*/true)));
    }

    // Define the ${repeatIndex}
    context.defineConstant(VARIABLE_REPEAT_INDEX, String.valueOf(repeatIndex));

    // Instantiate the DisplayObject using its DisplayObjectInflater
    final DisplayObjectInflater inflater = context.getOrCreateInflaterByType(type);
    final DisplayObject displayObject = newDisplayObject(descriptor, inflater);

    // Inflate the DisplayObject and its (potential) children
    final Future<Void> future =
        inflater.
            inflate(context, descriptor, displayObject).
            flatMap(theVoid -> {
                  // At this point, the DisplayObject has been inflated and we're ready to
                  // add it to its parent
                  parent.addChild(displayObject);

                  // If it's a DisplayObjectContainer we might have to inflate additional
                  // children which is a Future itself.
                  if(displayObject instanceof DisplayObjectContainer) {
                    final DisplayObjectContainer container = (DisplayObjectContainer) displayObject;
                    return inflateDescriptors(container, descriptor.optArray(KEY_CHILDREN));
                  }

                  return success();
                }, context.dispatcher()
            );

    // Exit the scope of this DisplayObject when we're done
    future.onComplete(attempt -> context.popScope(), context.dispatcher());

    return future;
  }

  /**
   * Inflates an array of descriptors into a parent
   *
   * <p>This method should only be called from {@link #inflateRepetition(DisplayObjectContainer, JSONObject, int)}
   * and not manually.
   *
   * @param parent The parent
   * @param descriptors The array of descriptors
   *
   * @return The Future representing the completion of inflating the descriptors
   */
  @Nonnull
  private Future<Void> inflateDescriptors(@Nonnull final DisplayObjectContainer parent,
                                          @Nonnull final JSONArray descriptors) {
    final int descriptorCount = descriptors.length();

    final Future<Void> future;

    if(0 == descriptorCount) {
      future = success();
    } else if(1 == descriptorCount) {
      final JSON firstChild = descriptors.get(0);

      if(firstChild == null || !firstChild.isObject()) {
        future = success();
      } else {
        future = inflate(parent, (JSONObject)firstChild);
      }
    } else {
      future = inflateDescriptorsTrampoline(parent, descriptors, 0, descriptorCount);
    }

    return future;
  }

  /**
   * Trampoline for asynchronous inflation of an array of descriptors into a parent
   *
   * @param parent The parent
   * @param descriptors The descriptor
   * @param descriptorIndex The current index in the descriptors array
   * @param descriptorCount The length of the descriptors array
   *
   * @return The Future representing the completion of inflating the descriptor {@code repeatCount - repeatIndex} times
   */
  @Nonnull
  private Future<Void> inflateDescriptorsTrampoline(@Nonnull final DisplayObjectContainer parent,
                                                    @Nonnull final JSONArray descriptors,
                                                    final int descriptorIndex,
                                                    final int descriptorCount) {
    final JSON descriptor = descriptors.get(descriptorIndex);
    final Future<Void> future = inflate(parent, descriptor == null ? null : descriptor.asObject());

    return future.flatMap(theVoid -> {
      final int nextDescriptorIndex = descriptorIndex + 1;

      if(nextDescriptorIndex == descriptorCount) {
        return success();
      } else {
        return inflateDescriptorsTrampoline(parent, descriptors, nextDescriptorIndex, descriptorCount);
      }
    }, context.dispatcher());
  }

  /**
   * Creates and returns a new DisplayObject for the descriptor
   *
   * <p>The display object will have its width, height and name set. It is also registered in the context
   * for its identifier.
   *
   * @param descriptor The descriptor
   * @param inflater The inflater for the DisplayObject
   *
   * @return The DisplayObject instance
   */
  @Nonnull
  private DisplayObject newDisplayObject(final @Nonnull JSONObject descriptor, final DisplayObjectInflater inflater) {
    final String id = context.resolveString(descriptor, KEY_ID);

    // Resolve the width and height property in the JSON
    // and default to -1 if absent
    final JSON jsonWidth = context.resolveProperty(descriptor, KEY_WIDTH, NEGATIVE_ONE);
    final JSON jsonHeight = context.resolveProperty(descriptor, KEY_HEIGHT, NEGATIVE_ONE);

    // The width and height of the current scope are the parent's dimension
    // for the new DisplayObject we're going to instantiate
    final float parentWidth = context.currentScope().width;
    final float parentHeight = context.currentScope().height;

    // The resulting width and height for the DisplayObject based on the parent's dimension
    final float width = context.resolvePercentage(parentWidth, jsonWidth);
    final float height = context.resolvePercentage(parentHeight, jsonHeight);

    final DisplayObject displayObject = inflater.newInstance(context, width, height);

    if(!Strings.isNullOrEmpty(id)) {
      context.storeDisplayObjectForId(id, displayObject, width, height);
      displayObject.name(id);
    }

    if(displayObject instanceof DisplayObjectContainer) {
      // DisplayObjectContainer inherits the parent dimension if not
      // explicitly specified.
      final float inheritedOrDefinedWidth  = width  < 0 ? parentWidth  : width;
      final float inheritedOrDefinedHeight = height < 0 ? parentHeight : height;
      context.pushScope(displayObject, inheritedOrDefinedWidth, inheritedOrDefinedHeight);
    } else {
      context.pushScope(displayObject, width, height);
    }

    return displayObject;
  }

  /**
   * Creates and returns a successful {@code Future<Void>} instance
   *
   * @return A successful {@code Future<Void>} instance
   */
  @Nonnull
  private static Future<Void> success() {
    return Futures.success(Void.INSTANCE);
  }
}
