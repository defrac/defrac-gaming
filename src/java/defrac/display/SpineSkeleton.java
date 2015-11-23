/*
 *
 *  * Copyright 2015 defrac inc.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package defrac.display;

import defrac.animation.spine.AnimationState;
import defrac.animation.spine.Skeleton;
import defrac.animation.spine.SkeletonData;
import defrac.animation.spine.Slot;
import defrac.animation.spine.attachments.Attachment;
import defrac.animation.spine.attachments.MeshAttachment;
import defrac.animation.spine.attachments.RegionAttachment;
import defrac.animation.spine.attachments.SkinnedMeshAttachment;
import defrac.display.render.RenderContent;
import defrac.display.render.Renderer;
import defrac.geom.Rectangle;
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
  @Nonnull
  private static final Rectangle AABB = new Rectangle();

  private static final float MS_TO_SEC = 0.001f;

  @Nonnull
  private final Skeleton skeleton;

  @Nonnull
  private float[] vertices = ArrayUtil.EMPTY_FLOAT_ARRAY;

  @Nonnull
  private float[] uvs = ArrayUtil.EMPTY_FLOAT_ARRAY;

  @Nonnull
  private float[] colors = ArrayUtil.EMPTY_FLOAT_ARRAY;

  @Nonnull
  private short[] indices = ArrayUtil.EMPTY_SHORT_ARRAY;

  /**
   * Creates and returns a new SpineSkeleton object
   *
   * @param skeletonData The skeleton data to display
   */
  public SpineSkeleton(@Nonnull final SkeletonData skeletonData) {
    skeleton = new Skeleton(skeletonData);
    skeleton.updateWorldTransform();
    onPoseUpdate();
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
    final float dt = (float)dtMS * MS_TO_SEC;

    state.update(dt);
    skeleton.update(dt);
    state.apply(skeleton);
    onPoseUpdate();

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
    onPoseUpdate();

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
    final float dt = (float)dtMS * MS_TO_SEC;

    skeleton.update(dt);
    onPoseUpdate();

    return this;
  }

  /**
   * Callback when the pose of the skeleton is changed
   */
  private void onPoseUpdate() {
    skeleton.updateWorldTransform();

    final Array<Slot> drawOrder = skeleton.getDrawOrder();
    final int vertexCountOfAllSlots = vertexCount(drawOrder);
    final int triangleCountOfAllSlots = triangleCount(drawOrder);

    updateVertices(vertexCountOfAllSlots, triangleCountOfAllSlots);
    computeAABB();

    if(    vertexCountOfAllSlots   >= vertices.length
        || triangleCountOfAllSlots >= indices.length) {
      invalidate(RENDERLIST_DIRTY);
    } else {
      invalidate(RENDERLIST_MATRIX_DIRTY);
    }
  }

  /**
   * Computes the AABB of the skeleton and applies it to the display object
   */
  private void computeAABB() {
    float
        minX = Integer.MAX_VALUE,
        minY = Integer.MAX_VALUE,
        maxX = Integer.MIN_VALUE,
        maxY = Integer.MIN_VALUE;

    final Array<Slot> drawOrder = skeleton.getDrawOrder();
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
  }

  /** {@inheritDoc} */
  @Nullable
  @Override
  public RenderContent render(@Nonnull final GLMatrix projectionMatrix,
                              @Nonnull final GLMatrix modelViewMatrix,
                              @Nonnull final Renderer renderer,
                              @Nonnull final BlendMode parentBlendMode,
                              final float parentAlpha) {
    final BlendMode inheritedBlendMode = blendMode().inherit(parentBlendMode);
    final float alpha = parentAlpha * this.alpha * skeleton.a;

    final Array<Slot> drawOrder = skeleton.getDrawOrder();
    final Array<RenderContent> contents = new Array<>();

    int vertexOffset = 0;
    int colorOffset = 0;
    int indexOffset = 0;

    for(final Slot slot : drawOrder) {
      final Attachment attachment = slot.attachment();

      if(attachment instanceof RegionAttachment) {
        final RegionAttachment regionAttachment = (RegionAttachment)attachment;
        final BlendMode blendMode = slot.data().blendMode.inherit(inheritedBlendMode);
        final int triangleCount = regionAttachment.triangleCount();
        final int vertexCount = regionAttachment.vertexCount();

        contents.push(
            renderer.drawTexture(
                projectionMatrix, modelViewMatrix,
                alpha,
                blendMode,
                regionAttachment.region().textureData,
                vertices, vertexOffset,
                uvs, vertexOffset,
                colors, colorOffset,
                indices,
                indexOffset,
                vertexCount / 2,
                triangleCount / 3));

        vertexOffset += vertexCount;
        colorOffset += vertexCount * 2;
        indexOffset += triangleCount;
      } else if(attachment instanceof MeshAttachment) {
        final MeshAttachment meshAttachment = (MeshAttachment)attachment;
        final BlendMode blendMode = slot.data().blendMode.inherit(inheritedBlendMode);
        final int triangleCount = meshAttachment.triangleCount();
        final int vertexCount = meshAttachment.vertexCount();

        contents.push(
            renderer.drawTexture(
                projectionMatrix, modelViewMatrix,
                alpha,
                blendMode,
                meshAttachment.region().textureData,
                vertices, vertexOffset,
                uvs, vertexOffset,
                colors, colorOffset,
                indices,
                indexOffset,
                vertexCount / 2,
                triangleCount / 3));

        vertexOffset += vertexCount;
        colorOffset += vertexCount * 2;
        indexOffset += triangleCount;
      } else if(attachment instanceof SkinnedMeshAttachment) {
        final SkinnedMeshAttachment skinnedMeshAttachment = (SkinnedMeshAttachment)attachment;
        final BlendMode blendMode = slot.data().blendMode;
        final int triangleCount = skinnedMeshAttachment.triangleCount();
        final int vertexCount = skinnedMeshAttachment.vertexCount();

        contents.push(
            renderer.drawTexture(
                projectionMatrix, modelViewMatrix,
                alpha,
                blendMode,
                skinnedMeshAttachment.region().textureData,
                vertices, vertexOffset,
                uvs, vertexOffset,
                colors, colorOffset,
                indices,
                indexOffset,
                vertexCount / 2,
                triangleCount / 3));

        vertexOffset += vertexCount;
        colorOffset += vertexCount * 2;
        indexOffset += triangleCount;
      }
    }

    return renderer.zone(contents);
  }

  private void updateVertices(final int vertexCountOfAllSlots,
                              final int triangleCountOfAllSlots) {
    final Array<Slot> drawOrder = skeleton.getDrawOrder();

    if(vertexCountOfAllSlots >= vertices.length) {
      vertices = new float[vertexCountOfAllSlots];
      uvs = new float[vertexCountOfAllSlots];
      colors = new float[vertexCountOfAllSlots * 2];
    }

    if(triangleCountOfAllSlots >= indices.length) {
      indices = new short[triangleCountOfAllSlots];
    }

    final float skeletonX = skeleton.x();
    final float skeletonY = skeleton.y();

    int vertexOffset = 0;
    int colorOffset = 0;
    int indexOffset = 0;

    for(final Slot slot : drawOrder) {
      final Attachment attachment = slot.attachment();

      if(attachment instanceof RegionAttachment) {
        final RegionAttachment regionAttachment = (RegionAttachment)attachment;
        final int vertexCount = regionAttachment.vertexCount();
        final int triangleCount = regionAttachment.triangleCount();

        regionAttachment.computeWorldVertices(
            skeletonX, skeletonY, slot,
            vertices, uvs, colors, indices,
            vertexOffset, colorOffset, indexOffset);

        vertexOffset += vertexCount;
        colorOffset  += vertexCount * 2;
        indexOffset  += triangleCount;
      } else if(attachment instanceof MeshAttachment) {
        final MeshAttachment meshAttachment = (MeshAttachment)attachment;
        final int vertexCount = meshAttachment.vertexCount();
        final int triangleCount = meshAttachment.triangleCount();

        meshAttachment.computeWorldVertices(
            skeletonX, skeletonY, slot,
            vertices, uvs, colors, indices,
            vertexOffset, colorOffset, indexOffset);

        vertexOffset += vertexCount;
        colorOffset  += vertexCount * 2;
        indexOffset  += triangleCount;
      } else if(attachment instanceof SkinnedMeshAttachment) {
        final SkinnedMeshAttachment skinnedMeshAttachment = (SkinnedMeshAttachment)attachment;
        final int vertexCount = skinnedMeshAttachment.vertexCount();
        final int triangleCount = skinnedMeshAttachment.triangleCount();

        skinnedMeshAttachment.computeWorldVertices(
            skeletonX, skeletonY, slot,
            vertices, uvs, colors, indices,
            vertexOffset, colorOffset, indexOffset);

        vertexOffset += vertexCount;
        colorOffset  += vertexCount * 2;
        indexOffset  += triangleCount;
      }
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
      }
    }

    return sum;
  }
}
