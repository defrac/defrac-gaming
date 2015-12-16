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

package defrac.display;

import defrac.animation.spine.*;
import defrac.animation.spine.attachments.*;
import defrac.display.event.UIEventTarget;
import defrac.display.render.RenderContent;
import defrac.display.render.Renderer;
import defrac.geom.Matrix;
import defrac.geom.Point;
import defrac.gl.GLMatrix;
import defrac.util.Array;
import defrac.util.ArrayUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static defrac.display.DisplayObjectFlags.RENDERLIST_DIRTY;
import static defrac.display.DisplayObjectFlags.RENDERLIST_MATRIX_DIRTY;

/**
 *
 */
public final class SpineSkeleton extends DisplayObject {
  private static final double MS_TO_SEC = 0.001;

  @Nonnull
  private final Skeleton skeleton;

  @Nullable
  private SkeletonBounds skeletonBounds;

  @Nonnull
  private float[] vertices = ArrayUtil.EMPTY_FLOAT_ARRAY;

  @Nonnull
  private float[] uvs = ArrayUtil.EMPTY_FLOAT_ARRAY;

  @Nonnull
  private float[] colors = ArrayUtil.EMPTY_FLOAT_ARRAY;

  @Nonnull
  private short[] indices = ArrayUtil.EMPTY_SHORT_ARRAY;

  @Nonnull
  private final Array<RenderContent> contents = new Array<>();

  @Nullable
  private RenderContent content;

  private int vertexOffset = 0;
  private int colorOffset = 0;
  private int indexOffset = 0;

  private int lastVertexCount = -1;
  private int lastTriangleCount = -1;

  private boolean useSkeletonBounds;

  /**
   * Creates and returns a new SpineSkeleton object
   *
   * @param skeletonData The skeleton data to display
   */
  public SpineSkeleton(@Nonnull final SkeletonData skeletonData) {
    skeleton = new Skeleton(skeletonData);
    skeleton.updateWorldTransform();
    renderSkeleton();
  }

  /**
   * Creates and returns a new SpineSkeleton object
   *
   * @param skeleton The skeleton to display
   */
  public SpineSkeleton(@Nonnull final Skeleton skeleton) {
    this.skeleton = skeleton;
    renderSkeleton();
  }

  /** The Skeleton represented by this display object */
  @Nonnull
  public Skeleton skeleton() {
    return skeleton;
  }

  /** The SkeletonBounds of this display object; null if {@link #useSkeletonBounds()} is {@literal false} */
  @Nullable
  public SkeletonBounds skeletonBounds() {
    return skeletonBounds;
  }

  /** Whether or not to use the SkeletonBounds for precise hit-test */
  public boolean useSkeletonBounds() {
    return useSkeletonBounds;
  }

  /**
   * Whether or not to use the BoundsAttachment for precise hit-test
   *
   * <p>When the
   */
  public SpineSkeleton useSkeletonBounds(final boolean value) {
    if(value == useSkeletonBounds) {
      return this;
    }

    useSkeletonBounds = value;

    if(value) {
      skeletonBounds = new SkeletonBounds();
      updateBounds();
    } else {
      skeletonBounds = null;
    }

    return this;
  }

  /**
   * Updates the time of the animation state and skeleton, then poses the skeleton using the animation
   *
   * @param state The animation state to apply
   * @param dtMS The time elapsed since the last update in milliseconds
   * @return The current object
   */
  @Nonnull
  public SpineSkeleton update(@Nonnull final AnimationState state, final int dtMS) {
    final double dt = (double)dtMS * MS_TO_SEC;
    return update(state, dt);
  }


/**
 * Updates the time of the animation state and skeleton, then poses the skeleton using the animation
 *
 * @param state The animation state to apply
 * @param dtSec The time elapsed since the last update in seconds
 * @return The current object
 */
  @Nonnull
  public SpineSkeleton update(@Nonnull final AnimationState state, final double dtSec) {
    final float dt = (float)dtSec;
    state.update(dt);
    skeleton.update(dt);
    state.apply(skeleton);
    renderSkeleton();

    return this;
  }

  /**
   * Poses the skeleton using the given animation state
   *
   * @param state The animation state to apply
   * @return The current object
   */
  @Nonnull
  public SpineSkeleton updatePose(@Nonnull final AnimationState state) {
    state.apply(skeleton);
    renderSkeleton();

    return this;
  }

