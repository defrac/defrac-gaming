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

import defrac.animation.spine.Animation.*;
import defrac.animation.spine.attachments.*;
import defrac.display.TextureAtlas;
import defrac.json.JSON;
import defrac.json.JSONArray;
import defrac.json.JSONObject;
import defrac.util.Array;
import defrac.util.Color;
import defrac.util.FloatArray;
import defrac.util.IntArray;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;

import static defrac.lang.Preconditions.checkNotNull;

public final class SkeletonJson {
  @Nonnull
  private final AttachmentLoader attachmentLoader;

  private float scale = 1.0f;

  public SkeletonJson(@Nonnull final  TextureAtlas atlas) {
    attachmentLoader = new AtlasAttachmentLoader(atlas);
  }

  public SkeletonJson(@Nonnull final AttachmentLoader attachmentLoader) {
    this.attachmentLoader = attachmentLoader;
  }

  public float scale() {
    return scale;
  }

  /** Scales the bones, images, and animations as they are loaded. */
  public void scale(final float value) {
    scale = value;
  }

  @Nonnull
  public SkeletonData readSkeletonData(@Nonnull final JSONObject root) {
    return readSkeletonData(root, null);
  }

  @Nonnull
  public SkeletonData readSkeletonData(@Nonnull final JSONObject root, @Nullable final String name) {
    final float scale = this.scale;
    final SkeletonData skeletonData = new SkeletonData();

    skeletonData.name = name == null ? "" : name;

    // Skeleton.
    final JSONObject skeletonMap = root.optObject("skeleton");
    skeletonData.hash = skeletonMap.optString("hash", null);
    skeletonData.version = skeletonMap.optString("spine", null);
    skeletonData.width = skeletonMap.optFloat("width", 0);
    skeletonData.height = skeletonMap.optFloat("height", 0);
    skeletonData.imagesPath = skeletonMap.optString("images", null);

    // Bones.
    final JSONArray bones = root.optArray("bones");
    for(int i = 0, n = bones.size(); i < n; ++i) {
      final JSONObject boneMap = bones.optObject(i);

      BoneData parent = null;
      final String parentName = boneMap.optString("parent", null);
      if(parentName != null) {
        parent = skeletonData.findBone(parentName);
        if(parent == null) {
          throw new SpineException("Parent bone not found: " + parentName);
        }
      }

      final BoneData boneData = new BoneData(boneMap.getString("name"), parent);
      boneData.length = boneMap.optFloat("length", 0.0f) * scale;
      boneData.x = boneMap.optFloat("x", 0.0f) * scale;
      boneData.y = boneMap.optFloat("y", 0.0f) * scale;
      boneData.rotation = boneMap.optFloat("rotation", 0.0f);
      boneData.scaleX = boneMap.optFloat("scaleX", 1.0f);
      boneData.scaleY = boneMap.optFloat("scaleY", 1.0f);
      boneData.flipX = boneMap.optBoolean("flipX", false);
      boneData.flipY = boneMap.optBoolean("flipY", false);
      boneData.inheritScale = boneMap.optBoolean("inheritScale", true);
      boneData.inheritRotation = boneMap.optBoolean("inheritRotation", true);

      final String color = boneMap.optString("color", null);
      if(color != null) {
        boneData.setColor(Color.valueOf(color));
      }

      skeletonData.bones.push(boneData);
    }

    // IK constraints.
    final JSONArray iks = root.optArray("ik");
    for(int i = 0, n = iks.size(); i < n; ++i) {
      final JSONObject ikMap = iks.optObject(i);
      final IkConstraintData ikConstraintData = new IkConstraintData(ikMap.getString("name"));

      for(final JSON boneMap : ikMap.optArray("bones")) {
        final String boneName = boneMap.stringValue();
        final BoneData bone = skeletonData.findBone(boneName);

        if(bone == null) {
          throw new SpineException("IK bone not found: " + boneName);
        }

        ikConstraintData.bones.push(bone);
      }

      final String targetName = ikMap.getString("target");
      ikConstraintData.target = skeletonData.findBone(targetName);

      if(ikConstraintData.target == null) {
        throw new SpineException("Target bone not found: " + targetName);
      }

      ikConstraintData.bendDirection = ikMap.optBoolean("bendPositive", true) ? 1 : -1;
      ikConstraintData.mix = ikMap.optFloat("mix", 1.0f);

      skeletonData.ikConstraints.push(ikConstraintData);
    }

    // Slots.
    final JSONArray slots = root.optArray("slots");
    for(int i = 0, n = slots.size(); i < n; ++i) {
      final JSONObject slotMap = slots.optObject(i);
      final String slotName = checkNotNull(slotMap.getString("name"));
      final String boneName = slotMap.getString("bone");
      final BoneData boneData = skeletonData.findBone(boneName);

      if(boneData == null) {
        throw new SpineException("Slot bone not found: " + boneName);
      }

      final SlotData slotData = new SlotData(slotName, boneData);
      final String color = slotMap.optString("color", null);

      if(color != null) {
        slotData.color(Color.valueOf(color));
      }

      slotData.attachmentName = slotMap.optString("attachment", null);
      slotData.blendMode = BlendModeMapping.map(slotMap.optString("blend", "normal"));
      skeletonData.slots.push(slotData);
    }

    // Skins.
    final JSONObject skins = root.optObject("skins");
    for(final String skinName : skins.keySet()) {
      final JSONObject skinMap = skins.optObject(skinName);
      final Skin skin = new Skin(skinName);

      for(final String slotName : skinMap.keySet()) {
        int slotIndex = skeletonData.findSlotIndex(slotName);
        final JSONObject slotEntry = skinMap.optObject(slotName);

        if(slotIndex == -1) {
          throw new SpineException("Slot not found: "+slotName);
        }

        for(final String attachmentName : slotEntry.keySet()) {
          final JSONObject entry = slotEntry.optObject(attachmentName);
          final Attachment attachment = readAttachment(skin, attachmentName, entry);
          if(attachment != null) {
            skin.addAttachment(slotIndex, attachmentName, attachment);
          }
        }
      }

      skeletonData.skins.push(skin);

      if("default".equals(skin.name)) {
        skeletonData.defaultSkin = skin;
      }
    }

    // Events.
    final JSONObject events = root.optObject("events");
    for(final String eventName : events.keySet()) {
      final JSONObject eventMap = events.optObject(eventName);
      final EventData eventData = new EventData(eventName);
      eventData.intValue = eventMap.optInt("int", 0);
      eventData.floatValue = eventMap.optFloat("float", 0f);
      eventData.stringValue = eventMap.optString("string", null);
      skeletonData.events.push(eventData);
    }

    // Animations.
    final JSONObject animations = root.optObject("animations");
    for(final String animationName : animations.keySet()) {
      readAnimation(animationName, animations.optObject(animationName), skeletonData);
    }

    skeletonData.bones.trimToSize();
    skeletonData.slots.trimToSize();
    skeletonData.skins.trimToSize();
    skeletonData.events.trimToSize();
    skeletonData.animations.trimToSize();
    skeletonData.ikConstraints.trimToSize();

    return skeletonData;
  }

