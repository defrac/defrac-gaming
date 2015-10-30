/*
 * Copyright 2015 defrac inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package defrac.resource;

import defrac.concurrent.Future;
import defrac.display.TextureAtlas;
import defrac.display.TextureDataSupplies;
import defrac.display.TextureDataSupply;
import defrac.display.atlas.libgdx.LibgdxTextureAtlas;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.Charset;

/**
 *
 */
public final class LibgdxTextureAtlasResource extends Resource<TextureAtlas> {
  public interface Listener extends GenericListener<LibgdxTextureAtlasResource, TextureAtlas> {}

  public static abstract class SimpleListener implements Listener {
    /** {@inheritDoc} */
    @Override
    public void onResourceStart(@Nonnull final LibgdxTextureAtlasResource resource) {}

    /** {@inheritDoc} */
    @Override
    public void onResourceProgress(@Nonnull final LibgdxTextureAtlasResource resource, final float progress) {}

    /** {@inheritDoc} */
    @Override
    public void onResourceComplete(@Nonnull final LibgdxTextureAtlasResource resource, @Nonnull final TextureAtlas content) {}

    /** {@inheritDoc} */
    @Override
    public void onResourceCancel(@Nonnull final LibgdxTextureAtlasResource resource) {}

    /** {@inheritDoc} */
    @Override
    public void onResourceError(@Nonnull final LibgdxTextureAtlasResource resource, @Nonnull final Throwable reason) {}

    /** {@inheritDoc} */
    @Override
    public void onResourceUpdate(@Nonnull final LibgdxTextureAtlasResource resource, @Nonnull final TextureAtlas content) {}
  }

  @Nonnull
  public static LibgdxTextureAtlasResource from(@Nonnull final String path) {
    return from(path, Charset.defaultCharset(), TextureDataSupplies.resource());
  }

  @Nonnull
  public static LibgdxTextureAtlasResource from(@Nonnull final String path,
                                                @Nonnull final Charset charset) {
    return from(path, charset, TextureDataSupplies.resource());
  }

  @Nonnull
  public static LibgdxTextureAtlasResource from(@Nonnull final String path,
                                                @Nonnull final TextureDataSupply supply) {
    return from(path, Charset.defaultCharset(), supply);
  }

  @Nonnull
  public static LibgdxTextureAtlasResource from(@Nonnull final String path,
                                                @Nonnull final Charset charset,
                                                @Nonnull final TextureDataSupply supply) {
    return new LibgdxTextureAtlasResource(path, charset, supply);
  }

  @Nonnull
  private final Charset charset;

  @Nonnull
  private final TextureDataSupply supply;

  private LibgdxTextureAtlasResource(@Nonnull final String path, @Nonnull final Charset charset, @Nonnull final TextureDataSupply supply) {
    super(path);
    this.charset = charset;
    this.supply = supply;
  }

  @Nonnull
  public LibgdxTextureAtlasResource listener(@Nullable final Listener listener) {
    //noinspection UnnecessaryLocalVariable
    final Object untypedListener = listener;
    @SuppressWarnings("unchecked")
    final GenericListener<Resource<TextureAtlas>, TextureAtlas> uncheckedListener =
        (GenericListener<Resource<TextureAtlas>, TextureAtlas>)untypedListener;
    genericListener(uncheckedListener);
    return this;
  }

  @Nullable
  public Listener listener() {
    final Object untypedListener = genericListener();
    @SuppressWarnings("unchecked")
    final Listener listener = (Listener)untypedListener;
    return listener;
  }

  @Nonnull
  @Override
  protected Future<TextureAtlas> internalLoad(final boolean dispatchEvents) {
    final Future<TextureAtlas> result =
        LibgdxTextureAtlas.load(
            StringResource.from(path, charset), supply);

    if(dispatchEvents) {
      result.
          onFailure(this::dispatchError).
          onSuccess(this::dispatchComplete);
    }

    try {
      if(dispatchEvents) {
        dispatchStart();
      }
    } catch(final Throwable error) {
      if(dispatchEvents) {
        dispatchError(error);
      }
    }

    return result;
  }

  @Override
  protected void internalCancel() {}
}
