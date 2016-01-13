package defrac.display.layout.inflater;

import defrac.display.layout.LayoutContext;

import javax.annotation.Nonnull;

/**
 *
 */
public final class DefaultDisplayObjectInflaters {
  public static void register(@Nonnull final LayoutContext context) {
    context.registerInflater("defrac.display.Canvas", new CanvasInflater());
    context.registerInflater("defrac.display.GLSurface", new GLSurfaceInflater());
    context.registerInflater("defrac.display.Image", new ImageInflater());
    context.registerInflater("defrac.display.Label", new LabelInflater());
    context.registerInflater("defrac.display.Layer", new LayerInflater());
    context.registerInflater("defrac.display.MovieClip", new MovieClipInflater());
    context.registerInflater("defrac.display.Quad", new QuadInflater());
    context.registerInflater("defrac.display.Scale3Image", new Scale3ImageInfalter());
    context.registerInflater("defrac.display.Scale9Image", new Scale9ImageInfalter());
    context.registerInflater("defrac.display.Stats", new StatsInflater());
  }

  private DefaultDisplayObjectInflaters() {}
}