  @Nullable
  private Attachment readAttachment(@Nonnull final  Skin skin,
                                    @Nonnull String name,
                                    @Nonnull final JSONObject map) {
    name = map.optString("name", name);

    final float scale = this.scale;
    final String path = map.optString("path", name);

    switch(map.optString("type","region")) {
      case "region": {
        final RegionAttachment attachment =
            attachmentLoader.newRegionAttachment(skin, name, path);

        if(attachment == null) {
          return null;
        }

        attachment.path(path);
        attachment.x(map.optFloat("x", 0) * scale);
        attachment.y(map.optFloat("y", 0) * scale);
        attachment.scaleX(map.optFloat("scaleX", 1));
        attachment.scaleY(map.optFloat("scaleY", 1));
        attachment.rotation(map.optFloat("rotation", 0));
        attachment.setWidth(map.optFloat("width") * scale);
        attachment.setHeight(map.optFloat("height") * scale);

        final String color = map.optString("color", null);
        if(color != null) {
          attachment.color(Color.valueOf(color));
        }

        attachment.updateOffset();
        return attachment;
      }

      case "mesh": {
        final MeshAttachment mesh =
            attachmentLoader.newMeshAttachment(skin, name, path);

        if(mesh == null) {
          return null;
        }

        float[] vertices = checkNotNull(map.getArray("vertices")).toFloatArray();
        if(scale != 1.0f) {
          for(int i = 0, n = vertices.length; i < n; i++) {
            vertices[i] *= scale;
          }
        }

        mesh.path(path);
        mesh.vertices(vertices);
        mesh.regionUVs(checkNotNull(map.getArray("uvs")).toFloatArray());
        mesh.triangles(checkNotNull(map.getArray("triangles")).toShortArray());
        mesh.updateUVs();

        final String color = map.optString("color", null);
        if(color != null) {
          mesh.color(Color.valueOf(color));
        }

        mesh.hullLength(map.optInt("hull") * 2);

        if(map.contains("edges")) {
          mesh.edges(checkNotNull(map.getArray("edges")).toIntArray());
        }

        mesh.width(map.optFloat("width", 0.0f) * scale);
        mesh.height(map.optFloat("height", 0.0f) * scale);

        return mesh;
      }

      case "skinnedmesh": {
        final SkinnedMeshAttachment attachment =
            attachmentLoader.newSkinnedMeshAttachment(skin, name, path);

        if(attachment == null) {
          return null;
        }

        final float[] uvs = checkNotNull(map.getArray("uvs")).toFloatArray();
        final float[] vertices = checkNotNull(map.getArray("vertices")).toFloatArray();

        attachment.path(path);

        final FloatArray weights = new FloatArray(uvs.length * 3 * 3);
        final IntArray bones = new IntArray(uvs.length * 3);

        for(int i = 0, n = vertices.length; i < n;) {
          final int boneCount = (int)vertices[i++];

          bones.push(boneCount);

          for(int nn = i + boneCount * 4; i < nn; i += 4) {
            bones.push((int)vertices[i]);

            weights.push(vertices[i + 1] * scale);
            weights.push(vertices[i + 2] * scale);
            weights.push(vertices[i + 3]);
          }
        }

        attachment.bones(bones.toArray());
        attachment.weights(weights.toArray());
        attachment.regionUVs(uvs);
        attachment.triangles(checkNotNull(map.getArray("triangles")).toShortArray());
        attachment.updateUVs();

        final String color = map.optString("color", null);
        if(color != null) {
          attachment.color(Color.valueOf(color));
        }

        attachment.hullLength(map.optInt("hull") * 2);
        if(map.contains("edges")) {
          attachment.edges(checkNotNull(map.getArray("edges")).toIntArray());
        }

        attachment.width(map.optFloat("width", 0.0f) * scale);
        attachment.height(map.optFloat("height", 0.0f) * scale);

        return attachment;
      }

      case "boundingbox": {
        final BoundingBoxAttachment attachment = attachmentLoader.newBoundingBoxAttachment(skin, name);

        if(attachment == null) {
          return null;
        }

        final float[] vertices = checkNotNull(map.getArray("vertices")).toFloatArray();

        if(scale != 1.0f) {
          for(int i = 0, n = vertices.length; i < n; i++) {
            vertices[i] *= scale;
          }
        }

        attachment.setVertices(vertices);

        return attachment;
      }
    }

    return null;
  }

