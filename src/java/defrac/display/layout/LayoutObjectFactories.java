package defrac.display.layout;

import defrac.display.*;
import defrac.json.JSON;
import defrac.json.JSONObject;
import defrac.util.Color;

import javax.annotation.Nonnull;

/**
 *
 */
public final class LayoutObjectFactories {
  @Nonnull
  public static final LayoutObjectFactory.Functional<Canvas> CANVAS_FACTORY =
      new LayoutObjectFactory.Functional<>(Canvas::new);

  @Nonnull
  public static final LayoutObjectFactory.Functional<GLSurface> GL_SURFACE_FACTORY =
      new LayoutObjectFactory.Functional<>(GLSurface::new);

  @Nonnull
  public static final LayoutObjectFactory.Functional<Image> IMAGE_FACTORY =
      new LayoutObjectFactory.Functional<>((width, height) -> {
        final Image image = new Image();
        if(width >= 0.0f) image.width(width);
        if(height >= 0.0f) image.height(height);
        return image;
      });

  @Nonnull
  public static final LayoutObjectFactory.Functional<Label> LABEL_FACTORY =
      new LayoutObjectFactory.Functional<>((width, height) -> {
        final Label label = new Label();
        if(width >= 0.0f) label.width(width);
        if(height >= 0.0f) label.height(height);
        return label;
      });

  @Nonnull
  public static final LayoutObjectFactory.Functional<Layer> LAYER_FACTORY =
      new LayoutObjectFactory.Functional<>((width, height) -> new Layer());

  @Nonnull
  public static final LayoutObjectFactory.Functional<MovieClip> MOVIE_CLIP_FACTORY =
      new LayoutObjectFactory.Functional<>((width, height) -> {
        final MovieClip movieClip = new MovieClip();
        if(width >= 0.0f) movieClip.width(width);
        if(height >= 0.0f) movieClip.height(height);
        return movieClip;
      });

  public static final LayoutObjectFactory<Quad> QUAD_FACTORY = new LayoutObjectFactory<Quad>() {
    @Nonnull
    @Override
    protected Quad newInstance(final float width, final float height) {
      return new Quad(width, height, 0x00000000);
    }

    @Override
    protected void configure(@Nonnull final LayoutContext context,
                             @Nonnull final JSONObject properties,
                             @Nonnull final Quad quad) {
      super.configure(context, properties, quad);

      final JSON property = properties.opt("color", null);

      if(null != property) {
        if(property.isString()) {
          quad.color(Color.valueOf(property.stringValue()));
        } else {
          quad.color(property.intValue());
        }
      }
    }
  };

  @Nonnull
  public static final LayoutObjectFactory.Functional<Scale3Image> SCALE3_IMAGE_FACTORY =
      new LayoutObjectFactory.Functional<>((width, height) -> {
        final Scale3Image image = new Scale3Image();
        if(width >= 0.0f) image.width(width);
        if(height >= 0.0f) image.height(height);
        return image;
      });

  @Nonnull
  public static final LayoutObjectFactory.Functional<Scale9Image> SCALE9_IMAGE_FACTORY =
      new LayoutObjectFactory.Functional<>((width, height) -> {
        final Scale9Image image = new Scale9Image();
        if(width >= 0.0f) image.width(width);
        if(height >= 0.0f) image.height(height);
        return image;
      });


  @Nonnull
  public static final LayoutObjectFactory.Functional<Stats> STATS_FACTORY =
      new LayoutObjectFactory.Functional<>((width, height) -> new Stats());

  public static void registerDefaults(@Nonnull final LayoutContext context) {
    context.registerFactory("defrac.display.Canvas", CANVAS_FACTORY);
    context.registerFactory("defrac.display.GLSurface", GL_SURFACE_FACTORY);
    context.registerFactory("defrac.display.Image", IMAGE_FACTORY);
    context.registerFactory("defrac.display.Label", LABEL_FACTORY);
    context.registerFactory("defrac.display.Layer", LAYER_FACTORY);
    context.registerFactory("defrac.display.MovieClip", MOVIE_CLIP_FACTORY);
    context.registerFactory("defrac.display.Quad", QUAD_FACTORY);
    context.registerFactory("defrac.display.Scale3Image", SCALE3_IMAGE_FACTORY);
    context.registerFactory("defrac.display.Scale9Image", SCALE9_IMAGE_FACTORY);
    context.registerFactory("defrac.display.Stats", STATS_FACTORY);
  }

  private LayoutObjectFactories() {}
}
