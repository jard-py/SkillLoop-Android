package com.example.skillloop;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

/** Shared animated navigation island used by all four primary destinations. */
public final class SkillLoopTabNavigation {
    private static final int DURATION_MS = 250;
    private static final int[] ICON_IDS = {
            R.id.nav_discover, R.id.nav_connect, R.id.nav_chats, R.id.nav_profile
    };
    private static final Class<?>[] DESTINATIONS = {
            DiscoverActivity.class, ConnectActivity.class,
            ChatsActivity.class, MyProfileActivity.class
    };

    private SkillLoopTabNavigation() { }

    public static void setup(Activity activity, int activeIndex) {
        View island = activity.findViewById(R.id.nav_island);
        View indicator = activity.findViewById(R.id.nav_active_pill);
        if (island == null || indicator == null || activeIndex < 0 || activeIndex >= ICON_IDS.length) {
            return;
        }

        for (int i = 0; i < ICON_IDS.length; i++) {
            final int targetIndex = i;
            View icon = activity.findViewById(ICON_IDS[i]);
            if (icon == null) continue;
            applyIconTint(icon, i == activeIndex ? Color.WHITE : Color.rgb(23, 23, 23));
            icon.setOnClickListener(view -> {
                if (targetIndex == activeIndex) {
            // Selecting the current tab is a safe no-op; the tab's own content remains visible.
            return;
        }
                animateAndNavigate(activity, island, indicator, targetIndex);
            });
        }

        island.post(() -> positionIndicator(activity, island, indicator, activeIndex));
    }

    private static void animateAndNavigate(Activity activity, View island, View indicator, int targetIndex) {
        View target = activity.findViewById(ICON_IDS[targetIndex]);
        if (target == null || target.getWidth() <= 0 || indicator.getWidth() <= 0) {
            navigate(activity, DESTINATIONS[targetIndex]);
            return;
        }
        float[] destination = indicatorPosition(island, target, indicator);
        boolean[] tintChanged = { false };
        indicator.animate().cancel();
        indicator.animate()
                .x(destination[0])
                .y(destination[1])
                .setDuration(DURATION_MS)
                .setInterpolator(new PathInterpolator(0.16f, 0.0f, 0.3f, 1.0f))
                .setUpdateListener(animation -> {
                    if (!tintChanged[0] && animation.getAnimatedFraction() >= 0.50f) {
                        tintChanged[0] = true;
                        for (int i = 0; i < ICON_IDS.length; i++) {
                            View icon = activity.findViewById(ICON_IDS[i]);
                            if (icon != null) applyIconTint(icon,
                                    i == targetIndex ? Color.WHITE : Color.rgb(23, 23, 23));
                        }
                    }
                })
                .withEndAction(() -> navigate(activity, DESTINATIONS[targetIndex]))
                .start();
    }

    private static void positionIndicator(Activity activity, View island, View indicator, int index) {
        View icon = activity.findViewById(ICON_IDS[index]);
        if (icon == null || icon.getWidth() <= 0 || icon.getHeight() <= 0) return;
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) indicator.getLayoutParams();
        lp.width = icon.getWidth();
        lp.height = icon.getHeight();
        lp.gravity = android.view.Gravity.TOP | android.view.Gravity.START;
        indicator.setLayoutParams(lp);
        int[] islandPosition = new int[2];
        int[] iconPosition = new int[2];
        island.getLocationOnScreen(islandPosition);
        icon.getLocationOnScreen(iconPosition);
        indicator.setX(iconPosition[0] - islandPosition[0]);
        indicator.setY(iconPosition[1] - islandPosition[1]);
        island.invalidate();
    }

    private static float[] indicatorPosition(View island, View icon, View indicator) {
        int[] islandPosition = new int[2];
        int[] iconPosition = new int[2];
        island.getLocationOnScreen(islandPosition);
        icon.getLocationOnScreen(iconPosition);
        float x = iconPosition[0] - islandPosition[0] + (icon.getWidth() - indicator.getWidth()) / 2f;
        float y = iconPosition[1] - islandPosition[1] + (icon.getHeight() - indicator.getHeight()) / 2f;
        return new float[]{x, y};
    }

    private static void applyIconTint(View view, int color) {
        if (view instanceof ImageView) {
            ((ImageView) view).setImageTintList(ColorStateList.valueOf(color));
        }
    }

    private static void navigate(Activity activity, Class<?> destination) {
        Intent intent = new Intent(activity, destination);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
        activity.overridePendingTransition(0, 0);
        activity.finish();
    }
}