  private void readAnimation(@Nonnull final String name,
                             @Nonnull final JSONObject map,
                             @Nonnull final SkeletonData skeletonData) {
    final Array<Timeline> timelines = new Array<>();
    final float scale = this.scale;
    float duration = 0;

    // Slot timelines.
    final JSONObject slots = map.optObject("slots");
    for(final String slotName : slots.keySet()) {
      final JSONObject slotMap = slots.optObject(slotName);
      final int slotIndex = skeletonData.findSlotIndex(slotName);

      if(slotIndex == -1) {
        throw new SpineException("Slot not found: "+slotName);
      }

      final JSONArray colorTimeline = slotMap.optArray("color", null);
      if(colorTimeline != null) {
        final ColorTimeline timeline = new ColorTimeline(colorTimeline.size());
        timeline.slotIndex = slotIndex;

        for(int frameIndex = 0, frameCount = colorTimeline.size(); frameIndex < frameCount; ++frameIndex) {
          final JSONObject valueMap = colorTimeline.optObject(frameIndex);
          int color = Color.valueOf(checkNotNull(valueMap.getString("color")));
          timeline.setFrame(frameIndex, valueMap.getFloat("time"), color);
          readCurve(timeline, frameIndex, valueMap);
        }

        timelines.push(timeline);
        duration = Math.max(duration, timeline.getFrames()[timeline.getFrameCount() * 5 - 5]);
      }

      final JSONArray attachmentTimeline = slotMap.optArray("attachment", null);
      if(attachmentTimeline != null) {
        final AttachmentTimeline timeline = new AttachmentTimeline(attachmentTimeline.size());
        timeline.slotIndex = slotIndex;

        for(int frameIndex = 0, frameCount = attachmentTimeline.size(); frameIndex < frameCount; ++frameIndex) {
          final JSONObject valueMap = attachmentTimeline.optObject(frameIndex);
          timeline.setFrame(frameIndex, valueMap.getFloat("time"), valueMap.getString("name"));
        }

        timelines.push(timeline);
        duration = Math.max(duration, timeline.getFrames()[timeline.getFrameCount() - 1]);
      }
    }

    // Bone timelines.
    final JSONObject bones = map.optObject("bones");
    for(final String boneName : bones.keySet()) {
      final JSONObject boneMap = bones.optObject(boneName);
      final int boneIndex = skeletonData.findBoneIndex(boneName);

      if(boneIndex == -1) {
        throw new SpineException("Bone not found: " + boneName);
      }

      final JSONArray rotateTimeline = boneMap.optArray("rotate", null);
      if(rotateTimeline != null) {
        final RotateTimeline timeline = new RotateTimeline(rotateTimeline.size());
        timeline.boneIndex = boneIndex;

        for(int frameIndex = 0, frameCount = rotateTimeline.size(); frameIndex < frameCount; ++frameIndex) {
          final JSONObject valueMap = rotateTimeline.optObject(frameIndex);
          timeline.setFrame(frameIndex, valueMap.getFloat("time"), valueMap.getFloat("angle"));
          readCurve(timeline, frameIndex, valueMap);
        }
        timelines.push(timeline);
        duration = Math.max(duration, timeline.getFrames()[timeline.getFrameCount() * 2 - 2]);
      }

      final JSONArray translateTimeline = boneMap.optArray("translate", null);
      if(translateTimeline != null) {
        duration =
            readTranslateTimeline(
                timelines, boneIndex, duration,
                translateTimeline, new TranslateTimeline(translateTimeline.size()), scale);
      }

      final JSONArray scaleTimeline = boneMap.optArray("scale", null);
      if(scaleTimeline != null) {
        duration = readTranslateTimeline(
            timelines, boneIndex, duration,
            scaleTimeline, new ScaleTimeline(scaleTimeline.size()), 1.0f);
      }

      final JSONArray flipXTimeline = boneMap.optArray("flipX", null);
      if(flipXTimeline != null) {
        duration = readFlipTimeline(
            timelines, boneIndex, duration,
            flipXTimeline, "x", new FlipXTimeline(flipXTimeline.size()));

      }

      final JSONArray flipYTimeline = boneMap.optArray("flipY", null);
      if(flipYTimeline != null) {
        duration = readFlipTimeline(
            timelines, boneIndex, duration,
            flipYTimeline, "y", new FlipYTimeline(flipYTimeline.size()));
      }
    }

    // IK timelines.
    final JSONObject iks = map.optObject("ik");
    for(final String ikConstraintName : iks.keySet()) {
      final JSONArray ikMap = iks.optArray(ikConstraintName);
      final IkConstraintData ikConstraint = skeletonData.findIkConstraint(ikConstraintName);
      final IkConstraintTimeline timeline = new IkConstraintTimeline(ikMap.size());
      timeline.ikConstraintIndex = skeletonData.ikConstraints().identityIndexOf(ikConstraint);
      for(int frameIndex = 0, frameCount = ikMap.size(); frameIndex < frameCount; ++frameIndex) {
        final JSONObject valueMap = ikMap.optObject(frameIndex);
        timeline.setFrame(frameIndex, valueMap.getFloat("time"), valueMap.optFloat("mix", 1.0f),
            valueMap.getBoolean("bendPositive") ? 1 : -1);
        readCurve(timeline, frameIndex, valueMap);
      }
      timelines.push(timeline);
      duration = Math.max(duration, timeline.getFrames()[timeline.getFrameCount() * 3 - 3]);
    }

    // FFD timelines.
    final JSONObject ffds = map.optObject("ffd");
    for(final String skinName : ffds.keySet()) {
      final JSONObject ffdMap = ffds.optObject(skinName);
      final Skin skin = skeletonData.findSkin(skinName);

      if(skin == null) {
        throw new SpineException("Skin not found: " + skinName);
      }

      for(final String slotKey : ffdMap.keySet()) {
        final JSONObject slotMap = ffdMap.optObject(slotKey);
        final int slotIndex = skeletonData.findSlotIndex(slotKey);

        if(slotIndex == -1) {
          throw new SpineException("Slot not found: " + slotKey);
        }

        for(final String meshName : slotMap.keySet()) {
          final JSONArray meshMap = slotMap.optArray(meshName);
          final FfdTimeline timeline = new FfdTimeline(meshMap.size());
          final Attachment attachment = skin.getAttachment(slotIndex, meshName);

          if(attachment == null) {
            throw new SpineException("FFD attachment not found: " + meshName);
          }

          timeline.slotIndex = slotIndex;
          timeline.attachment = attachment;

          final int vertexCount;

          if(attachment instanceof MeshAttachment) {
            vertexCount = ((MeshAttachment) attachment).vertices().length;
          } else {
            vertexCount = ((SkinnedMeshAttachment) attachment).weights().length / 3 * 2;
          }

          for(int frameIndex = 0, frameCount = meshMap.size(); frameIndex < frameCount; ++frameIndex) {
            final JSONObject value = meshMap.optObject(frameIndex);
            final JSONArray verticesValue = value.optArray("vertices", null);
            float[] vertices;

            if(verticesValue == null) {
              if(attachment instanceof MeshAttachment) {
                vertices = ((MeshAttachment)attachment).vertices();
              } else {
                vertices = new float[vertexCount];
              }
            } else {
              //TODO: avoid double copy of float array if vertexCount == verticesValue.length
              //TODO: avoid double iteration for scale
              vertices = new float[vertexCount];
              int start = value.optInt("offset", 0);
              System.arraycopy(verticesValue.toFloatArray(), 0, vertices, start, verticesValue.size());

              if(scale != 1) {
                for(int ii = start, nn = ii + verticesValue.size(); ii < nn; ii++) {
                  vertices[ii] *= scale;
                }
              }

              if(attachment instanceof MeshAttachment) {
                float[] meshVertices = ((MeshAttachment)attachment).vertices();

                for(int ii = 0; ii < vertexCount; ii++) {
                  vertices[ii] += meshVertices[ii];
                }
              }
            }

            timeline.setFrame(frameIndex, value.getFloat("time"), vertices);
            readCurve(timeline, frameIndex, value);
          }

          timelines.push(timeline);
          duration = Math.max(duration, timeline.getFrames()[timeline.getFrameCount() - 1]);
        }
      }
    }

    // Draw order timeline.
    JSONArray drawOrderValues = map.optArray("drawOrder", null);

    if(drawOrderValues == null) {
      drawOrderValues = map.optArray("draworder", null);
    }

    if(drawOrderValues != null) {
      final DrawOrderTimeline timeline = new DrawOrderTimeline(drawOrderValues.size());
      final int slotCount = skeletonData.slots.size();

      for(int frameIndex = 0, frameCount = drawOrderValues.size(); frameIndex < frameCount; ++frameIndex) {
        final JSONObject drawOrderMap = drawOrderValues.optObject(frameIndex);
        int[] drawOrder = null;

        final JSONArray offsets = drawOrderMap.optArray("offsets", null);

        if(offsets != null) {
          drawOrder = new int[slotCount];

          Arrays.fill(drawOrder, -1);

          final int[] unchanged = new int[slotCount - offsets.size()];

          int originalIndex = 0, unchangedIndex = 0;

          for(int i = 0, n = offsets.size(); i < n; ++i) {
            final JSONObject offsetMap = offsets.optObject(i);
            final int slotIndex = skeletonData.findSlotIndex(offsetMap.getString("slot"));

            if(slotIndex == -1) {
              throw new SpineException("Slot not found: " + offsetMap.getString("slot"));
            }

            // Collect unchanged items.
            while(originalIndex != slotIndex) {
              unchanged[unchangedIndex++] = originalIndex++;
            }

            // Set changed items.
            drawOrder[originalIndex + offsetMap.getInt("offset")] = originalIndex++;
          }

          // Collect remaining unchanged items.
          while(originalIndex < slotCount) {
            unchanged[unchangedIndex++] = originalIndex++;
          }

          // Fill in unchanged items.
          for(int i = slotCount - 1; i >= 0; i--) {
            if(drawOrder[i] == -1) {
              drawOrder[i] = unchanged[--unchangedIndex];
            }
          }
        }

        timeline.setFrame(frameIndex, drawOrderMap.getFloat("time"), drawOrder);
      }

      timelines.push(timeline);
      duration = Math.max(duration, timeline.getFrames()[timeline.getFrameCount() - 1]);
    }

    // Event timeline.
    final JSONArray eventsMap = map.optArray("events", null);
    if(eventsMap != null) {
      final EventTimeline timeline = new EventTimeline(eventsMap.size());

      for(int frameIndex = 0, frameCount = eventsMap.size(); frameIndex < frameCount; ++frameIndex) {
        final JSONObject eventMap = eventsMap.optObject(frameIndex);
        final EventData eventData = skeletonData.findEvent(eventMap.getString("name"));

        if(eventData == null) {
          throw new SpineException("Event not found: " + eventMap.getString("name"));
        }

        final Event event = new Event(eventData);
        event.intValue = eventMap.optInt("int", eventData.intValue());
        event.floatValue = eventMap.optFloat("float", eventData.floatValue());
        event.stringValue = eventMap.optString("string", eventData.stringValue());
        timeline.setFrame(frameIndex, eventMap.getFloat("time"), event);
      }

      timelines.push(timeline);
      duration = Math.max(duration, timeline.getFrames()[timeline.getFrameCount() - 1]);
    }

    timelines.trimToSize();
    skeletonData.animations.push(new Animation(name, timelines, duration));
  }

