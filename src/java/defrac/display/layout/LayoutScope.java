package defrac.display.layout;

import defrac.display.DisplayObject;
import defrac.pool.ObjectPool;
import defrac.pool.ObjectPools;
import defrac.util.ArrayUtil;

import javax.annotation.Nonnull;

/**
 * The LayoutScope defines a scope in the layout
 */
public final class LayoutScope {
  @Nonnull
  public static final ArrayUtil.ArrayFactory<LayoutScope> ARRAY_FACTORY = LayoutScope[]::new;

  @Nonnull
  private static final ObjectPool<LayoutScope> POOL =
      ObjectPools.newPool(
          /*initialCapacity=*/8,
          LayoutScope::new,
          scope -> scope.displayObject = null);

  @Nonnull
  static LayoutScope createPooled(@Nonnull final DisplayObject displayObject,
                                  final float width,
                                  final float height) {
    final LayoutScope scope = POOL.get();

    initialize(scope, displayObject, width, height);

    return scope;
  }

  @Nonnull
  static LayoutScope create(@Nonnull final DisplayObject displayObject,
                            final float width,
                            final float height) {
    final LayoutScope scope = new LayoutScope();

    initialize(scope, displayObject, width, height);

    return scope;
  }

  private static void initialize(@Nonnull final LayoutScope scope,
                                 @Nonnull final DisplayObject displayObject,
                                 final float width,
                                 final float height) {
    scope.displayObject = displayObject;
    scope.width = width;
    scope.height = height;
  }

  public float width;

  public float height;

  public DisplayObject displayObject;

  private LayoutScope() {}

  void dispose() {
    POOL.ret(this);
  }
}
