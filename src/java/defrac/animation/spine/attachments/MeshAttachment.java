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
import defrac.animation.spine.Slot;
import defrac.display.Texture;
import defrac.util.ArrayUtil;
import defrac.util.Color;
import defrac.util.MathUtil;

import javax.annotation.Nonnull;

import static defrac.lang.Preconditions.checkState;

/**
 * Attachment that displays a texture region.
 */
public final class MeshAttachment extends Attachment {
  public float r = 1.0f, g = 1.0f, b = 1.0f, a = 1.0f;

  private Texture region;
  private String path;

  @Nonnull
  private float[] vertices = ArrayUtil.EMPTY_FLOAT_ARRAY;
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

  public MeshAttachment(@Nonnull final String name) {
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

  public void regionUVs(@Nonnull final float[] value) {
    regionUVs = value;
    uvs = new float[value.length];
  }

  @Nonnull
  public float[] uvs() {
    return uvs;
  }

  public void updateUVs() {
    final float u, v;
    final float width, height;

    if(region == null) {
      u = v = 0.0f;
      width = height = 1.0f;
    } else {
      u = region.uv00x;
      v = region.uv00y;
      width = region.uv11x - u;
      height = region.uv11y - v;
    }

    final float[] regionUVs = this.regionUVs;
    final int uvCount = regionUVs.length;

    for(int uvIndex = 0; uvIndex < uvCount; uvIndex += 2) {
      final float tu = regionUVs[uvIndex    ];
      final float tv = regionUVs[uvIndex + 1];
      uvs[uvIndex    ] = u + tu * width;
      uvs[uvIndex + 1] = v + tv * height;
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
    final Bone bone = slot.bone();

    final float x = skeletonX + bone.worldX();
    final float y = skeletonY + bone.worldY();

    final float m00 = bone.m00();
    final float m01 = bone.m01();
    final float m10 = bone.m10();
    final float m11 = bone.m11();

    float[] vertices = this.vertices;
    final int verticesCount = vertices.length;

    if(slot.attachmentVertices.length() == verticesCount) {
      vertices = slot.attachmentVertices.elements();
    }

    int worldVertexIndex = worldVertexOffset;
    int worldColorIndex = worldColorOffset;

    final float r = this.r;
    final float g = this.g;
    final float b = this.b;
    final float a = this.a * slot.a;

    for(int vertexIndex = 0; vertexIndex < verticesCount; vertexIndex += 2, worldVertexIndex += 2, worldColorIndex += 4) {
      final float vx = vertices[vertexIndex    ];
      final float vy = vertices[vertexIndex + 1];

      worldVertices[worldVertexIndex    ] = vx * m00 + vy * m01 + x;
      worldVertices[worldVertexIndex + 1] = vx * m10 + vy * m11 + y;

      worldUVs[worldVertexIndex    ] = uvs[vertexIndex    ];
      worldUVs[worldVertexIndex + 1] = uvs[vertexIndex + 1];

      worldColors[worldColorIndex    ] = r;
      worldColors[worldColorIndex + 1] = g;
      worldColors[worldColorIndex + 2] = b;
      worldColors[worldColorIndex + 3] = a;
    }

    System.arraycopy(triangles, 0, worldIndices, worldIndexOffset, triangles.length);
  }

  @Nonnull
  public float[] vertices() {
    return vertices;
  }

  public void vertices(@Nonnull final float[] value) {
    vertices = value;
  }

  @Nonnull
  public short[] triangles() {
    return triangles;
  }

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
    return vertices.length;
  }
}