  /**
   * Updates the skeleton
   *
   * @param dtMS The time elapsed since the last update in milliseconds
   * @return The current object
   */
  @Nonnull
  public SpineSkeleton updateSkeleton(final int dtMS) {
    final double dt = (double)dtMS * MS_TO_SEC;
    return updateSkeleton(dt);
  }

  /**
   * Updates the skeleton
   *
   * @param dtSec The time elapsed since the last update in seconds
   * @return The current object
   */
  @Nonnull
  public SpineSkeleton updateSkeleton(final double dtSec) {
    skeleton.update((float)dtSec);
    renderSkeleton();

    return this;
  }

  /**
   * Callback when the pose of the skeleton is changed
   *
   * <p>Calling {@code renderSkeleton} is only necessary when
   * the {@link #skeleton() skeleton} is manually updated.
   *
   * The SpineSkeleton will invoke this method after any of
   * its {@code update*} methods (like {@link #update(AnimationState, int)})
   * are being invoked.
   */
  public void renderSkeleton() {
    skeleton.updateWorldTransform();

    final Array<Slot> drawOrder = skeleton.drawOrder();
    final int vertexCountOfAllSlots = vertexCount(drawOrder);
    final int triangleCountOfAllSlots = triangleCount(drawOrder);

    // We compute all the vertices relative to the coordinates
    // of the display object and its AABB
    updateVertices(vertexCountOfAllSlots, triangleCountOfAllSlots);
    updateBounds();

    if(content != null) {
      // We have cached render content and if we're able
      // to re-use it, we can invalidate only the projected
      // vertex coordinates

      if(    vertexCountOfAllSlots   != lastVertexCount
          || triangleCountOfAllSlots != lastTriangleCount) {
        // We're not able to re-use the existing content
        invalidate(RENDERLIST_DIRTY);
      } else {
        // This is an optimistic assumption which can be wrong.
        //
        // If the number of vertices and triangles is exactly
        // the same we'll only invalidate the vertices and not
        // the whole render-list.
        //
        // TODO(joa): use sequence of texture data for correctness
        invalidate(RENDERLIST_MATRIX_DIRTY);
      }
    } else {
      // We don't have any content we can re-use so we have to
      // invalidate the render list
      invalidate(RENDERLIST_DIRTY);
    }

    lastVertexCount = vertexCountOfAllSlots;
    lastTriangleCount = triangleCountOfAllSlots;
  }

  /**
   * Computes the AABB of the skeleton and applies it to the display object
   */
  private void updateBounds() {
    float
        minX = Integer.MAX_VALUE,
        minY = Integer.MAX_VALUE,
        maxX = Integer.MIN_VALUE,
        maxY = Integer.MIN_VALUE;

    final Array<Slot> drawOrder = skeleton.drawOrder();
    final int vertexCount = vertexCount(drawOrder);

    for(int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex += 2) {
      final float x = vertices[vertexIndex    ];
      final float y = vertices[vertexIndex + 1];

      if(x < minX) { minX = x; }
      if(x > maxX) { maxX = x; }
      if(y < minY) { minY = y; }
      if(y > maxY) { maxY = y; }
    }

    initAABB(minX, minY, maxX - minX, maxY - minY);

    if(useSkeletonBounds) {
      assert skeletonBounds != null;
      skeletonBounds.update(skeleton, true);
    }
  }

  /** {@inheritDoc} */
  @Nullable
  @Override
  public RenderContent render(@Nonnull final GLMatrix projectionMatrix,
                              @Nonnull final GLMatrix modelViewMatrix,
                              @Nonnull final Renderer renderer,
                              @Nonnull final BlendMode parentBlendMode,
                              final float parentAlpha) {
    final BlendMode displayObjectBlendMode = blendMode().inherit(parentBlendMode);
    final float alpha = parentAlpha * this.alpha;

    vertexOffset = 0;
    colorOffset = 0;
    indexOffset = 0;

    render(
        projectionMatrix, modelViewMatrix,
        renderer,
        displayObjectBlendMode,
        alpha,
        skeleton);

    content = renderer.zone(contents);
    contents.clear();

    return content;
  }

