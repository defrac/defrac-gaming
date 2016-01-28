package defrac.gaming;

import defrac.app.ScreenAppDelegate;
import defrac.ios.uikit.UIKit;
import defrac.ui.Screen;
import defrac.ui.ScreenStack;

import javax.annotation.Nonnull;

/**
 *
 */

public final class Main extends ScreenAppDelegate {
  public static void main(String[] args) {
    UIKit.applicationMain(args, null, Main.class);
  }

  @Nonnull
  @Override
  protected Screen createScreen() {
    return new ScreenStack(new SampleScreen());
  }
}
