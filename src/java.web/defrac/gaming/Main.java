package defrac.gaming;

import defrac.ui.FrameBuilder;
import defrac.ui.ScreenStack;

import static defrac.lang.Preconditions.checkNotNull;
import static defrac.web.Toplevel.document;

/**
 *
 */
public final class Main {
  public static void main(String[] args) {
    checkNotNull(document().body).
        style.backgroundColor = "#fff";

    FrameBuilder.
        forScreen(new ScreenStack(new SampleScreen())).
        appendCss("*{font-family: sans-serif}").
        show();
  }
}
