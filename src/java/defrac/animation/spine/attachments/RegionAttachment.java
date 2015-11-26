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
import defrac.util.Color;
import defrac.util.MathUtil;

import javax.annotation.Nonnull;

import static defrac.lang.Preconditions.checkState;

/**
 * Attachment that displays a texture region.
 */
public class RegionAttachment extends Attachment {
  public static final int NUM_TRIANGLES = 6;
  public static final int NUM_VERTICES = 8;

  public static final int BOTTOM_LEFT_X = 0;
  public static final int BOTTOM_LEFT_Y = 1;
  public static final int TOP_LEFT_X = 2;
  public static final int TOP_LEFT_Y = 3;
  public static final int TOP_RIGHT_X = 4;
  public static final int TOP_RIGHT_Y = 5;
  public static final int BOTTOM_RIGHT_X = 6;
  public static final int BOTTOM_RIGHT_Y = 7;

  @Nonnull
  private final float[] offset = new float[8];

  private Texture region;
  private String path;
  private float x, y, scaleX = 1.0f, scaleY = 1.0f, rotation, width, height;
  public float r = 1.0f, g = 1.0f, b = 1.0f, a = 1.0f;

  public RegionAttachment(String name) {
    super(name);
  }

  public void updateOffset() {
    final float width = width();
    final float height = height();
    final float regionScaleX = width / region.width * scaleX;
    final float regionScaleY = height / region.height * scaleY;
    final float localX = -width * 0.5f * scaleX + region.offsetX * regionScaleX;
    final float localY = -height * 0.5f * scaleY + region.offsetY * regionScaleY;
    final float localX2 = localX + region.width * regionScaleX;
    final float localY2 = localY + region.height * regionScaleY;
    final float radians = MathUtil.degToRad(rotation);
    final float cos = MathUtil.cos(radians);
    final float sin = MathUtil.sin(radians);
    final float localXCos  = localX * cos + x;
    final float localXSin  = localX * sin;
    final float localYCos  = localY * cos + y;
    final float localYSin  = localY * sin;
    final float localX2Cos = localX2 * cos + x;
    final float localX2Sin = localX2 * sin;
    final float localY2Cos = localY2 * cos + y;
    final float localY2Sin = localY2 * sin;

    offset[BOTTOM_LEFT_X] = localXCos  - localYSin;
    offset[BOTTOM_LEFT_Y] = localYCos  + localXSin;
    offset[TOP_LEFT_X] = localXCos  - localY2Sin;
    offset[TOP_LEFT_Y] = localY2Cos + localXSin;
    offset[TOP_RIGHT_X] = localX2Cos - localY2Sin;
    offset[TOP_RIGHT_Y] = localY2Cos + localX2Sin;
    offset[BOTTOM_RIGHT_X] = localX2Cos - localYSin;
    offset[BOTTOM_RIGHT_Y] = localYCos  + localX2Sin;
  }

  @Nonnull
  public Texture region() {
    checkState(region != null, "Region has not been set");
    return region;
  }

  public void region(@Nonnull final Texture value) {
    if(region == value) {
      return;
    }

    region = value;
  }

  @Nonnull
  public float[] offset() {
    return offset;
  }

  public float x() {
    return x;
  }

  public void x(final float value) {
    x = value;
  }

  public float y() {
    return y;
  }

  public void y(final float value) {
    y = value;
  }

  public float scaleX() {
    return scaleX;
  }

  public void scaleX(final float value) {
    scaleX = value;
  }

  public float scaleY() {
    return scaleY;
  }

  public void scaleY(final float value) {
    scaleY = value;
  }

  public float rotation() {
    return rotation;
  }

  public void rotation(final float valueDeg) {
    rotation = valueDeg;
  }

  public float width() {
    return width;
  }

  public void setWidth(final float width) {
    this.width = width;
  }

  public float height() {
    return height;
  }

  public void setHeight(final float height) {
    this.height = height;
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

  public void path(final String path) {
    this.path = path;
  }

  public int vertexCount() {
    return NUM_VERTICES;
  }

  public int triangleCount() {
    return NUM_TRIANGLES;
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
    checkState(region != null, "Region has not been set");

    final Bone bone = slot.bone();

    final float x = skeletonX + bone.worldX();
    final float y = skeletonY + bone.worldY();

    final float m00 = bone.m00();
    final float m01 = bone.m01();
    final float m10 = bone.m10();
    final float m11 = bone.m11();

    final float x1 = offset[BOTTOM_LEFT_X];
    final float y1 = offset[BOTTOM_LEFT_Y];
    final float x2 = offset[TOP_LEFT_X];
    final float y2 = offset[TOP_LEFT_Y];
    final float x3 = offset[TOP_RIGHT_X];
    final float y3 = offset[TOP_RIGHT_Y];
    final float x4 = offset[BOTTOM_RIGHT_X];
    final float y4 = offset[BOTTOM_RIGHT_Y];

    final float vert00x = x2 * m00 + y2 * m01 + x;
    final float vert00y = x2 * m10 + y2 * m11 + y;
    final float vert10x = x3 * m00 + y3 * m01 + x;
    final float vert10y = x3 * m10 + y3 * m11 + y;
    final float vert11x = x4 * m00 + y4 * m01 + x;
    final float vert11y = x4 * m10 + y4 * m11 + y;
    final float vert01x = x1 * m00 + y1 * m01 + x;
    final float vert01y = x1 * m10 + y1 * m11 + y;

    // --

    worldVertices[worldVertexOffset    ] = vert00x;
    worldVertices[worldVertexOffset + 1] = vert00y;

    worldVertices[worldVertexOffset + 2] = vert10x;
    worldVertices[worldVertexOffset + 3] = vert10y;

    worldVertices[worldVertexOffset + 4] = vert11x;
    worldVertices[worldVertexOffset + 5] = vert11y;

    worldVertices[worldVertexOffset + 6] = vert01x;
    worldVertices[worldVertexOffset + 7] = vert01y;

    // --

    worldUVs[worldVertexOffset    ] = region.uv00u;
    worldUVs[worldVertexOffset + 1] = region.uv00v;

    worldUVs[worldVertexOffset + 2] = region.uv10u;
    worldUVs[worldVertexOffset + 3] = region.uv10v;

    worldUVs[worldVertexOffset + 4] = region.uv11u;
    worldUVs[worldVertexOffset + 5] = region.uv11v;

    worldUVs[worldVertexOffset + 6] = region.uv01u;
    worldUVs[worldVertexOffset + 7] = region.uv01v;

    // --

    final float r = this.r;
    final float g = this.g;
    final float b = this.b;
    final float a = this.a * slot.a;

    for(int i = 0, j = worldColorOffset; i < 4; ++i, j += 4) {
      worldColors[j    ] = r;
      worldColors[j + 1] = g;
      worldColors[j + 2] = b;
      worldColors[j + 3] = a;
    }

    // --

    worldIndices[worldIndexOffset    ] = 0;
    worldIndices[worldIndexOffset + 1] = 1;
    worldIndices[worldIndexOffset + 2] = 2;

    worldIndices[worldIndexOffset + 3] = 0;
    worldIndices[worldIndexOffset + 4] = 2;
    worldIndices[worldIndexOffset + 5] = 3;
  }
}
