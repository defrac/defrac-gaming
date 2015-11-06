/******************************************************************************
 * Spine Runtimes Software License
 * Version 2.3
 * <p>
 * Copyright (c) 2013-2015, Esoteric Software
 * All rights reserved.
 * <p>
 * You are granted a perpetual, non-exclusive, non-sublicensable and
 * non-transferable license to use, install, execute and perform the Spine
 * Runtimes Software (the "Software") and derivative works solely for personal
 * or internal use. Without the written permission of Esoteric Software (see
 * Section 2 of the Spine Software License Agreement), you may not (a) modify,
 * translate, adapt or otherwise create derivative works, improvements of the
 * Software or develop new applications using the Software or (b) remove,
 * delete, alter or obscure any trademarks or any copyright, trademark, patent
 * or other intellectual property or proprietary rights notices on or in the
 * Software, including any copy thereof. Redistributions in binary or source
 * form must include this license and terms.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY ESOTERIC SOFTWARE "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL ESOTERIC SOFTWARE BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *****************************************************************************/

// Ported from the Spine runtime by defrac 2015.

package defrac.animation.spine.attachments;

import defrac.animation.spine.Skin;
import defrac.display.Texture;
import defrac.display.TextureAtlas;

import javax.annotation.Nonnull;

public final class AtlasAttachmentLoader implements AttachmentLoader {
  @Nonnull
  private final TextureAtlas atlas;

  public AtlasAttachmentLoader(@Nonnull final TextureAtlas atlas) {
    this.atlas = atlas;
  }

  @Nonnull
  @Override
  public RegionAttachment newRegionAttachment(Skin skin, @Nonnull final String name, @Nonnull final String path) {
    final Texture region = atlas.get(path);
    final RegionAttachment attachment = new RegionAttachment(name);

    attachment.region(region);

    return attachment;
  }

  @Nonnull
  @Override
  public MeshAttachment newMeshAttachment(Skin skin, @Nonnull final String name, @Nonnull final String path) {
    final Texture region = atlas.get(path);
    final MeshAttachment attachment = new MeshAttachment(name);

    attachment.region(region);

    return attachment;
  }

  @Nonnull
  @Override
  public SkinnedMeshAttachment newSkinnedMeshAttachment(Skin skin, @Nonnull final String name, @Nonnull final String path) {
    final Texture region = atlas.get(path);
    final SkinnedMeshAttachment attachment = new SkinnedMeshAttachment(name);

    attachment.region(region);

    return attachment;
  }

  @Nonnull
  @Override
  public BoundingBoxAttachment newBoundingBoxAttachment(Skin skin, @Nonnull final String name) {
    return new BoundingBoxAttachment(name);
  }
}
