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

package defrac.animation.spine;

import defrac.animation.spine.attachments.Attachment;
import defrac.animation.spine.attachments.BoundingBoxAttachment;
import defrac.pool.ObjectPool;
import defrac.pool.ObjectPools;
import defrac.util.Array;
import defrac.util.FloatArray;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class SkeletonBounds {
  private float minX, minY, maxX, maxY;

  @Nonnull
  private final Array<BoundingBoxAttachment> boundingBoxes = new Array<>();

  @Nonnull
  private final Array<FloatArray> polygons = new Array<>();

  @Nonnull
  private final ObjectPool<FloatArray> polygonPool = ObjectPools.newPool(FloatArray::new);

  public void update(Skeleton skeleton, boolean updateAabb) {
    final Array<BoundingBoxAttachment> boundingBoxes = this.boundingBoxes;
    final Array<FloatArray> polygons = this.polygons;
    final Array<Slot> slots = skeleton.slots;

    boundingBoxes.clear();
    polygonPool.retAll(polygons);
    polygons.clear();

    for(final Slot slot : slots) {
      final Attachment attachment = slot.attachment;

      if(attachment instanceof BoundingBoxAttachment) {
        final BoundingBoxAttachment boundingBox = (BoundingBoxAttachment)attachment;
        boundingBoxes.push(boundingBox);

        final FloatArray polygon = polygonPool.get();

        polygons.push(polygon);

        final int vertexCount = boundingBox.vertices().length;

        polygon.size(vertexCount);

        boundingBox.computeWorldVertices(slot.bone, polygon.elements());
      }
    }

    if(updateAabb) {
      aabbCompute();
    }
  }

  private void aabbCompute() {
    float minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;

    for(final FloatArray polygon : polygons) {
      float[] vertices = polygon.elements();

      for(int vertexIndex = 0, vertexCount = polygon.size(); vertexIndex < vertexCount; vertexIndex += 2) {
        final float x = vertices[vertexIndex    ];
        final float y = vertices[vertexIndex + 1];
        minX = Math.min(minX, x);
        minY = Math.min(minY, y);
        maxX = Math.max(maxX, x);
        maxY = Math.max(maxY, y);
      }
    }

    this.minX = minX;
    this.minY = minY;
    this.maxX = maxX;
    this.maxY = maxY;
  }

  /** Returns true if the axis aligned bounding box contains the point. */
  public boolean aabbContainsPoint(final float x, final float y) {
    return x >= minX && x <= maxX && y >= minY && y <= maxY;
  }

  /** Returns true if the axis aligned bounding box intersects the line segment. */
  public boolean aabbIntersectsSegment(final float x1, final float y1, final float x2, final float y2) {
    float minX = this.minX;
    float minY = this.minY;
    float maxX = this.maxX;
    float maxY = this.maxY;

    if((x1 <= minX && x2 <= minX) || (y1 <= minY && y2 <= minY) || (x1 >= maxX && x2 >= maxX) || (y1 >= maxY && y2 >= maxY)) {
      return false;
    }

    float m = (y2 - y1) / (x2 - x1);
    float y = m * (minX - x1) + y1;

    if (y > minY && y < maxY) return true;

    y = m * (maxX - x1) + y1;

    if (y > minY && y < maxY) return true;

    float x = (minY - y1) / m + x1;

    if (x > minX && x < maxX) return true;

    x = (maxY - y1) / m + x1;

    if (x > minX && x < maxX) return true;
    return false;
  }

  /** Returns true if the axis aligned bounding box intersects the axis aligned bounding box of the specified bounds. */
  public boolean aabbIntersectsSkeleton(@Nonnull final SkeletonBounds bounds) {
    return minX < bounds.maxX && maxX > bounds.minX && minY < bounds.maxY && maxY > bounds.minY;
  }

  /** Returns the first bounding box attachment that contains the point, or null. When doing many checks, it is usually more
   * efficient to only call this method if {@link #aabbContainsPoint(float, float)} returns true. */
  @Nullable
  public BoundingBoxAttachment containsPoint(final float x, final float y) {
    Array<FloatArray> polygons = this.polygons;

    for(int polygonIndex = 0, polygonCount = polygons.size(); polygonIndex < polygonCount; polygonIndex++) {
      if(containsPoint(polygons.get(polygonIndex), x, y)) {
        return boundingBoxes.get(polygonIndex);
      }
    }

    return null;
  }

  /** Returns true if the polygon contains the point. */
  public boolean containsPoint(@Nonnull final FloatArray polygon,
                               final float x,
                               final float y) {
    final float[] vertices = polygon.elements();
    final int vertexCount = polygon.size();

    int prevIndex = vertexCount - 2;
    boolean inside = false;

    for(int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex += 2) {
      final float vertexY = vertices[vertexIndex + 1];
      final float prevY   = vertices[prevIndex   + 1];

      if((vertexY < y && prevY >= y) || (prevY < y && vertexY >= y)) {
        final float vertexX = vertices[vertexIndex];

        if(vertexX + (y - vertexY) / (prevY - vertexY) * (vertices[prevIndex] - vertexX) < x) {
          inside = !inside;
        }
      }

      prevIndex = vertexIndex;
    }
    return inside;
  }

  /** Returns the first bounding box attachment that contains the line segment, or null. When doing many checks, it is usually
   * more efficient to only call this method if {@link #aabbIntersectsSegment(float, float, float, float)} returns true. */
  @Nullable
  public BoundingBoxAttachment intersectsSegment(final float x1, final float y1, final float x2, final float y2) {
    for(int polygonIndex = 0, polygonCount = polygons.size(); polygonIndex < polygonCount; polygonIndex++) {
      if(intersectsSegment(polygons.get(polygonIndex), x1, y1, x2, y2)) {
        return boundingBoxes.get(polygonIndex);
      }
    }

    return null;
  }

  /** Returns true if the polygon contains the line segment. */
  public boolean intersectsSegment(@Nonnull final FloatArray polygon,
                                   final float x1, final float y1,
                                   final float x2, final float y2) {
    final float[] vertices = polygon.elements();
    final int vertexCount = polygon.size();

    float width12 = x1 - x2, height12 = y1 - y2;
    float det1 = x1 * y2 - y1 * x2;
    float x3 = vertices[vertexCount - 2], y3 = vertices[vertexCount - 1];
    for(int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex += 2) {
      float x4 = vertices[vertexIndex], y4 = vertices[vertexIndex + 1];
      float det2 = x3 * y4 - y3 * x4;
      float width34 = x3 - x4, height34 = y3 - y4;
      float det3 = width12 * height34 - height12 * width34;
      float x = (det1 * width34 - width12 * det2) / det3;
      if (((x >= x3 && x <= x4) || (x >= x4 && x <= x3)) && ((x >= x1 && x <= x2) || (x >= x2 && x <= x1))) {
        float y = (det1 * height34 - height12 * det2) / det3;
        if (((y >= y3 && y <= y4) || (y >= y4 && y <= y3)) && ((y >= y1 && y <= y2) || (y >= y2 && y <= y1)))
          return true;
      }
      x3 = x4;
      y3 = y4;
    }
    return false;
  }

  public float minX() {
    return minX;
  }

  public float minY() {
    return minY;
  }

  public float maxX() {
    return maxX;
  }

  public float maxY() {
    return maxY;
  }

  public float width() {
    return maxX - minX;
  }

  public float height() {
    return maxY - minY;
  }

  @Nonnull
  public Array<BoundingBoxAttachment> boundingBoxes() {
    return boundingBoxes;
  }

  @Nonnull
  public Array<FloatArray> polygons() {
    return polygons;
  }

  /** Returns the polygon for the specified bounding box, or null. */
  @Nullable
  public FloatArray getPolygon(@Nullable final BoundingBoxAttachment boundingBox) {
    int index = boundingBoxes.identityIndexOf(boundingBox);
    return index == -1
        ? null
        : polygons.get(index);
  }
}
