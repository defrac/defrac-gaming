package defrac.gaming;

import android.content.Intent;
import android.os.Build;
import defrac.dni.Activity;
import defrac.dni.IntentFilter;
import defrac.dni.UsesSdk;
import defrac.ui.Screen;
import defrac.ui.ScreenActivity;

import javax.annotation.Nonnull;

@Activity(label = "defrac-ui", filter = @IntentFilter(action = Intent.ACTION_MAIN, category = Intent.CATEGORY_LAUNCHER))
@UsesSdk(minSdkVersion = Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public final class Main extends ScreenActivity {
  @Override
  @Nonnull
  protected Screen createScreen() {
    return new SampleScreen();
  }
}
