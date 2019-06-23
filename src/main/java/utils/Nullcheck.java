package utils;

public abstract class Nullcheck {

    public static <T> void checkNotNull(final T var, final String varName) {
        if (var == null) {
            throw new NullPointerException("Variable \"" + varName + "\" is null!");
        }
    }

}