  private void render(@Nonnull final GLMatrix projectionMatrix,
                      @Nonnull final GLMatrix modelViewMatrix,
                      @Nonnull final Renderer renderer,
                      @Nonnull final BlendMode displayObjectBlendMode,
                      final float displayObjectAlpha,
                      @Nonnull final Skeleton skeleton) {
    final float alpha = displayObjectAlpha * skeleton.a;

    for(final Slot slot : skeleton.drawOrder()) {
      final Attachment attachment = slot.attachment();

      final BlendMode blendMode;
      final TextureData textureData;
      final int triangleCount;
      final int vertexCount;

      if(attachment instanceof RegionAttachment) {
        final RegionAttachment regionAttachment = (RegionAttachment)attachment;

        blendMode = slot.data().blendMode;
        textureData = regionAttachment.region().textureData;
        triangleCount = regionAttachment.triangleCount();
        vertexCount = regionAttachment.vertexCount();
      } else if(attachment instanceof MeshAttachment) {
        final MeshAttachment meshAttachment = (MeshAttachment)attachment;

        blendMode = slot.data().blendMode;
        textureData = meshAttachment.region().textureData;
        triangleCount = meshAttachment.triangleCount();
        vertexCount = meshAttachment.vertexCount();
      } else if(attachment instanceof SkinnedMeshAttachment) {
        final SkinnedMeshAttachment skinnedMeshAttachment = (SkinnedMeshAttachment)attachment;

        blendMode = slot.data().blendMode;
        textureData = skinnedMeshAttachment.region().textureData;
        triangleCount = skinnedMeshAttachment.triangleCount();
        vertexCount = skinnedMeshAttachment.vertexCount();
      } else if(attachment instanceof SkeletonAttachment) {
        final SkeletonAttachment skeletonAttachment = (SkeletonAttachment)attachment;
        final Skeleton attachedSkeleton = skeletonAttachment.skeleton();

        if(attachedSkeleton == null) {
          continue;
        }

        render(
            projectionMatrix,
            modelViewMatrix,
            renderer,
            displayObjectBlendMode,
            alpha,
            attachedSkeleton);

        continue;
      } else {
        continue;
      }

      contents.push(
          renderer.drawTexture(
              projectionMatrix, modelViewMatrix,
              alpha,
              blendMode.inherit(displayObjectBlendMode),
              textureData,
              vertices, vertexOffset,
              uvs, vertexOffset,
              colors, colorOffset,
              indices, indexOffset,
              vertexCount / 2,
              triangleCount / 3));

      vertexOffset += vertexCount;
      colorOffset  += vertexCount * 2;
      indexOffset  += triangleCount;
    }
  }

  private void updateVertices(final int vertexCountOfAllSlots,
                              final int triangleCountOfAllSlots) {
    if(vertexCountOfAllSlots >= vertices.length) {
      // We have to invalidate the content since our existing content
      // references the old arrays
      content = null;

      vertices = new float[vertexCountOfAllSlots];
      uvs = new float[vertexCountOfAllSlots];
      colors = new float[vertexCountOfAllSlots * 2];
    }

    if(triangleCountOfAllSlots >= indices.length) {
      // We have to invalidate the content since our existing content
      // references the old arrays
      content = null;

      indices = new short[triangleCountOfAllSlots];
    }

    vertexOffset = 0;
    colorOffset = 0;
    indexOffset = 0;

    updateVertices(skeleton);
  }

