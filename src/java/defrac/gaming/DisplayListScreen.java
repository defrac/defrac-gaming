package defrac.gaming;

import defrac.display.Stage;
import defrac.ui.*;

import javax.annotation.Nonnull;

/**
 *
 */
public abstract class DisplayListScreen extends ContentScreen {
  private DisplayList displayList;
  protected Stage stage;

  @Override
  protected void onCreate() {
    super.onCreate();

    final FrameLayout layout = new FrameLayout();

    addDisplayList(layout);
    addButton(layout);

    rootView(layout);
  }

  private void addButton(@Nonnull  final FrameLayout layout) {
    final FrameLayout.LayoutConstraints layoutConstraints =
        new FrameLayout.LayoutConstraints();
    final Button button = new Button();

    layoutConstraints.width(LayoutConstraints.WRAP_CONTENT);
    layoutConstraints.height(LayoutConstraints.WRAP_CONTENT);
    layoutConstraints.gravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
    layoutConstraints.marginBottom(32, PixelUnits.LP);

    button.text("Back");
    button.layoutConstraints(layoutConstraints);
    button.clickListener(sender -> { dismiss(); return false; });

    layout.addView(button);
  }

  private void addDisplayList(@Nonnull  final FrameLayout layout) {
    final FrameLayout.LayoutConstraints layoutConstraints =
        new FrameLayout.LayoutConstraints();

    layoutConstraints.width(LayoutConstraints.MATCH_PARENT);
    layoutConstraints.height(LayoutConstraints.MATCH_PARENT);

    displayList = new DisplayList();
    displayList.layoutConstraints(layoutConstraints);
    displayList.onStageReady(stage -> { this.stage = stage; initWithStage(stage); });

    layout.addView(displayList);
  }

  protected abstract void initWithStage(@Nonnull final Stage stage);

  @Override
  protected void onPause() {
    super.onPause();
    displayList.onPause();
  }

  @Override
  protected void onResume() {
    super.onResume();
    displayList.onResume();
  }
}
