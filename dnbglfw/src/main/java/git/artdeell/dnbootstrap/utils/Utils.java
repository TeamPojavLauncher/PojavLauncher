package git.artdeell.dnbootstrap.utils;

import java.lang.ref.WeakReference;

public class Utils {
    public static <T> T getWeakReference(WeakReference<T> reference) {
        if(reference == null) return null;
        return reference.get();
    }
}
