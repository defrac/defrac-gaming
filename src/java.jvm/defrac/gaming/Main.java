package defrac.gaming;

import defrac.concurrent.Dispatchers;
import defrac.ui.FrameBuilder;
import defrac.ui.ScreenStack;

/**
 *
 */
public final class Main {
  public static void main(String[] args) {
    Dispatchers.FOREGROUND.exec(() -> {
      FrameBuilder.
          forScreen(new ScreenStack(new SampleScreen())).
          title("defrac Gaming").
          width(1024).
          height(768).
          resizable().
          show();
    });
  }
}
