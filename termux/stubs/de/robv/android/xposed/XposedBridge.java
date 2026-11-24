package de.robv.android.xposed;
import java.util.Set;
public final class XposedBridge {
    public static void log(String s) {}
    public static void log(Throwable t) {}
    public static Set<XC_MethodHook.Unhook> hookAllMethods(Class<?> c, String n, XC_MethodHook h) { return null; }
}
