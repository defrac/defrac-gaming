package defrac.gaming;

import defrac.ui.*;

/**
 *
 */
public final class SampleScreen extends Screen {
  private DisplayList displayList;

  @Override
  protected void onCreate() {
    super.onCreate();

    final LinearLayout layout =
        LinearLayout.horizontal().gravity(Gravity.CENTER);

    final LinearLayout.LayoutConstraints layoutConstraints =
        new LinearLayout.LayoutConstraints();

    layoutConstraints.width(800, PixelUnits.LP);
    layoutConstraints.height(600, PixelUnits.LP);
    layoutConstraints.gravity = Gravity.CENTER;

    displayList = new DisplayList();
    displayList.layoutConstraints(layoutConstraints);

    layout.addView(displayList);
    rootView(layout);

    //displayList.onStageReady(LayoutSample::new);
    displayList.onStageReady(SpineRaptorSample::new);
  }

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