  private void updateVertices(@Nonnull final Skeleton skeleton) {
    final float skeletonX = skeleton.x();
    final float skeletonY = skeleton.y();

    for(final Slot slot : skeleton.drawOrder()) {
      final Attachment attachment = slot.attachment();

      final int vertexCount;
      final int triangleCount;

      if(attachment instanceof RegionAttachment) {
        final RegionAttachment regionAttachment = (RegionAttachment)attachment;

        vertexCount = regionAttachment.vertexCount();
        triangleCount = regionAttachment.triangleCount();

        regionAttachment.computeWorldVertices(
            skeletonX, skeletonY, slot,
            vertices, uvs, colors, indices,
            vertexOffset, colorOffset, indexOffset);
      } else if(attachment instanceof MeshAttachment) {
        final MeshAttachment meshAttachment = (MeshAttachment)attachment;

        vertexCount = meshAttachment.vertexCount();
        triangleCount = meshAttachment.triangleCount();

        meshAttachment.computeWorldVertices(
            skeletonX, skeletonY, slot,
            vertices, uvs, colors, indices,
            vertexOffset, colorOffset, indexOffset);
      } else if(attachment instanceof SkinnedMeshAttachment) {
        final SkinnedMeshAttachment skinnedMeshAttachment = (SkinnedMeshAttachment) attachment;

        vertexCount = skinnedMeshAttachment.vertexCount();
        triangleCount = skinnedMeshAttachment.triangleCount();

        skinnedMeshAttachment.computeWorldVertices(
            skeletonX, skeletonY, slot,
            vertices, uvs, colors, indices,
            vertexOffset, colorOffset, indexOffset);
      } else if(attachment instanceof SkeletonAttachment) {
        final SkeletonAttachment skeletonAttachment = (SkeletonAttachment)attachment;
        final Skeleton attachmentSkeleton = skeletonAttachment.skeleton();

        if(attachmentSkeleton == null) {
          continue;
        }

        final Bone bone = slot.bone();
        final Bone rootBone = attachmentSkeleton.rootBone();
        final float oldScaleX = rootBone.scaleX();
        final float oldScaleY = rootBone.scaleY();
        final float oldRotation = rootBone.rotation();
        final float oldX = attachmentSkeleton.x();
        final float oldY = attachmentSkeleton.x();

        attachmentSkeleton.moveTo(skeleton.x() + bone.worldX(), skeleton.y() + bone.worldY());

        rootBone.scaleTo(
            1.0f + bone.worldScaleX() - oldScaleX,
            1.0f + bone.worldScaleY() - oldScaleY);
        rootBone.rotation(oldRotation + bone.worldRotation());

        attachmentSkeleton.updateWorldTransform();

        updateVertices(attachmentSkeleton);

        attachmentSkeleton.moveTo(oldX, oldY);

        rootBone.scaleTo(oldScaleX, oldScaleY);
        rootBone.rotation(oldRotation);
        continue;
      } else {
        continue;
      }

      vertexOffset += vertexCount;
      colorOffset  += vertexCount * 2;
      indexOffset  += triangleCount;
    }
  }

  private static int vertexCount(@Nonnull final Array<Slot> drawOrder) {
    int sum = 0;

    for(final Slot slot : drawOrder) {
      final Attachment attachment = slot.attachment();

      if(attachment instanceof RegionAttachment) {
        sum += ((RegionAttachment)attachment).vertexCount();
      } else if(attachment instanceof MeshAttachment) {
        sum += ((MeshAttachment)attachment).vertexCount();
      } else if(attachment instanceof SkinnedMeshAttachment) {
        sum += ((SkinnedMeshAttachment)attachment).vertexCount();
      } else if(attachment instanceof SkeletonAttachment) {
        final SkeletonAttachment skeletonAttachment = ((SkeletonAttachment)attachment);
        final Skeleton skeleton = skeletonAttachment.skeleton();
        if(skeleton != null) {
          sum += vertexCount(skeleton.drawOrder());
        }
      }
    }

    return sum;
  }


  private static int triangleCount(@Nonnull final Array<Slot> drawOrder) {
    int sum = 0;

    for(final Slot slot : drawOrder) {
      final Attachment attachment = slot.attachment();

      if(attachment instanceof RegionAttachment) {
        sum += ((RegionAttachment)attachment).triangleCount();
      } else if(attachment instanceof MeshAttachment) {
        sum += ((MeshAttachment)attachment).triangleCount();
      } else if(attachment instanceof SkinnedMeshAttachment) {
        sum += ((SkinnedMeshAttachment)attachment).triangleCount();
      } else if(attachment instanceof SkeletonAttachment) {
        final SkeletonAttachment skeletonAttachment = ((SkeletonAttachment)attachment);
        final Skeleton skeleton = skeletonAttachment.skeleton();
        if(skeleton != null) {
          sum += triangleCount(skeleton.drawOrder());
        }
      }
    }

    return sum;
  }

  @Nullable
  @Override
  public UIEventTarget captureEventTarget(@Nonnull Point point) {
    if(!useSkeletonBounds) {
      return super.captureEventTarget(point);
    }

    assert skeletonBounds != null;

    if(!visible) {
      return null;
    }

    final Matrix matrix = this.temporaryConcatMatrix(this);
    matrix.invert();

    float localX = matrix.a * point.x + matrix.c * point.y + matrix.tx;
    float localY = matrix.b * point.x + matrix.d * point.y + matrix.ty;

    return skeletonBounds.aabbContainsPoint(localX, localY)
        && skeletonBounds.containsPoint(localX, localY) != null
        ? this
        : null;
  }
}
