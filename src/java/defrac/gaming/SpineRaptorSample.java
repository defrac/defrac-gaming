package defrac.gaming;

import defrac.animation.spine.AnimationState;
import defrac.animation.spine.AnimationStateData;
import defrac.animation.spine.SkeletonData;
import defrac.animation.spine.SkeletonJson;
import defrac.concurrent.Future;
import defrac.display.SpineSkeleton;
import defrac.display.Stage;
import defrac.display.TextureAtlas;
import defrac.display.TextureDataSupplies;
import defrac.json.JSON;
import defrac.json.JSONObject;
import defrac.lang.Procedures;
import defrac.resource.LibgdxTextureAtlasResource;
import defrac.resource.StringResource;

import javax.annotation.Nonnull;

/**
 *
 */
public final class SpineRaptorSample {
  @Nonnull
  private final Stage stage;

  public SpineRaptorSample(@Nonnull final Stage stage) {
    this.stage = stage;

    final Future<TextureAtlas> atlasFuture =
        LibgdxTextureAtlasResource.from(
            "raptor/raptor.atlas",
            TextureDataSupplies.resource("raptor/")
        ).load();

    final Future<JSON> jsonFuture =
        JSON.parse(StringResource.from("raptor/raptor.json"));

    final Future<SkeletonData> dataFuture =
      atlasFuture.
          join(jsonFuture).
          map(pair -> {
            final SkeletonJson json = new SkeletonJson(pair.x);
            json.scale(0.5f);
            return json.readSkeletonData((JSONObject)pair.y);
          });

    dataFuture.
        onSuccess(this::init).
        onFailure(Procedures.printStackTrace());
  }

  private void init(final SkeletonData skeletonData) {
    final SpineSkeleton skeleton = new SpineSkeleton(skeletonData);

    stage.
        addChild(skeleton).
        moveTo(350, 550);

    // Defines mixing (crossfading) between animations.
    AnimationStateData stateData = new AnimationStateData(skeletonData);

    // Holds the animation state for a skeleton (current animation, time, etc).
    AnimationState state = new AnimationState(stateData);

    // Slow all animations down to 60% speed.
    state.timeScale(0.6f);

    // Queue animations on tracks 0 and 1.
    state.setAnimation(0, "walk", true);
    state.setAnimation(1, "empty", false);
    state.addAnimation(1, "gungrab", false, 2);

    stage.animationSystem().add(skeleton.animatable(state));
  }
}
