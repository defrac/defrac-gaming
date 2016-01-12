package defrac.display.layout;

import defrac.display.DisplayObject;
import defrac.json.JSON;
import defrac.json.JSONObject;
import defrac.lang.Function;

import javax.annotation.Nonnull;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
final class LayoutObjectBuilderFactory {
  @Nonnull
  private final LayoutContext context;

  public LayoutObjectBuilderFactory(@Nonnull final LayoutContext context) {
    this.context = context;
  }

  /**
   * Creates and returns a new builder for the given qualified class name
   *
   * <p>This method resolves the best constructor for the class and creates
   * a builder based on this constructor. The builder itself stores lookups
   * for each property. This keeps costly reflection lookups to a minimum.
   *
   * @param qname The qualified class name
   * @return The builder for the class
   */
  @Nonnull
  public LayoutObjectBuilder<DisplayObject> newInstance(@Nonnull final String qname) {
    final Class<?> klass;

    try {
      klass = Class.forName(qname);
    } catch(final ClassNotFoundException exception) {
      throw new LayoutException("Can't find "+qname, exception);
    }

    final Constructor<?> constructor = findBestConstructor(klass);
    final LayoutObjectBuilder<DisplayObject> newBuilder =
        new ReflectingLayoutObjectBuilder(constructor, klass);

    context.registerBuilder(qname, newBuilder);

    return newBuilder;
  }

  /**
   * Creates and returns a StoredProcedure for the property
   *
   * <p>This method will try to find a property setter in the given
   * class for the property name. Since overloading might apply we
   * go first for a property that accepts a float, then an integer
   * and finally a string.
   *
   * @param context The current layout context
   * @param property The property
   * @param klass The class reference
   *
   * @return The StoredProcedure for the given property
   */
  @Nonnull
  private StoredProcedure newPropertyProcedure(@Nonnull final LayoutContext context,
                                                 @Nonnull final String property,
                                                 @Nonnull final Class<?> klass) {
    Method method;
    Function<JSON, Object> mapping;

    try {
      method = klass.getMethod(property, float.class);
      mapping = context::resolveValue;
    } catch(final NoSuchMethodException exception) {
      method = null;
      mapping = null;
    }

    if(method == null) {
      try {
        method = klass.getMethod(property, int.class);
        mapping = in -> (int)context.resolveValue(in);
      } catch(final NoSuchMethodException exception) {
        method = null;
        mapping = null;
      }
    }

    if(method == null) {
      try {
        method = klass.getMethod(property, String.class);
        mapping = context::interpolateString;
      } catch(final NoSuchMethodException exception) {
        method = null;
        mapping = null;
      }
    }

    if(method == null) {
      return StoredProcedure.EMPTY;
    }

    final Method finalMethod = method;
    final Function<JSON, Object> finalMapping = mapping;

    return (d, p) -> {
      JSON value = context.resolveProperty(p, property);

      if(value != null) {
        try {
          finalMethod.invoke(d, finalMapping.apply(value));
        } catch(final InvocationTargetException | IllegalAccessException exception) {
          throw new LayoutException("Can't set property \""+property+"\" for "+klass.getName(), exception);
        }
      }
    };
  }

  /**
   * Finds and returns the best matching constructor for a given class
   *
   * <p>The best matching constructor is a publicly declared constructor that has
   * two floating point parameters. If no such constructor is found the next best
   * constructor is a publicly declared constructor with no parameters at all.
   * In case no constructor matches any of the criteria <strong>no</strong> constructor
   * is found and this method throws a {@link LayoutException}.
   *
   * @param klass The class reference
   * @return The constructor for the class
   * @throws LayoutException If no applicable constructor is found
   */
  @Nonnull
  private Constructor<?> findBestConstructor(@Nonnull final Class<?> klass) {
    final Constructor<?>[] constructors = klass.getDeclaredConstructors();

    Constructor<?> defaultConstructor = null;

    for(final Constructor<?> candidate : constructors) {
      if(candidate.isSynthetic()) {
        continue;
      }

      if(!Modifier.isPublic(candidate.getModifiers())) {
        continue;
      }

      final Class<?>[] parameterTypes = candidate.getParameterTypes();

      if(parameterTypes.length == 0) {
        defaultConstructor = candidate;
      } else if(parameterTypes.length == 2
          && parameterTypes[0] == float.class
          && parameterTypes[1] == float.class) {
        return candidate;
      }
    }

    if(null == defaultConstructor) {
      throw new LayoutException("No applicable constructor for "+klass.getName());
    }

    return defaultConstructor;
  }

  private class ReflectingLayoutObjectBuilder extends LayoutObjectBuilder<DisplayObject> {
    private final boolean isArity2;

    @Nonnull
    private final Map<String, StoredProcedure> storedProcedures;

    @Nonnull
    private final Constructor<?> constructor;

    @Nonnull
    private final Class<?> klass;

    @Nonnull
    private final JSONObject temp = new JSONObject();

    public ReflectingLayoutObjectBuilder(@Nonnull final Constructor<?> constructor,
                                         @Nonnull final Class<?> klass) {
      this.isArity2 = constructor.getParameterTypes().length == 2;
      this.constructor = constructor;
      this.klass = klass;
      this.storedProcedures = new HashMap<>();
    }

    @Nonnull
    @Override
    protected DisplayObject newInstance(@Nonnull final LayoutContext context,
                                        final float width,
                                        final float height) {
      final DisplayObject result;

      try {
        // we could split this up beforehand but code is complex enough
        result = (DisplayObject)(isArity2 ? constructor.newInstance(width, height) : constructor.newInstance());
      } catch(final InstantiationException | IllegalAccessException | InvocationTargetException exception) {
        throw new LayoutException("Can't instantiate "+klass.getName(), exception);
      }

      if(!isArity2) {
        if(width >= 0.0f) {
          temp.put(LayoutConstants.KEY_WIDTH, width);
          getOrCreateStoredProcedure(context, LayoutConstants.KEY_WIDTH).apply(result, temp);
        }

        if(height >= 0.0f) {
          temp.put(LayoutConstants.KEY_HEIGHT, height);
          getOrCreateStoredProcedure(context, LayoutConstants.KEY_HEIGHT).apply(result, temp);
        }
      }

      return result;
    }

    @Override
    protected void applyProperties(@Nonnull final LayoutContext context,
                                   @Nonnull final JSONObject properties,
                                   @Nonnull final DisplayObject displayObject) {
      super.applyProperties(context, properties, displayObject);

      for(final String property : properties.keySet()) {
        // we ignore all default properties and variant properties
        if(LayoutConstants.DEFAULT_PROPERTIES.contains(property) || context.isVariant(property)) {
          continue;
        }

        // create and apply new stored procedure for this property
        final StoredProcedure procedure =
            getOrCreateStoredProcedure(context, property);

        procedure.apply(displayObject, properties);
      }
    }

    @Nonnull
    private StoredProcedure getOrCreateStoredProcedure(final @Nonnull LayoutContext context, final String property) {
      final StoredProcedure storedProcedure = storedProcedures.get(property);

      if(storedProcedure != null) {
        return storedProcedure;
      }

      final StoredProcedure procedure =
          newPropertyProcedure(context, property, klass);

      storedProcedures.put(property, procedure);

      return procedure;
    }
  }

  private interface StoredProcedure {
    @Nonnull
    StoredProcedure EMPTY = (displayObject, property) -> {};

    void apply(@Nonnull final DisplayObject displayObject, @Nonnull final JSONObject property);
  }

}