  void readCurve(@Nonnull final CurveTimeline timeline,
                 final int frameIndex,
                 @Nonnull final JSONObject valueMap) {
    final JSON curve = valueMap.opt("curve");

    if(curve.isString() && "stepped".equals(curve.stringValue())) {
      timeline.setStepped(frameIndex);
    } else if (curve.isArray()) {
      final JSONArray curveArray = (JSONArray)curve;

      timeline.setCurve(
          frameIndex,
          curveArray.getFloat(0), curveArray.getFloat(1),
          curveArray.getFloat(2), curveArray.getFloat(3));
    }
  }

  float readTranslateTimeline(@Nonnull final  Array<Timeline> timelines,
                              final int boneIndex,
                              final float duration,
                              @Nonnull final JSONArray timelineData,
                              @Nonnull final TranslateTimeline timeline,
                              final float timelineScale) {
    timeline.boneIndex = boneIndex;

    for(int frameIndex = 0, frameCount = timelineData.size(); frameIndex < frameCount; ++frameIndex) {
      final JSONObject valueMap = timelineData.optObject(frameIndex);
      final float x = valueMap.optFloat("x", 0.0f) * timelineScale;
      final float y = valueMap.optFloat("y", 0.0f) * timelineScale;
      timeline.setFrame(frameIndex, valueMap.getFloat("time"), x, y);
      readCurve(timeline, frameIndex, valueMap);
    }

    timelines.push(timeline);
    return Math.max(duration, timeline.getFrames()[timeline.getFrameCount() * 3 - 3]);
  }

  float readFlipTimeline(@Nonnull final  Array<Timeline> timelines,
                         final int boneIndex,
                         final float duration,
                         @Nonnull final JSONArray timelineData,
                         @Nonnull final String field,
                         @Nonnull final FlipXTimeline timeline) {
    timeline.boneIndex = boneIndex;

    for(int frameIndex = 0, frameCount = timelineData.size(); frameIndex < frameCount; ++frameIndex) {
      final JSONObject value = timelineData.optObject(frameIndex);
      timeline.setFrame(frameIndex, value.getFloat("time"), value.optBoolean(field, false));
    }

    timelines.push(timeline);
    return Math.max(duration, timeline.getFrames()[timeline.getFrameCount() * 3 - 3]);
  }
}
