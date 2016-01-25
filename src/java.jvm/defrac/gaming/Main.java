package defrac.gaming;

import defrac.concurrent.Dispatchers;
import defrac.ui.FrameBuilder;

/**
 *
 */
public final class Main {
  public static void main(String[] args) {
    Dispatchers.FOREGROUND.exec(() -> {
      FrameBuilder.
          forScreen(new SampleScreen()).
          title("defrac Gaming").
          width(1024).
          height(768).
          resizable().
          show();
    });
  }
}
