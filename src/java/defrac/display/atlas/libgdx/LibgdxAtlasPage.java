package defrac.display.atlas.libgdx;

import defrac.display.TextureDataFormat;
import defrac.display.TextureDataRepeat;
import defrac.display.TextureDataSmoothing;
import defrac.lang.Maps;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * @hide
 */
final class LibgdxAtlasPage {
  @Nonnull
  public final String path;

  @Nonnull
  public  final TextureDataFormat format;

  @Nonnull
  public  final TextureDataRepeat repeat;

  @Nonnull
  public  final TextureDataSmoothing smoothing;

  @Nonnull
  public final Map<String, LibgdxAtlasRegion> mapping;

  public LibgdxAtlasPage(@Nonnull final String path,
                         @Nonnull final TextureDataFormat format,
                         @Nonnull final TextureDataRepeat repeat,
                         @Nonnull final TextureDataSmoothing smoothing) {
    this.path = path;
    this.format = format;
    this.repeat = repeat;
    this.smoothing = smoothing;
    this.mapping = Maps.newHashMap();
  }

  public void add(@Nonnull final String name,
                  @Nonnull final LibgdxAtlasRegion region) {
    final LibgdxAtlasRegion existing = mapping.put(name, region);

    if(existing != null) {
      region.next = existing;
    }
  }
}
