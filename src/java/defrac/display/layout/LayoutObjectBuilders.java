package defrac.display.layout;

import defrac.display.*;
import defrac.json.JSON;
import defrac.json.JSONObject;
import defrac.util.Color;

import javax.annotation.Nonnull;

/**
 *
 */
public final class LayoutObjectBuilders {
  @Nonnull
  public static final LayoutObjectBuilder.Functional<Canvas> CANVAS_FACTORY =
      new LayoutObjectBuilder.Functional<>(Canvas::new);

  @Nonnull
  public static final LayoutObjectBuilder.Functional<GLSurface> GL_SURFACE_FACTORY =
      new LayoutObjectBuilder.Functional<>(GLSurface::new);

  @Nonnull
  public static final LayoutObjectBuilder.Functional<Image> IMAGE_FACTORY =
      new LayoutObjectBuilder.Functional<>((width, height) -> {
        final Image image = new Image();
        if(width >= 0.0f) image.width(width);
        if(height >= 0.0f) image.height(height);
        return image;
      });

  @Nonnull
  public static final LayoutObjectBuilder.Functional<Label> LABEL_FACTORY =
      new LayoutObjectBuilder.Functional<>((width, height) -> {
        final Label label = new Label();
        if(width >= 0.0f) label.width(width);
        if(height >= 0.0f) label.height(height);
        return label;
      });

  @Nonnull
  public static final LayoutObjectBuilder.Functional<Layer> LAYER_FACTORY =
      new LayoutObjectBuilder.Functional<>((width, height) -> new Layer());

  @Nonnull
  public static final LayoutObjectBuilder.Functional<MovieClip> MOVIE_CLIP_FACTORY =
      new LayoutObjectBuilder.Functional<>((width, height) -> {
        final MovieClip movieClip = new MovieClip();
        if(width >= 0.0f) movieClip.width(width);
        if(height >= 0.0f) movieClip.height(height);
        return movieClip;
      });

  public static final LayoutObjectBuilder<Quad> QUAD_FACTORY = new LayoutObjectBuilder<Quad>() {
    @Nonnull
    @Override
    protected Quad newInstance(@Nonnull final LayoutContext context,
                               final float width,
                               final float height) {
      return new Quad(width, height, 0x00000000);
    }

    @Override
    protected void applyProperties(@Nonnull final LayoutContext context,
                                   @Nonnull final JSONObject properties,
                                   @Nonnull final Quad quad) {
      super.applyProperties(context, properties, quad);

      final JSON property = context.resolveProperty(properties, "color");

      if(null != property) {
        if(property.isString()) {
          quad.color(Color.valueOf(context.interpolateString(property.stringValue())));
        } else {
          quad.color(property.intValue());
        }
      }
    }
  };

  @Nonnull
  public static final LayoutObjectBuilder.Functional<Scale3Image> SCALE3_IMAGE_FACTORY =
      new LayoutObjectBuilder.Functional<>((width, height) -> {
        final Scale3Image image = new Scale3Image();
        if(width >= 0.0f) image.width(width);
        if(height >= 0.0f) image.height(height);
        return image;
      });

  @Nonnull
  public static final LayoutObjectBuilder.Functional<Scale9Image> SCALE9_IMAGE_FACTORY =
      new LayoutObjectBuilder.Functional<>((width, height) -> {
        final Scale9Image image = new Scale9Image();
        if(width >= 0.0f) image.width(width);
        if(height >= 0.0f) image.height(height);
        return image;
      });


  @Nonnull
  public static final LayoutObjectBuilder.Functional<Stats> STATS_FACTORY =
      new LayoutObjectBuilder.Functional<>((width, height) -> new Stats());

  public static void registerDefaults(@Nonnull final LayoutContext context) {
    context.registerBuilder("defrac.display.Canvas", CANVAS_FACTORY);
    context.registerBuilder("defrac.display.GLSurface", GL_SURFACE_FACTORY);
    context.registerBuilder("defrac.display.Image", IMAGE_FACTORY);
    context.registerBuilder("defrac.display.Label", LABEL_FACTORY);
    context.registerBuilder("defrac.display.Layer", LAYER_FACTORY);
    context.registerBuilder("defrac.display.MovieClip", MOVIE_CLIP_FACTORY);
    context.registerBuilder("defrac.display.Quad", QUAD_FACTORY);
    context.registerBuilder("defrac.display.Scale3Image", SCALE3_IMAGE_FACTORY);
    context.registerBuilder("defrac.display.Scale9Image", SCALE9_IMAGE_FACTORY);
    context.registerBuilder("defrac.display.Stats", STATS_FACTORY);
  }

  private LayoutObjectBuilders() {}
}
