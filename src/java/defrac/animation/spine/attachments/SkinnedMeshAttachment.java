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

import defrac.animation.spine.Bone;
import defrac.animation.spine.Skeleton;
import defrac.animation.spine.Slot;
import defrac.display.Texture;
import defrac.util.ArrayUtil;
import defrac.util.Color;
import defrac.util.MathUtil;

import javax.annotation.Nonnull;
import java.util.Arrays;

import static defrac.lang.Preconditions.checkState;

/**
 * Attachment that displays a texture region.
 */
public final class SkinnedMeshAttachment extends Attachment {
  public float r = 1.0f, g = 1.0f, b = 1.0f, a = 1.0f;

  private Texture region;
  private String path;

  @Nonnull
  private int[] bones = ArrayUtil.EMPTY_INT_ARRAY;
  @Nonnull
  private float[] weights = ArrayUtil.EMPTY_FLOAT_ARRAY;
  @Nonnull
  private float[] regionUVs = ArrayUtil.EMPTY_FLOAT_ARRAY;
  @Nonnull
  private short[] triangles = ArrayUtil.EMPTY_SHORT_ARRAY;
  @Nonnull
  private float[] uvs = ArrayUtil.EMPTY_FLOAT_ARRAY;

  private int hullLength;

  // Nonessential.
  private int[] edges;
  private float width, height;

  public SkinnedMeshAttachment(@Nonnull final String name) {
    super(name);
  }

  public Texture region() {
    checkState(region != null, "Region has not been set");
    return region;
  }

  public void region(@Nonnull final Texture value) {
    region = value;
  }

  @Nonnull
  public float[] regionUVs() {
    return regionUVs;
  }

  /**
   * For each vertex, a texture coordinate pair. Ie: u, v, ...
   */
  public void regionUVs(@Nonnull final float[] value) {
    final int length = value.length;

    regionUVs = value;

    if(uvs.length != length) {
      uvs = new float[length];
    }
  }

  public void updateUVs() {
    if(region == null) {
      Arrays.fill(uvs, 0.0f);
      return;
    }

    final float[] regionUVs = this.regionUVs;
    final int uvCount = regionUVs.length;

    if(region.rotation == 0) {
      final float u = region.uv00u;
      final float v = region.uv00v;
      final float width  = region.uv11u - u;
      final float height = region.uv11v - v;

      for(int uvIndex = 0; uvIndex < uvCount; uvIndex += 2) {
        uvs[uvIndex    ] = u + regionUVs[uvIndex    ] * width;
        uvs[uvIndex + 1] = v + regionUVs[uvIndex + 1] * height;
      }
    } else if(region.rotation == 1) {
      final float u = region.uv10u;
      final float v = region.uv10v;
      final float width  = region.uv01u - u;
      final float height = region.uv01v - v;

      for(int uvIndex = 0; uvIndex < uvCount; uvIndex += 2) {
        uvs[uvIndex    ] = u +          regionUVs[uvIndex + 1] * width;
        uvs[uvIndex + 1] = v + height - regionUVs[uvIndex    ] * height;
      }
    }
  }

