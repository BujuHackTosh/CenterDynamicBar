package dev.centerbar;

import android.content.res.Resources;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/** Moves the crDroid dynamic bar chip (id/rounded_container > id/compose_view)
 *  out of the start-side container and centers it on the status bar. */
public class Hook implements IXposedHookLoadPackage {
    private static final String PKG = "com.android.systemui";
    private static final String TAG = "CenterDynamicBar: ";
    private static final Set<View> watched =
            Collections.newSetFromMap(new WeakHashMap<View, Boolean>());

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!PKG.equals(lpparam.packageName)) return;
        try {
            Class<?> c = XposedHelpers.findClass(
                    "com.android.systemui.statusbar.phone.PhoneStatusBarView",
                    lpparam.classLoader);
            XposedBridge.hookAllMethods(c, "onAttachedToWindow", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam p) {
                    try { watch((ViewGroup) p.thisObject); }
                    catch (Throwable t) { XposedBridge.log(TAG + t); }
                }
            });
            XposedBridge.log(TAG + "hooked PhoneStatusBarView");
        } catch (Throwable t) {
            XposedBridge.log(TAG + t);
        }
    }

    private static void watch(final ViewGroup bar) {
        if (!watched.add(bar)) return;
        bar.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            try {
                View chip = findChip(bar);
                if (chip != null && chip.getParent() != bar) {
                    bar.post(() -> moveToCenter(bar, chip));
                }
            } catch (Throwable t) { XposedBridge.log(TAG + t); }
        });
    }

    private static View findChip(ViewGroup bar) {
        Resources r = bar.getResources();
        int cid = r.getIdentifier("rounded_container", "id", PKG);
        int vid = r.getIdentifier("compose_view", "id", PKG);
        if (cid == 0 || vid == 0) return null;
        return find(bar, cid, vid);
    }

    private static View find(View v, int cid, int vid) {
        if (v.getId() == cid && v.findViewById(vid) != null) return v;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                View f = find(g.getChildAt(i), cid, vid);
                if (f != null) return f;
            }
        }
        return null;
    }

    private static void moveToCenter(ViewGroup bar, View chip) {
        try {
            if (chip.getParent() == bar) return;
            ViewGroup old = (ViewGroup) chip.getParent();
            if (old != null) old.removeView(chip);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER_HORIZONTAL);
            bar.addView(chip, lp);
            XposedBridge.log(TAG + "moved chip to center from "
                    + (old == null ? "null" : old.getClass().getName()));
        } catch (Throwable t) {
            XposedBridge.log(TAG + t);
        }
    }
}
