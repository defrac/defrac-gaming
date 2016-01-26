package defrac.gaming;

import defrac.ui.FrameBuilder;

import static defrac.lang.Preconditions.checkNotNull;
import static defrac.web.Toplevel.document;

/**
 *
 */
public final class Main {
  public static void main(String[] args) {
    checkNotNull(document().body).
        style.backgroundColor = "#666";

    FrameBuilder.
        forScreen(new SampleScreen()).
        show();
  }
}
