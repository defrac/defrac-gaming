package defrac.display.layout;

import defrac.util.Dictionary;

import javax.annotation.Nonnull;

/**
 *
 */
final class StringInterpolator {
  @Nonnull
  private final Dictionary<String> variables;

  StringInterpolator(@Nonnull final Dictionary<String> variables) {
    this.variables = variables;
  }

  @Nonnull
  public String interpolate(@Nonnull final String input) {
    final int length = input.length();
    final StringBuilder result = new StringBuilder(length);

    outer: for(int i = 0; i < length; ++i) {
      final char c = input.charAt(i);
      if(c == '$') {
        ++i;
        if(i < length && input.charAt(i) == '{') {
          final StringBuilder key = new StringBuilder();

          for(++i; i < length; ++i) {
            final char cc = input.charAt(i);

            if(cc == '}') {
              final String constant = variables.get(key.toString());
              if(constant == null) {
                throw new LayoutException("Missing constant \""+key+"\" in \""+input+'"');
              }
              result.append(constant);
              continue outer;
            } else {
              key.append(cc);
            }
          }

          throw new LayoutException("Missing \"}\" character in \""+input+'"');
        } else {
          result.append('$');
        }
      } else {
        result.append(c);
      }
    }

    return result.toString();
  }
}
