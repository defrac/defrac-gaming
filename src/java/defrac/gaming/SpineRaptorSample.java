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
public final class SpineRaptorSample extends DisplayListScreen {
  @Override
  protected void initWithStage(@Nonnull final Stage stage) {
    final Future<TextureAtlas> atlasFuture =
        LibgdxTextureAtlasResource.from(
            "raptor/raptor.atlas",
            TextureDataSupplies.premultipliedResource("raptor/")
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

    stage.addChild(skeleton);
    skeleton.registrationPoint(20, -250);
    skeleton.moveTo(stage.width() * 0.5f, stage.height() * 0.5f);

    // Defines mixing (crossfading) between animations.
    AnimationStateData stateData = new AnimationStateData(skeletonData);

    // Holds the animation state for a skeleton (current animation, time, etc).
    AnimationState state = new AnimationState(stateData);

    // Queue animations on tracks 0 and 1.
    state.setAnimation(0, "walk", true);
    state.setAnimation(1, "empty", false);
    state.addAnimation(1, "gungrab", false, 2);

    stage.globalEvents().onResize.add(e -> skeleton.moveTo(e.width * 0.5f, e.height * 0.5f));
    stage.animationSystem().add(skeleton.animatable(state));
  }
}
