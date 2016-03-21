package defrac.gaming;

import android.content.Intent;
import android.os.Build;
import defrac.dni.Activity;
import defrac.dni.IntentFilter;
import defrac.ui.Screen;
import defrac.ui.ScreenActivity;
import defrac.ui.ScreenStack;

import javax.annotation.Nonnull;

@Activity(label = "defrac-ui", filter = @IntentFilter(action = Intent.ACTION_MAIN, category = Intent.CATEGORY_LAUNCHER))
public final class Main extends ScreenActivity {
  @Override
  @Nonnull
  protected Screen createScreen() {
    return new ScreenStack(new SampleScreen());
  }
}
