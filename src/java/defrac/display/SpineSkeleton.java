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

  /**
   * Creates and returns a new SpineSkeleton object
   *
   * @param skeletonData The skeleton data to display
   */
  public SpineSkeleton(@Nonnull final SkeletonData skeletonData) {
    skeleton = new Skeleton(skeletonData);
    skeleton.updateWorldTransform();

    computeAABB();
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
    computeAABB();

    if(vertexCount(skeleton.getDrawOrder()) != vertices.length) {
      invalidate(RENDERLIST_DIRTY);
    } else {
      updateVertices();
      invalidate(RENDERLIST_MATRIX_DIRTY);
    }
  }

  /**
   * Computes the AABB of the skeleton and applies it to the display object
   */
  private void computeAABB() {
    skeleton.getBounds(AABB);
    initAABB(AABB);
  }

  /** {@inheritDoc} */
  @Nullable
  @Override
  public RenderContent render(@Nonnull final GLMatrix projectionMatrix,
                              @Nonnull final GLMatrix modelViewMatrix,
                              @Nonnull final Renderer renderer,
                              @Nonnull final BlendMode parentBlendMode,
                              final float parentAlpha) {
    final float alpha = parentAlpha * this.alpha * skeleton.a;
    final float skeletonX = skeleton.x();
    final float skeletonY = skeleton.y();

    final Array<Slot> drawOrder = skeleton.getDrawOrder();
    final Array<RenderContent> contents = new Array<>();

    int vertexCountOfAllSlots = vertexCount(drawOrder);

    if(vertexCountOfAllSlots >= vertices.length) {
      vertices = new float[vertexCountOfAllSlots];
      uvs = new float[vertexCountOfAllSlots];
    }

    int offset = 0;

    for(final Slot slot : drawOrder) {
      final Attachment attachment = slot.attachment();
      final float slotAlpha = slot.a;

      if(attachment instanceof RegionAttachment) {
        final RegionAttachment regionAttachment = (RegionAttachment)attachment;
        final float attachmentAlpha = regionAttachment.a;
        final BlendMode blendMode = slot.data().blendMode;
        final int triangleCount = regionAttachment.triangleCount();
        final int vertexCount = regionAttachment.vertexCount();

        regionAttachment.updateWorldVertices(skeletonX, skeletonY, slot);

        System.arraycopy(regionAttachment.worldVertices(), 0, vertices, offset, vertexCount);
        System.arraycopy(regionAttachment.uvs(), 0, uvs, offset, vertexCount);

        contents.push(
            renderer.drawTexture(
                projectionMatrix, modelViewMatrix,
                alpha * slotAlpha * attachmentAlpha,
                blendMode,
                regionAttachment.region().textureData,
                vertices, offset,
                uvs, offset,
                triangleCount));
        offset += vertexCount;
      } else if(attachment instanceof MeshAttachment) {
        final MeshAttachment meshAttachment = (MeshAttachment)attachment;
        final float attachmentAlpha = meshAttachment.a;
        final BlendMode blendMode = slot.data().blendMode;
        final int triangleCount = meshAttachment.triangleCount();
        final int vertexCount = meshAttachment.vertexCount();

        meshAttachment.updateWorldVertices(skeletonX, skeletonY, slot);

        System.arraycopy(meshAttachment.worldVertices(), 0, vertices, offset, vertexCount);
        System.arraycopy(meshAttachment.uvs(), 0, uvs, offset, vertexCount);

        contents.push(
            renderer.drawTexture(
                projectionMatrix, modelViewMatrix,
                alpha * slotAlpha * attachmentAlpha,
                blendMode,
                meshAttachment.region().textureData,
                vertices, offset,
                uvs, offset,
                triangleCount));
        offset += vertexCount;
      } else if(attachment instanceof SkinnedMeshAttachment) {
        final SkinnedMeshAttachment skinnedMeshAttachment = (SkinnedMeshAttachment)attachment;
        final float attachmentAlpha = skinnedMeshAttachment.a;
        final BlendMode blendMode = slot.data().blendMode;
        final int triangleCount = skinnedMeshAttachment.triangleCount();
        final int vertexCount = skinnedMeshAttachment.vertexCount();

        skinnedMeshAttachment.updateWorldVertices(skeletonX, skeletonY, slot);

        System.arraycopy(skinnedMeshAttachment.worldVertices(), 0, vertices, offset, vertexCount);
        System.arraycopy(skinnedMeshAttachment.uvs(), 0, uvs, offset, vertexCount);

        contents.push(
            renderer.drawTexture(
                projectionMatrix, modelViewMatrix,
                alpha * slotAlpha * attachmentAlpha,
                blendMode,
                skinnedMeshAttachment.region().textureData,
                vertices, offset,
                uvs, offset,
                triangleCount));
        offset += vertexCount;
      }
    }

    return renderer.zone(contents);
  }

  private void updateVertices() {
    final float skeletonX = skeleton.x();
    final float skeletonY = skeleton.y();

    final Array<Slot> drawOrder = skeleton.getDrawOrder();

    int offset = 0;

    for(final Slot slot : drawOrder) {
      final Attachment attachment = slot.attachment();

      if(attachment instanceof RegionAttachment) {
        final RegionAttachment regionAttachment = (RegionAttachment)attachment;
        final int vertexCount = regionAttachment.vertexCount();

        regionAttachment.updateWorldVertices(skeletonX, skeletonY, slot);

        System.arraycopy(regionAttachment.worldVertices(), 0, vertices, offset, vertexCount);
        System.arraycopy(regionAttachment.uvs(), 0, uvs, offset, vertexCount);
        offset += vertexCount;
      } else if(attachment instanceof MeshAttachment) {
        final MeshAttachment meshAttachment = (MeshAttachment)attachment;
        final int vertexCount = meshAttachment.vertexCount();

        meshAttachment.updateWorldVertices(skeletonX, skeletonY, slot);

        System.arraycopy(meshAttachment.worldVertices(), 0, vertices, offset, vertexCount);
        System.arraycopy(meshAttachment.uvs(), 0, uvs, offset, vertexCount);

        offset += vertexCount;
      } else if(attachment instanceof SkinnedMeshAttachment) {
        final SkinnedMeshAttachment skinnedMeshAttachment = (SkinnedMeshAttachment)attachment;
        final int vertexCount = skinnedMeshAttachment.vertexCount();

        skinnedMeshAttachment.updateWorldVertices(skeletonX, skeletonY, slot);

        System.arraycopy(skinnedMeshAttachment.worldVertices(), 0, vertices, offset, vertexCount);
        System.arraycopy(skinnedMeshAttachment.uvs(), 0, uvs, offset, vertexCount);

        offset += vertexCount;
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
}