  public void computeWorldVertices(final float skeletonX,
                                   final float skeletonY,
                                   @Nonnull final Slot slot,
                                   @Nonnull final float[] worldVertices,
                                   @Nonnull final float[] worldUVs,
                                   @Nonnull final float[] worldColors,
                                   @Nonnull final short[] worldIndices,
                                   final int worldVertexOffset,
                                   final int worldColorOffset,
                                   final int worldIndexOffset) {
    final Skeleton skeleton = slot.skeleton();
    final Object[] skeletonBones = skeleton.bones().elements();
    final float[] weights = this.weights;
    final int[] bones = this.bones;

    final float colorRed = this.r;
    final float colorGreen = this.g;
    final float colorBlue = this.b;
    final float colorAlpha = this.a * slot.a;

    final int boneCount = bones.length;

    int worldVertexIndex = worldVertexOffset;
    int worldColorIndex = worldColorOffset;
    int boneIndex = 0;
    int weightIndex = 0;

    if(slot.attachmentVertices.isEmpty()) {
      for(; boneIndex < boneCount; worldVertexIndex += 2, worldColorIndex += 4) {
        final int nn = bones[boneIndex++] + boneIndex;

        float wx = 0.0f;
        float wy = 0.0f;

        for(; boneIndex < nn; boneIndex++, weightIndex += 3) {
          final Bone bone = (Bone)skeletonBones[bones[boneIndex]];
          final float vx = weights[weightIndex    ];
          final float vy = weights[weightIndex + 1];
          final float weight = weights[weightIndex + 2];
          wx += (vx * bone.m00() + vy * bone.m01() + bone.worldX()) * weight;
          wy += (vx * bone.m10() + vy * bone.m11() + bone.worldY()) * weight;
        }

        worldVertices[worldVertexIndex    ] = wx + skeletonX;
        worldVertices[worldVertexIndex + 1] = wy + skeletonY;

        worldColors[worldColorIndex    ] = colorRed;
        worldColors[worldColorIndex + 1] = colorGreen;
        worldColors[worldColorIndex + 2] = colorBlue;
        worldColors[worldColorIndex + 3] = colorAlpha;
      }
    } else {
      final float[] ffd = slot.attachmentVertices.elements();

      int ffdIndex = 0;

      for(; boneIndex < boneCount; worldVertexIndex += 2, worldColorIndex += 4) {
        float wx = 0.0f;
        float wy = 0.0f;

        final int nn = bones[boneIndex++] + boneIndex;

        for(; boneIndex < nn; boneIndex++, weightIndex += 3, ffdIndex += 2) {
          final Bone bone = (Bone)skeletonBones[bones[boneIndex]];
          final float vx = weights[weightIndex    ] + ffd[ffdIndex    ];
          final float vy = weights[weightIndex + 1] + ffd[ffdIndex + 1];
          final float weight = weights[weightIndex + 2];
          wx += (vx * bone.m00() + vy * bone.m01() + bone.worldX()) * weight;
          wy += (vx * bone.m10() + vy * bone.m11() + bone.worldY()) * weight;
        }

        worldVertices[worldVertexIndex    ] = wx + skeletonX;
        worldVertices[worldVertexIndex + 1] = wy + skeletonY;

        worldColors[worldColorIndex    ] = colorRed;
        worldColors[worldColorIndex + 1] = colorGreen;
        worldColors[worldColorIndex + 2] = colorBlue;
        worldColors[worldColorIndex + 3] = colorAlpha;
      }
    }

    System.arraycopy(uvs, 0, worldUVs, worldVertexOffset, uvs.length);
    System.arraycopy(triangles, 0, worldIndices, worldIndexOffset, triangles.length);
  }

  @Nonnull
  public float[] uvs() {
    return uvs;
  }

  @Nonnull
  public int[] bones() {
    return bones;
  }

  /**
   * For each vertex, the number of bones affecting the vertex followed by that many bone indices. Ie: count, boneIndex, ...
   */
  public void bones(int[] bones) {
    this.bones = bones;
  }

  public float[] weights() {
    return weights;
  }

  /**
   * For each bone affecting the vertex, the vertex position in the bone's coordinate system and the weight for the bone's
   * influence. Ie: x, y, weight, ...
   */
  public void weights(@Nonnull final float[] value) {
    weights = value;
  }

  @Nonnull
  public short[] triangles() {
    return triangles;
  }

  /**
   * Vertex number triplets which describe the mesh's triangulation.
   */
  public void triangles(@Nonnull final short[] value) {
    final int length = value.length;

    assert length % 3 == 0;

    triangles = value;
  }

  public int triangleCount() {
    return triangles.length;
  }

  public void color(final int valueARGB) {
    color(
        Color.extractRed(valueARGB),
        Color.extractGreen(valueARGB),
        Color.extractBlue(valueARGB),
        Color.extractAlpha(valueARGB));
  }

  public void color(final float r,
                    final float g,
                    final float b,
                    final float a) {
    this.r = MathUtil.clamp(r, 0.0f, 1.0f);
    this.g = MathUtil.clamp(g, 0.0f, 1.0f);
    this.b = MathUtil.clamp(b, 0.0f, 1.0f);
    this.a = MathUtil.clamp(a, 0.0f, 1.0f);
  }

  public String path() {
    return path;
  }

  public void path(final String value) {
    this.path = value;
  }

  public int hullLength() {
    return hullLength;
  }

  public void hullLength(final int hullLength) {
    this.hullLength = hullLength;
  }

  public int[] edges() {
    return edges;
  }

  public void edges(final int[] edges) {
    this.edges = edges;
  }

  public float width() {
    return width;
  }

  public void width(final float width) {
    this.width = width;
  }

  public float height() {
    return height;
  }

  public void height(final float height) {
    this.height = height;
  }

  public int vertexCount() {
    return uvs.length;
  }
}
