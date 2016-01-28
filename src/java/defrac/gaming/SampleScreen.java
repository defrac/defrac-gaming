package defrac.gaming;

import defrac.lang.Lazy;
import defrac.lang.Supplier;
import defrac.ui.*;

import javax.annotation.Nonnull;

/**
 *
 */
public final class SampleScreen extends ContentScreen {
  @Nonnull
  private static final Sample[] SAMPLES = {
      new Sample("Spine Raptor", SpineRaptorSample::new),
      new Sample("Layout", LayoutSample::new),
  };

  public SampleScreen() {}

  @Override
  protected void onCreate() {
    super.onCreate();

    final LinearLayout layout = LinearLayout.vertical().gravity(Gravity.CENTER);
    final LayoutConstraints layoutConstraints =
        new LinearLayout.LayoutConstraints(200, LayoutConstraints.WRAP_CONTENT, PixelUnits.LP).
            marginTop(8, PixelUnits.LP);

    title("defrac Gaming");

    layout.addView(
        new Label().text("Gaming Samples"));

    for(final Sample sample : SAMPLES) {
      final Button button = new Button();

      button.layoutConstraints(layoutConstraints);
      button.text(sample.name);
      button.clickListener(sender -> {
        pushScreen(sample.screen.get());
        return false;
      });

      layout.addView(button);
    }

    rootView(layout);
  }

  private static class Sample {
    @Nonnull public final String name;
    @Nonnull public final Lazy<Screen> screen;

    Sample(@Nonnull final String name,
           @Nonnull final Supplier<Screen> screen) {
      this.name = name;
      this.screen = new Lazy.Value<Screen>() {
        @Nonnull
        @Override
        protected Screen computeValue() {
          return screen.get();
        }
      };
    }
  }
}
