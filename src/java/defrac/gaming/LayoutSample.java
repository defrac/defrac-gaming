package defrac.gaming;

import defrac.display.Stage;
import defrac.display.layout.LayoutContext;
import defrac.display.layout.LayoutInflater;
import defrac.json.JSON;
import defrac.lang.Procedures;
import defrac.resource.StringResource;

import javax.annotation.Nonnull;

/**
 *
 */
public final class LayoutSample {
  public LayoutSample(@Nonnull final Stage stage) {
    JSON.parse(StringResource.from("layout.json")).
        onSuccess(json -> {
          LayoutContext context = new LayoutContext();
          context.defineConstant("definedInCode", "foo");
          new LayoutInflater(context).inflate(stage, json).
              onSuccess(theVoid -> System.out.println("--- Layout Ready ---")).
              onFailure(Procedures.printStackTrace());
        });
  }
}
