package com.example.skillloop;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;

/** Small, consistent tactile feedback for tappable UI without intercepting click events. */
public final class SkillLoopMotion {
    private SkillLoopMotion() { }

    public static void attachToClickableViews(View root) {
        if (root == null) return;
        if (root.isClickable() && !(root instanceof EditText) && root.getTag(R.id.motion_feedback_tag) == null) {
            root.setTag(R.id.motion_feedback_tag, Boolean.TRUE);
            root.setOnTouchListener((view, event) -> {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        view.animate().cancel();
                        view.animate().scaleX(0.975f).scaleY(0.975f).alpha(0.88f).setDuration(85).setInterpolator(new DecelerateInterpolator()).start();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        view.animate().cancel();
                        view.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(160).setInterpolator(new DecelerateInterpolator()).start();
                        break;
                }
                return false;
            });
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) attachToClickableViews(group.getChildAt(i));
        }
    }
}
