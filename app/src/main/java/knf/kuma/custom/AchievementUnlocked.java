package knf.kuma.custom;

import static android.text.TextUtils.isEmpty;
import static android.view.Gravity.CENTER_HORIZONTAL;
import static android.view.View.GONE;
import static android.widget.LinearLayout.VERTICAL;
import static java.lang.Boolean.FALSE;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.util.Collection;

import knf.kuma.App;
import knf.kuma.BuildConfig;

/**
 * Basically an animated toast notification with queue support.
 * <p>
 * It uses a set of invisible views (called 'fake') to measure
 * the data before showing it to user. This similar to using measureText
 * method but more accurate.
 * <p>
 * Doesn't work with power-saving mode on unless you implement your
 * own valueAnimator class.
 * <p>
 * This is 'all-in-one' library. You have to copy the class file
 * to your package folder, otherwise you won't have access to inner
 * classes such as AchievementData and listener.
 * <p>
 * Don't forget to grant 'draw over apps' permission (SYSTEM_ALERT_WINDOW)
 * <p>
 * GPL
 * By Darkion Avey @ http://darkion.net/
 */
@SuppressWarnings({"unused", "SetTextI18n"})
public class AchievementUnlocked {
    //animation interpolators
    private final static TimeInterpolator TIME_INTERPOLATOR = new DeceleratingInterpolator(50);
    private static final String TAG = "AU";
    private final OvershootInterpolator overshootInterpolator = new OvershootInterpolator();
    private final AnticipateInterpolator anticipateInterpolator = new AnticipateInterpolator();
    private final TimeInterpolator accelerateInterpolator = new AccelerateInterpolator(50);
    private final int focusable = WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
    private final int nonFocusable = WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
    private final boolean DEBUG = BuildConfig.DEBUG;
    private int currentContainerWidth;
    //dimens
    private int smallSize, largeSize, elevation, paddingLarge, paddingSmall, translationY, margin;
    private int initialSize = -1;
    //indices of data iterator
    private int index = 0;
    private final Context context;
    private boolean dismissible = false;
    private boolean added = false;
    //achievements data
    private AchievementData[] achievements;
    private int readingDelay = 1300;
    private int matchParent;
    private boolean dismissed = false;
    private AchievementListenerAdapter listener;
    private boolean isPowerSavingModeOn = false;
    private boolean isLarge = true, alignTop = true, isRounded = true;
    private boolean notchMode = VERSION.SDK_INT >= 26;
    private Integer statusBarHeight;
    private ViewGroup container;
    private AchievementIconView icon;
    private TextView titleTextView;
    private ScrollTextView subtitleTextView;
    private ViewGroup achievementLayout;
    private WindowManager.LayoutParams mainViewLP;
    private float mPxPerSeconds = 40;
    //private AchievementQueue queue = new AchievementQueue();
    private boolean initiatedGlobalFields = false;
    private boolean hasBeenDismissed = false;

    public AchievementUnlocked(Context context) {
        this.context = context;
        initGlobalFields();
    }

    /**
     * For debugging purposes
     */
    static long getScaledDuration(int duration) {
        return (long) (1f * duration);
    }

    private static int countMatches(final String str, final String sub) {
        if (isEmpty(str) || isEmpty(sub)) {
            return 0;
        }
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }

    /**
     * Set how many pixels should be scrolled per second when
     * the subtitle is scrollable (longer than screen width)
     * <p>
     * Default value is 40;
     *
     * @param PxPerSeconds higher values will result in faster
     *                     scrolling
     */
    public void setScrollingPxPerSeconds(float PxPerSeconds) {
        this.mPxPerSeconds = PxPerSeconds;
    }

    /**
     * Indicate that the system is running on a notched device.
     * This is set to true on Oreo+ devices since TYPE_SYSTEM_ERROR is
     * deprecated anyway and the popup will have to move below the status bar
     *
     * @param statusBarHeight custom status bar height (y shift) since this library
     *                        does not have direct access to decor view. You can supply
     *                        a null value and use the hardcoded status bar height
     */
    public void setNotchMode(@Nullable Integer statusBarHeight) {
        this.notchMode = true;
        if (statusBarHeight != null) this.statusBarHeight = statusBarHeight;
    }

    /**
     * Indicate whether the popup should appear on top of the screen
     * or not
     *
     * @param alignTop true for top alignment
     * @return same AchievementUnlocked object
     */
    public AchievementUnlocked setTopAligned(boolean alignTop) {
        this.alignTop = alignTop;
        return this;
    }

    /**
     * Set how many milliseconds the popup should wait before the next
     * animation is played. This value is ignored when the popup width
     * exceeds display width (aka scrolling popup).
     * The default value is 1500 which is 1.5 seconds
     *
     * @param readingDelay reading duration in milliseconds
     * @return same AchievementUnlocked object
     */
    public AchievementUnlocked setReadingDelay(int readingDelay) {
        this.readingDelay = readingDelay;
        return this;

    }

    /**
     * Set true if you want the popup to be rounded. Default
     * value is true
     *
     * @param rounded true for complete rounded appearance, false for rounded box
     * @return same AchievementUnlocked object
     */
    public AchievementUnlocked setRounded(boolean rounded) {
        isRounded = rounded;
        return this;
    }

    /**
     * Callbacks for different events occurring throughout the popup's
     * life span
     *
     * @param listener the listener to be used
     */
    public void setAchievementListener(@Nullable AchievementListenerAdapter listener) {
        this.listener = listener;
    }

    /**
     * Set to true if you want the popup to be large. Default value
     * is true. Large popup height is 65dp whereas the small one is
     * 50dp
     *
     * @param large true for large popups, false for small ones
     * @return same AchievementUnlocked object
     */
    public AchievementUnlocked setLarge(boolean large) {
        this.isLarge = large;
        return this;
    }

    /**
     * @return the popup view without the scrim. You should not modify
     * any of its properties since that might cause the animations
     * to go haywire
     */
    public View getAchievementView() {
        return container;
    }

    /**
     * @return the title text view
     */
    public TextView getTitleTextView() {
        return titleTextView;
    }

    /**
     * @return the subtitle text view
     */
    public TextView getSubtitleTextView() {
        return subtitleTextView;
    }

    /**
     * @return the icon view
     */
    public View getIconView() {
        return icon;
    }

    /**
     * @return get the view containing the scrim (background fade) and
     * the popup. You should not modify any of its properties since that might
     * cause the animations to go haywire
     */
    public ViewGroup getAchievementParent() {
        return achievementLayout;
    }

    private int convertDpToPixel(float dp) {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return Math.round(px);
    }

    @SuppressLint({"ObsoleteSdkInt", "SetTextI18n"})
    private void initGlobalFields() {
        try {
            if (!initiatedGlobalFields) {
                margin = convertDpToPixel(16);
                elevation = convertDpToPixel(10);
                paddingLarge = convertDpToPixel(10);
                paddingSmall = convertDpToPixel(5);
                smallSize = convertDpToPixel(50);
                largeSize = convertDpToPixel(65);
                translationY = convertDpToPixel(20);

                achievementLayout = new RelativeLayout(context);
                achievementLayout.setClipToPadding(FALSE);
                LayoutParams motherLayoutLP = new LayoutParams(-2, -2);
                achievementLayout.setLayoutParams(motherLayoutLP);
                achievementLayout.setTag("motherLayout");
                LinearLayout textContainerFake = new LinearLayout(context);
                textContainerFake.setOrientation(VERTICAL);
                textContainerFake.setPadding(convertDpToPixel(10), 0, convertDpToPixel(20), 0);
                textContainerFake.setVisibility(View.INVISIBLE);
                LayoutParams textContainerFakeLP = new LayoutParams(-2, -2);
                textContainerFakeLP.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
                textContainerFake.setLayoutParams(textContainerFakeLP);
                textContainerFake.setTag("textContainerFake");
                TextView titleFake = new TextView(context);
                titleFake.setText("Title");
                LayoutParams titleFakeLP = new LayoutParams(-2, -2);
                titleFake.setLayoutParams(titleFakeLP);
                titleFake.setTag("titleFake");
                titleFake.setMaxLines(1);
                ScrollTextView subtitleFake = new ScrollTextView(context);
                subtitleFake.setText("Subtitle");
                subtitleFake.setVisibility(GONE);
                subtitleFake.setMaxLines(1);
                LayoutParams subtitleFakeLP = new LayoutParams(-2, -2);
                subtitleFake.setLayoutParams(subtitleFakeLP);
                subtitleFake.setTag("subtitleFake");
                textContainerFake.addView(titleFake);
                textContainerFake.addView(subtitleFake);
                achievementLayout.addView(textContainerFake);
                container = new RelativeLayout(context);
                container.setClipToPadding(false);
                container.setClipChildren(false);

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    achievementLayout.setClipToOutline(true);
                }

                LayoutParams achievementBodyLP = new LayoutParams(-2, largeSize);
                achievementBodyLP.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
                achievementBodyLP.addRule(CENTER_HORIZONTAL, RelativeLayout.TRUE);
                achievementBodyLP.bottomMargin = achievementBodyLP.topMargin = convertDpToPixel(10);

                if ((VERSION.SDK_INT >= 26 || notchMode) && alignTop) {
                    achievementBodyLP.topMargin += statusBarHeight == null ? Math.round(getStatusBarHeight() * 1.7f) : statusBarHeight;
                }

                container.setLayoutParams(achievementBodyLP);
                container.setTag("achievementBody");
                LinearLayout achievementIconBg = new LinearLayout(context);
                LayoutParams achievementIconBgLP = new LayoutParams(largeSize, largeSize);
                achievementIconBg.setLayoutParams(achievementIconBgLP);
                achievementIconBg.setTag("achievementIconBg");
                container.addView(achievementIconBg);
                icon = new AchievementIconView(context);
                icon.setPadding(convertDpToPixel(7), convertDpToPixel(7), convertDpToPixel(7), convertDpToPixel(7));
                LayoutParams achievementIconLP = new LayoutParams(largeSize, largeSize);
                icon.setMaxWidth(largeSize);
                icon.setLayoutParams(achievementIconLP);
                icon.setTag("achievementIcon");
                achievementIconBg.addView(icon);
                LinearLayout textContainer = new LinearLayout(context);
                textContainer.setClipToPadding(false);
                textContainer.setClipChildren(false);
                textContainer.setOrientation(VERTICAL);
                textContainer.setTag("textContainer");
                LayoutParams textContainerLP = new LayoutParams(-2, -2);
                textContainer.setLayoutParams(textContainerLP);
                textContainerLP.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
                container.addView(textContainer);

                container.setTag("achievementBody");
                titleTextView = new TextView(context);
                titleTextView.setText("Title");
                titleTextView.setMaxLines(1);
                LayoutParams titleLP = new LayoutParams(-2, -2);
                titleTextView.setLayoutParams(titleLP);
                titleTextView.setTag("title");
                subtitleTextView = new ScrollTextView(context);
                subtitleTextView.setText("Subtitle");
                subtitleTextView.setVisibility(GONE);
                subtitleTextView.setLayoutParams(titleLP);
                subtitleTextView.setMaxLines(1);
                subtitleTextView.setTag("subtitle");
                textContainer.addView(titleTextView);
                textContainer.addView(subtitleTextView);
                achievementLayout.addView(container);


                if (mainViewLP == null) {
                    mainViewLP = new WindowManager.LayoutParams(
                            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT,
                            WindowOverlayCompat.TYPE_SYSTEM_ERROR, focusable,
                            PixelFormat.TRANSLUCENT);
                }

                if (titleTextView != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isLarge) {
                        titleTextView.setGravity(View.TEXT_ALIGNMENT_CENTER);
                    }
                    titleTextView.setSingleLine(true);
                    titleTextView.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                        }

                        @Override
                        public void afterTextChanged(Editable s) {
                            ((TextView) achievementLayout.findViewWithTag("titleFake")).setText(titleTextView.getText());
                        }
                    });
                }
                if (subtitleTextView != null) {
                    subtitleTextView.setSingleLine(true);
                    subtitleTextView.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                        }

                        @Override
                        public void afterTextChanged(Editable s) {
                            ((TextView) achievementLayout.findViewWithTag("subtitleFake")).setText(subtitleTextView.getText());
                        }
                    });
                }
                initiatedGlobalFields = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Set to true if you want the popup to be swipeable
     *
     * @param dismissible true for swipe to dismiss behaviour
     */
    public void setDismissible(boolean dismissible) {
        this.dismissible = dismissible;
        if (dismissible) {
            achievementLayout.setOnTouchListener(new SwipeDismissTouchListener());
            container.setOnTouchListener(new SwipeDismissTouchListener());
        } else {
            achievementLayout.setOnTouchListener(null);
            container.setOnTouchListener(null);
        }
    }

    private int getTargetWidth(AchievementData data) {
        View textContainerFake = achievementLayout.findViewWithTag("textContainerFake");
        ((TextView) textContainerFake.findViewWithTag("titleFake")).setText(data.getTitle());
        ((TextView) textContainerFake.findViewWithTag("subtitleFake")).setText(data.getSubtitle());
        textContainerFake.measure(MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        return textContainerFake.getMeasuredWidth();
    }

    private void buildAchievement() {
        initGlobalFields();
        int padding;
        if (isLarge) {
            initialSize = largeSize;
            padding = paddingLarge;
        } else {
            initialSize = smallSize;
            padding = paddingSmall;
        }
        ((View) icon.getParent()).invalidate();
        icon.setPadding(padding, padding, padding, padding);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            container.setElevation(elevation);
        }
        titleTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s == null || s.length() == 0) {
                    titleTextView.setVisibility(GONE);
                } else {
                    titleTextView.setVisibility(View.VISIBLE);
                }
            }
        });
        final TextView fakeTitle = (achievementLayout.findViewWithTag("titleFake"));
        fakeTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s == null || s.length() == 0) {
                    fakeTitle.setVisibility(GONE);
                } else {
                    fakeTitle.setVisibility(View.VISIBLE);
                }
            }
        });
        final TextView fakeSubTitle = (achievementLayout.findViewWithTag("subtitleFake"));
        fakeSubTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s == null || s.length() == 0) {
                    fakeSubTitle.setVisibility(GONE);
                } else {
                    fakeSubTitle.setVisibility(View.VISIBLE);
                }
            }
        });
        subtitleTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s == null || s.length() == 0) {
                    subtitleTextView.setVisibility(GONE);
                } else subtitleTextView.setVisibility(View.VISIBLE);
            }
        });
        titleTextView.setAlpha(0f);
        titleTextView.setTranslationY(translationY);
        subtitleTextView.setTranslationY(translationY);
        subtitleTextView.setAlpha(0f);
        container.setScaleY(0f);
        container.setScaleX(0f);
        container.setVisibility(GONE);

        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();

        matchParent = Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels) - margin;
        //stretched = 900;
        //  textContainer.setVisibility(View.GONE);

        View textContainer = achievementLayout.findViewWithTag("textContainer");
        if (textContainer != null) {
            textContainer.setPadding(convertDpToPixel(10) + (initialSize), 0, convertDpToPixel(20), 0);
            achievementLayout.findViewWithTag("textContainerFake").setPadding(textContainer.getPaddingLeft(), textContainer.getPaddingTop(), textContainer.getPaddingRight(), textContainer.getPaddingBottom());
        }
        icon.setMaxWidth(initialSize);
        container.getLayoutParams().width = container.getLayoutParams().height = icon.getLayoutParams().height = icon.getLayoutParams().width = ((View) icon.getParent()).getLayoutParams().height = ((View) icon.getParent()).getLayoutParams().width = initialSize;
        container.requestLayout();


        if (alignTop) {
            mainViewLP.gravity = Gravity.TOP;
        } else {
            mainViewLP.gravity = Gravity.BOTTOM;
        }
        // No scrim for Android P
        if (alignTop && VERSION.SDK_INT < 28 && (achievementLayout.getBackground() == null || !(achievementLayout.getBackground() instanceof GradientDrawable))) {
            GradientDrawable scrim = new GradientDrawable();
            scrim.setShape(GradientDrawable.RECTANGLE);
            scrim.setColors(new int[]{0x40000000, 0});
            scrim.setAlpha(0);
            achievementLayout.setBackground(scrim);
            achievementLayout.setClipToPadding(false);
        } else if (!alignTop) {
            achievementLayout.setBackground(null);
        }

        final WindowManager manager = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE));
        if (manager == null) throw new RuntimeException("No window manager found");
        manager.addView(achievementLayout, mainViewLP);
        added = true;
    }

    private void setTextColor(int textColor) {
        subtitleTextView.setTextColor(Color.parseColor("#B2FFFFFF"));
        titleTextView.setTextColor(Color.rgb(Color.red(textColor), Color.green(textColor), Color.blue(textColor)));
    }

    /**
     * use listeners instead
     */
    @Deprecated
    public AchievementUnlocked createViews() {
        buildAchievement();
        return this;
    }

    public void show(Collection<AchievementData> data) {
        show(data.toArray(new AchievementData[0]));
    }

    /**
     * Pop the popup with the supplied data
     *
     * @param data data to be shown
     */
    public void show(AchievementData... data) {
        if (data == null || data.length == 0) {
            return;
        }
        //Check permission first
        if (VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(context)) {
            if (DEBUG)
                Toast.makeText(context, "'canDrawOverlays' permission is not granted", Toast.LENGTH_LONG).show();
            Log.e(TAG, "'canDrawOverlays' permission is not granted");
            return;
        }
        //Don't bother if powersaving is on
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            isPowerSavingModeOn = powerManager != null && powerManager.isPowerSaveMode();
            if (isPowerSavingModeOn) {
                Log.w(TAG, "Power saving is on, AU was canceled");
                return;
            }
        }

        if (added) {
            if (achievements != null) {
                achievements = concat(achievements, data);
            } else
                achievements = data;
            return;
        }
        dismissWithoutAnimation();
        this.achievements = data;
        buildAchievement();
        setContainerBg(achievements[0].getBackgroundColor());
        if (listener != null)
            listener.onViewCreated(this, data);
        prepareMorphism();
    }

    /**
     * Instantly remove the popup view from window manager
     */
    public void dismissWithoutAnimation() {
        removeView();
        if (listener != null)
            listener.onAchievementDismissed(this);
    }

    private void removeListeners(Animator animatorSet) {
        if (animatorSet == null) return;
        if (animatorSet instanceof AnimatorSet && !((AnimatorSet) animatorSet).getChildAnimations().isEmpty())
            for (Animator animator : ((AnimatorSet) animatorSet).getChildAnimations()) {
                removeListeners(animator);
            }
        else {
            if (animatorSet instanceof ValueAnimator) {
                ((ValueAnimator) animatorSet).removeAllUpdateListeners();
            }
            animatorSet.removeAllListeners();
            animatorSet.end();
            animatorSet.cancel();
        }
    }

    private AchievementData[] concat(AchievementData[] a, AchievementData[] b) {
        int aLen = a.length;
        int bLen = b.length;
        AchievementData[] c = new AchievementData[aLen + bLen];
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);
        return c;
    }

    @SuppressLint("ObsoleteSdkInt")
    private void setBackground(View v, Drawable d) {
        v.setBackground(d);
    }

    private void removeView() {
        if (!added) return;
        index = 0;
        setSwipeEffect(0);

        hasBeenDismissed = false;
        isPowerSavingModeOn = false;
        icon.setVisibility(View.VISIBLE);
        setBackground(((View) icon.getParent()), null);
        setBackground(container, null);
        setBackground(icon, null);
        isLarge = true;
        alignTop = true;
        isRounded = true;
        icon.setOnClickListener(null);
        container.setOnClickListener(null);
        achievementLayout.setOnClickListener(null);
        achievementLayout.setVisibility(View.VISIBLE);
        //    container.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
        container.setOnTouchListener(null);
        container.setVisibility(View.VISIBLE);
        container.setTranslationX(0f);
        container.setAlpha(1f);
        achievementLayout.setAlpha(1f);
        setDismissible(false);
        listener = null;
        ((View) icon.getParent()).setBackground(null);
        dismissed = false;
        final WindowManager manager = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE));

        try {
            if (manager != null) manager.removeView(achievementLayout);
            added = false;
        } catch (Exception e) {
            e.printStackTrace();
            // *shrug emoji*
            //there's no way to check if view is already added to windowManager or not, probably the exception is nullPointerException where achievementLayout is null
            //best thing we could do is to check added boolean
        }
    }

    private int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }

    private int getStartValue(int start) {
        return clamp(start, initialSize, matchParent);
    }

    private int getEndValue(int end) {
        return Math.min(end, matchParent);
    }

    private ValueAnimator getContainerStretchAnimation(int start, int end) {
        final ValueAnimator containerStretch = ValueAnimator.ofInt(getStartValue(start), getEndValue(end));
        containerStretch.addUpdateListener(valueAnimator -> {
            if (!dismissed) {
                int val = (Integer) valueAnimator.getAnimatedValue();
                ViewGroup.LayoutParams layoutParams = container.getLayoutParams();
                layoutParams.width = val;
                currentContainerWidth = val;
                container.setLayoutParams(layoutParams);
            }
        });
        containerStretch.setInterpolator(TIME_INTERPOLATOR);
        containerStretch.setDuration(getScaledDuration(300));
        return containerStretch;
    }

    private GradientDrawableWithColors getContainerBg() {
        if ((container.getBackground()) instanceof GradientDrawableWithColors)
            return (GradientDrawableWithColors) (container.getBackground());
        GradientDrawableWithColors iconBackground = new GradientDrawableWithColors();
        if (isRounded)
            iconBackground.setCornerRadius(initialSize / 2f);
        else iconBackground.setCornerRadius(convertDpToPixel(2));
        return iconBackground;
    }

    private void setContainerBg(int color) {
        Drawable bgDrawable = container.getBackground();
        if (bgDrawable instanceof GradientDrawable)
            ((GradientDrawableWithColors) bgDrawable).setColor(color);
        else {
            GradientDrawableWithColors iconBackground = getContainerBg();
            iconBackground.setColor(color);
            setBackground(container, iconBackground);
        }
    }

    private int getIconBgColor(int defaultColor) {
        Drawable bgDrawable = ((View) icon.getParent()).getBackground();
        if (bgDrawable instanceof GradientDrawable)
            return ((GradientDrawableWithColors) bgDrawable).getGradientColor();
        return defaultColor;
    }

    private int getContainerBgColor(int defaultColor) {
        Drawable bgDrawable = container.getBackground();
        if (bgDrawable instanceof GradientDrawable)
            return ((GradientDrawableWithColors) bgDrawable).getGradientColor();
        return defaultColor;
    }

    private GradientDrawableWithColors getIconBg() {
        if ((((View) icon.getParent()).getBackground()) instanceof GradientDrawable)
            return (GradientDrawableWithColors) (((View) icon.getParent()).getBackground());
        GradientDrawableWithColors iconBackground = new GradientDrawableWithColors();
        if (isRounded)
            iconBackground.setShape(GradientDrawable.OVAL);
        else iconBackground.setCornerRadius(convertDpToPixel(2));
        return iconBackground;
    }

    private void setIconBg(int color) {
        Drawable bgDrawable = (((View) icon.getParent()).getBackground());
        if (bgDrawable instanceof GradientDrawable)
            bgDrawable.setColorFilter(Color.argb(bgDrawable.getAlpha(), Color.red(color), Color.green(color), Color.blue(color)), PorterDuff.Mode.SRC_IN);
        else {
            GradientDrawableWithColors iconBackground = getIconBg();
            iconBackground.setColor(color);
            setBackground(((View) icon.getParent()), iconBackground);
        }
    }

    private AnimatorSet getExitAnimation() {
        final ObjectAnimator containerScale = ObjectAnimator.ofFloat(container, View.SCALE_X, 1f, 0f);
        containerScale.addUpdateListener(animation -> {
            if (!dismissed)
                container.setScaleY((float) animation.getAnimatedValue());
        });
        containerScale.setDuration(getScaledDuration(250));
        containerScale.setStartDelay(100);
        containerScale.setInterpolator(anticipateInterpolator);
        boolean scrimIsAvailable = alignTop && achievementLayout.getBackground() != null;
        ObjectAnimator scrim = null;
        if (scrimIsAvailable) {
            scrim = ObjectAnimator.ofInt(achievementLayout.getBackground(), "alpha", 255, 0);
        }
        AnimatorSet out = new AnimatorSet();
        if (scrim != null)
            out.playTogether(containerScale, scrim);
        else out.play(containerScale);
        AnimatorSet set = new AnimatorSet();
        set.playSequentially(getContainerStretchAnimation(Math.min(container.getMeasuredWidth(), matchParent), initialSize), out);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                try {
                    super.onAnimationEnd(animation);
                    dismissWithoutAnimation();
                } catch (Exception e) {
                    //
                }
            }
        });
        return set;
    }

    private int getContainerBackgroundColor() {
        if ((container).getBackground() != null)
            if ((container).getBackground() instanceof GradientDrawableWithColors)
                return ((GradientDrawableWithColors) (container).getBackground()).getGradientColor();
        return 0xffffffff;
    }

    private int getSubtitleLines(String subtitleRaw) {
        if (subtitleRaw.contains("\n"))
            return countMatches(subtitleRaw, "\n") + 1;
        return 1;
    }

    private boolean isBlank(final String cs) {
        int strLen;
        if (cs == null || (strLen = cs.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean allClear(AnimatorSet[] sets) {
        for (AnimatorSet set : sets) {
            if (set == null) return false;
        }
        return true;
    }

    private AnimatorSet morphData() {
        AnimatorSet sets = new AnimatorSet();
        AchievementData data = achievements[index];
        sets.play(animateData(achievements[index]));
        sets.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                try {
                    super.onAnimationEnd(animation);
                    if (AchievementUnlocked.this.achievements != null && !hasBeenDismissed && AchievementUnlocked.this.achievements.length > 0 && index + 1 < AchievementUnlocked.this.achievements.length) {
                        index++;
                        morphData().start();
                    } else
                        getExitAnimation().start();
                } catch (Exception e) {
                    //
                }
            }
        });
        return sets;
    }

    private AnimatorSet animateData(final AchievementData data) {
        final AnimatorSet backgroundAnimators = new AnimatorSet();
        final AnimatorSet inAnimation = new AnimatorSet();
        final AnimatorSet outAnimation = new AnimatorSet();
        final AnimatorSet result = new AnimatorSet();

        ObjectAnimator titleIn, subtitleIn = null, titleOut, subtitleOut = null;
        if ((container.getTag() != null && container.getTag() != data)) {
            int previousBgColor = 0xffffffff;
            int previousIconBgColor = 0x30ffffff;
            if (index == 0) {
                previousBgColor = data.getBackgroundColor();
                previousIconBgColor = data.getIconBackgroundColor();
            } else if (index > 0 && index < achievements.length) {
                previousBgColor = achievements[index - 1].getBackgroundColor();
                previousIconBgColor = achievements[index - 1].getIconBackgroundColor();
            }
            ValueAnimator iconBgColor = ValueAnimator.ofInt(getIconBgColor(previousIconBgColor), data.getIconBackgroundColor());
            iconBgColor.setEvaluator(new ArgbEvaluator());
            iconBgColor.addUpdateListener(animation -> {
                if (!dismissed)
                    setIconBg((int) animation.getAnimatedValue());
            });
            ValueAnimator bgColor = ValueAnimator.ofInt(getContainerBgColor(previousBgColor), data.getBackgroundColor());
            bgColor.setEvaluator(new ArgbEvaluator());
            bgColor.addUpdateListener(animation -> {
                if (!dismissed)
                    setContainerBg((int) animation.getAnimatedValue());
            });
            bgColor.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    if (index > 0) setIcon(data);
                }
            });

            backgroundAnimators.play(iconBgColor).with(bgColor);
            backgroundAnimators.setInterpolator(TIME_INTERPOLATOR);
            backgroundAnimators.setDuration(getScaledDuration(300));


        }
        titleIn = ObjectAnimator.ofFloat(titleTextView, View.TRANSLATION_Y, translationY, 0);
        titleIn.addUpdateListener(animation -> {
            if (dismissed) return;
            titleTextView.setAlpha(animation.getAnimatedFraction());
        });
        titleIn.setDuration(getScaledDuration(300));
        titleIn.setInterpolator(TIME_INTERPOLATOR);

        titleOut = ObjectAnimator.ofFloat(titleTextView, View.TRANSLATION_Y, 0, translationY);
        titleOut.addUpdateListener(animation -> {
            if (dismissed) return;
            titleTextView.setAlpha(1f - animation.getAnimatedFraction());
        });
        titleOut.setInterpolator(accelerateInterpolator);

        final boolean dataHasSubtitle = dataHasSubtitle(data);
        //indicates that scrolling is needed
        final boolean overFlow = (matchParent) < getTargetWidth(data);
        final int startScrollingDelay = dataHasSubtitle ? 800 : 0;
        final int scrollDistance = overFlow ? Math.abs(getTargetWidth(data) - matchParent) : 0;
        //if the text is scrolling, pause for a while at the end before collapsing
        final int endReadingDelay = overFlow ? 400 : 0;

        final int duration;
        if (overFlow) {
            final float density = context.getResources().getDisplayMetrics().density;
            float dpPerSec = mPxPerSeconds * density;
            //if the scroll distance is short, then use standard readingDelay value since the animation
            //will run too quickly and user won't be abel to read the contents
            duration = scrollDistance <= matchParent / 4 ? readingDelay : Math.round(scrollDistance * 1000 / dpPerSec);
        } else {
            duration = readingDelay;
        }

        ValueAnimator stretch = getContainerStretchAnimation(container.getMeasuredWidth(), getTargetWidth(data));

        if (dataHasSubtitle) {
            subtitleIn = ObjectAnimator.ofFloat(subtitleTextView, View.TRANSLATION_Y, translationY, 0);
            subtitleIn.addUpdateListener(animation -> {
                if (dismissed) return;
                subtitleTextView.setAlpha(animation.getAnimatedFraction());
            });
            subtitleIn.setInterpolator(TIME_INTERPOLATOR);
            subtitleIn.setStartDelay(getScaledDuration(150));
            subtitleIn.setInterpolator(TIME_INTERPOLATOR);
            subtitleIn.setDuration(getScaledDuration(300));
        }
        //use previousWidth better than real-time measuring to increase performance

        if (dataHasSubtitle) {
            AnimatorSet textViews = new AnimatorSet();
            //this null check is useful when seperating subtitle different lines
            //into different poups
            if (titleIn != null)
                textViews.playTogether(titleIn, subtitleIn);
            else textViews.playTogether(subtitleIn);

            inAnimation.play(stretch).with(backgroundAnimators).before(textViews);
        } else {
            if (titleIn != null)
                inAnimation.play(stretch).with(backgroundAnimators).before(titleIn);
            else inAnimation.playTogether(backgroundAnimators, stretch);

        }
        // inAnimation.setInterpolator(interpolator);
        if (dataHasSubtitle) {
            subtitleOut = ObjectAnimator.ofFloat(subtitleTextView, View.TRANSLATION_Y, 0, translationY);
            subtitleOut.addUpdateListener(animation -> {
                if (dismissed) return;
                subtitleTextView.setAlpha(1f - animation.getAnimatedFraction());
            });
            subtitleOut.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    subtitleTextView.stopScrolling();

                }
            });
            subtitleOut.setInterpolator(accelerateInterpolator);

        }

        if (dataHasSubtitle) {
            if (titleOut != null) {
                titleOut.setStartDelay(getScaledDuration(150));
                outAnimation.playTogether(subtitleOut, titleOut);
            } else outAnimation.play(subtitleOut);
        } else {
            if (titleOut != null)
                outAnimation.play(titleOut);
        }
        final String title = data.getTitle(), subtitle = data.getSubtitle();
        result.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                if (listener != null)
                    listener.onAchievementMorphed(AchievementUnlocked.this, data);
                if (data.getPopUpOnClickListener() != null || dismissible) {
                    mainViewLP.flags = focusable;
                } else {
                    mainViewLP.flags = nonFocusable;
                }
                container.setOnClickListener(data.getPopUpOnClickListener());
                final WindowManager manager = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE));
                if (manager != null && added)
                    manager.updateViewLayout(achievementLayout, mainViewLP);
                subtitleTextView.setText(subtitle);
                subtitleTextView.updateScroller(scrollDistance);
                titleTextView.setText(title);
                setTextColor(data.getTextColor());
            }


        });

        ScrollTextView fake = (achievementLayout.findViewWithTag("subtitleFake"));
        fake.setText(data.getSubtitle());

        subtitleTextView.setDurations(getScaledDuration(duration + endReadingDelay), getScaledDuration(startScrollingDelay));
        outAnimation.setStartDelay(getScaledDuration(duration + endReadingDelay + startScrollingDelay));
        outAnimation.setDuration(getScaledDuration(300));
        result.playSequentially(inAnimation, outAnimation);
        result.setInterpolator(TIME_INTERPOLATOR);
        container.setTag(data);
        return result;
    }

    private void prepareMorphism() {
        if (achievements == null || achievements.length == 0)
            return;
        index = 0;
        AnimatorSet scene = new AnimatorSet();
        scene.playSequentially(getEntranceAnimation(achievements[0]), morphData());

        scene.start();
    }

    private boolean dataHasSubtitle(AchievementData data) {
        return data.getSubtitle() != null && data.getSubtitle().length() > 0 && !data.getSubtitle().isEmpty();
    }

    private AnimatorSet getEntranceAnimation(final AchievementData data) {
        final int iconBG = data.getIconBackgroundColor();
        // final Drawable iconDrawable = data.getIcon();
        // final int bg = data.getBackgroundColor();
        //ValueAnimator stretch = getContainerStretchAnimation(initialSize, getTargetWidth(data));
        ObjectAnimator containerScale = ObjectAnimator.ofFloat(container, View.SCALE_X, 0f, 1f);
        containerScale.addUpdateListener(animation -> {
            if (dismissed) return;
            container.setScaleY((float) animation.getAnimatedValue());
        });
        containerScale.setDuration(getScaledDuration(250));
        containerScale.setInterpolator(overshootInterpolator);
        boolean scrimIsAvailable = alignTop && achievementLayout.getBackground() != null;
        ObjectAnimator scrim = null;
        if (scrimIsAvailable) {
            scrim = ObjectAnimator.ofInt(achievementLayout.getBackground(), "alpha", 0, 255);
        }
        AnimatorSet set = new AnimatorSet();
        if (scrim != null)
            set.playTogether(containerScale, scrim);
        else set.play(containerScale);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                if (Color.alpha(iconBG) > 0) {
                    setIconBg(iconBG);
                } else {
                    View textContainer = (View) titleTextView.getParent();
                    textContainer.setPadding((isLarge ? largeSize : smallSize), textContainer.getPaddingTop(), textContainer.getPaddingRight(), textContainer.getPaddingBottom());
                    achievementLayout.findViewWithTag("textContainerFake").setPadding(textContainer.getPaddingLeft(), textContainer.getPaddingTop(), textContainer.getPaddingRight(), textContainer.getPaddingBottom());
                }
                container.setVisibility(View.VISIBLE);
                setIcon(data);
            }
        });
        return set;
    }

    private void setIcon(AchievementData data) {
        if (data == null) {
            //  icon.setDrawable(null);
            return;
        }
        if (data.getState() == AchievementIconView.AchievementIconViewStates.SAME_DRAWABLE)
            return;
        Drawable d = data.getIcon();
        if (d != null) {
            if (data.getState() == AchievementIconView.AchievementIconViewStates.FADE_DRAWABLE)
                icon.fadeDrawable(d);
            else icon.setDrawable(d);

        } else icon.setDrawable(null);
    }

    private void setSwipeEffect(float amount) {
        container.setTranslationX(amount);
    }

    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = Resources.getSystem().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = Resources.getSystem().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    /* used by the abstract class adapter */
    @SuppressWarnings("unused")
    interface AchievementListener {
        void onViewCreated(AchievementUnlocked achievement, AchievementData[] data);

        void onAchievementMorphed(AchievementUnlocked achievement, AchievementData data);

        void onAchievementDismissed(AchievementUnlocked achievement);
    }

    /**
     * Class that holds the data to be displayed by
     * AchievementUnlocked object using the
     * {@link AchievementUnlocked#show(AchievementData...)} method
     */
    public static class AchievementData {
        private String title = "", subtitle;
        private Drawable icon;
        private int textColor = 0xff000000, backgroundColor = 0xffffffff, iconBackgroundColor = 0x0;
        private View.OnClickListener onClickListener;
        private AchievementIconView.AchievementIconViewStates state = null;

        public static AchievementData copyFrom(AchievementData data) {
            AchievementData result = new AchievementData();
            result.setTitle(data.getTitle());
            result.setSubtitle(data.getSubtitle());
            result.setIcon(data.getIcon());
            result.setState(data.getState());
            result.setBackgroundColor(data.getBackgroundColor());
            result.setIconBackgroundColor(data.getIconBackgroundColor());
            result.setTextColor(data.getTextColor());
            result.setPopUpOnClickListener(data.getPopUpOnClickListener());
            return result;
        }

        public View.OnClickListener getPopUpOnClickListener() {
            return onClickListener;
        }

        /**
         * Assign a per-data onclick listener to the popup
         *
         * @return same AchievementData object
         */
        public AchievementData setPopUpOnClickListener(View.OnClickListener onClickListener) {
            this.onClickListener = onClickListener;
            return this;
        }

        public int getTextColor() {
            return textColor;
        }

        public AchievementData setTextColor(int textColor) {
            this.textColor = textColor;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public AchievementData setTitle(String title) {
            this.title = title;
            return this;
        }

        public String getSubtitle() {
            return subtitle;
        }

        public AchievementData setSubtitle(String subtitle) {
            this.subtitle = subtitle;
            return this;
        }

        public AchievementIconView.AchievementIconViewStates getState() {
            return state;
        }

        /**
         * Indicate whether the popup icon should stay the same or
         * fade when showing different Achievement data. Default is
         * null which is the same as SAME_DRAWABLE.
         * When FADE_DRAWABLE is set, the icon will animate change to the
         * next data icon.
         *
         * @param state either of these two: FADE_DRAWABLE, SAME_DRAWABLE
         */
        public void setState(AchievementIconView.AchievementIconViewStates state) {
            this.state = state;
        }

        public Drawable getIcon() {
            return icon;
        }

        /**
         * Set popuup icon. Transparent one will be used if non is assigned
         *
         * @param icon icon drawable
         * @return same AchievementData object
         */
        public AchievementData setIcon(Drawable icon) {
            this.icon = icon;
            return this;
        }

        int getBackgroundColor() {
            return backgroundColor;
        }

        /**
         * Set popup background color
         *
         * @param backgroundColor integer color of background
         * @return same AchievementData object
         */
        public AchievementData setBackgroundColor(int backgroundColor) {
            this.backgroundColor = backgroundColor;
            return this;
        }

        int getIconBackgroundColor() {
            return iconBackgroundColor;
        }

        /**
         * Set the background of the popup's icon
         *
         * @param iconBackgroundColor integer color
         * @return same AchievementData object
         */
        public AchievementData setIconBackgroundColor(int iconBackgroundColor) {
            this.iconBackgroundColor = iconBackgroundColor;
            return this;
        }
    }

    /**
     * Ticker text view used for subtitle view
     */
    @SuppressLint("AppCompatCustomView")
    @SuppressWarnings("unused")
    final static class ScrollTextView extends TextView {
        private static final LinearInterpolator LINEAR_INTERPOLATOR = new LinearInterpolator();
        private final ValueAnimator mScrollingAnimator = ValueAnimator.ofInt(0, 1);
        private long mDuration, mStartOffset;

        public ScrollTextView(Context context) {
            super(context);
            init();
        }

        public ScrollTextView(Context context, AttributeSet attrs) {
            super(context, attrs);
            init();
        }

        public ScrollTextView(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            init();
        }

        private void init() {
            setSingleLine();
        }


        @Override
        public void setVisibility(int visibility) {
            super.setVisibility(visibility);
            setSelected(visibility == VISIBLE);
        }

        @Override
        public void setAlpha(float alpha) {
            super.setAlpha(alpha);
            if (alpha <= 0.1f) {
                stopScrolling();
            }
        }

        public void stopScrolling() {
            if (mScrollingAnimator.isRunning())
                mScrollingAnimator.cancel();
        }

        public void startScrolling() {
            requestFocus();
            setSelected(true);

        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            stopScrolling();
        }


        /**
         * Directly apply previously-calculated values instead of recalculating them
         *
         * @param scrollingDuration how many milliseconds shall the animator take to finish
         *                          scrolling the overflown text
         * @param startOffset       how many milliseconds before the animator starts
         */
        public void setDurations(long scrollingDuration, long startOffset) {
            this.mDuration = scrollingDuration;
            this.mStartOffset = startOffset;
        }

        /**
         * Use previously-calculated values for efficiency
         *
         * @param scrollAmount previously-calculated horizontal scroll amount
         */
        void updateScroller(int scrollAmount) {
            if (mScrollingAnimator.isRunning()) {
                mScrollingAnimator.cancel();
            }
            if (scrollAmount == 0) return;
            mScrollingAnimator.setIntValues(0, -scrollAmount);
            mScrollingAnimator.removeAllListeners();
            mScrollingAnimator.removeAllUpdateListeners();
            mScrollingAnimator.addUpdateListener(animation -> scrollTo(-(Integer) animation.getAnimatedValue(), 0));
            mScrollingAnimator.setDuration(mDuration);
            //LINEAR_INTERPOLATOR must be used
            mScrollingAnimator.setInterpolator(LINEAR_INTERPOLATOR);
            mScrollingAnimator.setStartDelay(mStartOffset);
            mScrollingAnimator.start();

        }
    }

    /**
     * GradientDrawable that saves the drawable colors; used for AU background
     */
    final static class GradientDrawableWithColors extends GradientDrawable {
        private int mColor;

        int getGradientColor() {
            return mColor;
        }

        @Override
        public void setColor(int argb) {
            super.setColors(new int[]{argb, argb});
            mColor = argb;
        }

        @Override
        public void setColors(int[] colors) {
            super.setColors(colors);
            mColor = colors[0];
        }
    }

    /**
     * ImageView that animates drawable change. It also scales
     * according to the drawable size to make sure it doesn't clip
     */
    @SuppressLint("AppCompatCustomView")
    static class AchievementIconView extends ImageView {
        public AchievementIconView(Context context) {
            super(context);
        }

        public void setDrawable(final Drawable drawable) {
            if (drawable == null) {
                setImageDrawable(null);
                return;
            }
            if (getScaleType() != ScaleType.CENTER_CROP) setScaleType(ScaleType.CENTER_CROP);

            final float scaleX = 3.5f / (getMaxWidth() / drawable.getIntrinsicWidth());
            final float scaleY = 3.5f / (getMaxWidth() / drawable.getIntrinsicHeight());

            if (getDrawable() == null) {
                setImageDrawable(drawable);
                setScaleX(1 / Math.max(scaleX, scaleY));
                setScaleY(1 / Math.max(scaleX, scaleY));
            } else {
                if (drawable.getAlpha() < 255)
                    drawable.setAlpha(255);

                animate()
                        .scaleX(0f)
                        .setDuration(AchievementUnlocked.getScaledDuration(200))
                        .scaleY(0f)
                        .alpha(0f)
                        .withEndAction(() -> animate()
                                .setDuration(AchievementUnlocked.getScaledDuration(200))
                                .scaleX(1 / Math.max(scaleX, scaleY))
                                .scaleY(1 / Math.max(scaleX, scaleY))
                                .alpha(1f)
                                .withStartAction(() -> setImageDrawable(drawable))
                                .start())
                        .start();
            }
        }

        public void fadeDrawable(final Drawable drawable) {
            if (drawable == null) {
                setImageDrawable(null);
                return;
            }
            if (getScaleType() != ScaleType.CENTER_CROP) setScaleType(ScaleType.CENTER_CROP);

            final float scaleX = 3.5f / (getMaxWidth() / drawable.getIntrinsicWidth());
            final float scaleY = 3.5f / (getMaxWidth() / drawable.getIntrinsicHeight());

            if (getDrawable() == null) {
                setImageDrawable(drawable);
                setScaleX(1 / Math.max(scaleX, scaleY));
                setScaleY(1 / Math.max(scaleX, scaleY));
            } else {
                if (drawable.getAlpha() < 255)
                    drawable.setAlpha(255);

                animate()
                        .setDuration(AchievementUnlocked.getScaledDuration(50))
                        .alpha(0f)
                        .withEndAction(() -> animate()
                                .setDuration(AchievementUnlocked.getScaledDuration(50))
                                .alpha(1f)
                                .withStartAction(() -> setImageDrawable(drawable))
                                .start())
                        .start();
            }
        }

        public enum AchievementIconViewStates {
            FADE_DRAWABLE, SAME_DRAWABLE
        }

    }

    /**
     * Same as LogDecelerateInterpolator.java from Launcher3
     */
    final static class DeceleratingInterpolator implements TimeInterpolator {

        private final float mLogScale;
        private final int mBase;

        DeceleratingInterpolator(int base) {
            mBase = base;
            mLogScale = 1f / computeLog(1, mBase);
        }

        private static float computeLog(float t, int base) {
            return (float) -Math.pow(base, -t) + 1;
        }

        @Override
        public float getInterpolation(float t) {
            return computeLog(t, mBase) * mLogScale;
        }
    }

    final static class WindowOverlayCompat {
        private static final int ANDROID_OREO = 26;
        private static final int TYPE_APPLICATION_OVERLAY = 2038;
        static final int TYPE_SYSTEM_ERROR = Build.VERSION.SDK_INT < ANDROID_OREO ? WindowManager.LayoutParams.TYPE_SYSTEM_ERROR : TYPE_APPLICATION_OVERLAY;
    }

    private class SwipeDismissTouchListener implements View.OnTouchListener {
        private final int mSlop;
        private final int mMinFlingVelocity;
        private final int mMaxFlingVelocity;
        private final long mAnimationTime;
        private float mDownX;
        private boolean mSwiping;
        private float mTranslationX;
        private final Runnable end;

        SwipeDismissTouchListener() {
            ViewConfiguration vc = ViewConfiguration.get(App.Companion.getContext());
            mSlop = vc.getScaledTouchSlop();
            mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
            mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
            mAnimationTime = container.getContext().getResources().getInteger(
                    android.R.integer.config_shortAnimTime);
            end = new Runnable() {
                @Override
                public void run() {
                    hasBeenDismissed = true;

                    //Fade out the popup view instead of direct visibility
                    // change, for aesthetics.
                    // Since there is no scrim in bottom-aligned and Android P+,
                    // we can use setVisibility directly

                    if (alignTop && VERSION.SDK_INT < 28)
                        achievementLayout.animate().alpha(0f).setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                achievementLayout.setVisibility(GONE);
                            }
                        }).start();
                    else achievementLayout.setVisibility(GONE);
                }
            };
        }


        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            motionEvent.offsetLocation(mTranslationX, 0);
            float deltaX = (motionEvent.getRawX() - mDownX);

            switch (motionEvent.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                    mDownX = motionEvent.getRawX();
                    view.onTouchEvent(motionEvent);
                    return false;
                }
                case MotionEvent.ACTION_UP: {

                    if (container.getAlpha() == 0) {
                        dismissWithoutAnimation();
                        return true;
                    }

                    boolean dismiss = false;
                    boolean dismissRight = false;

                    int spaceToEdge = ((achievementLayout.getWidth() - container.getWidth()) / 2);
                    float swipePercentage = Math.abs(mTranslationX / spaceToEdge);


                    if (swipePercentage >= 0.5f) {
                        dismiss = true;
                        dismissRight = deltaX > 0;
                    }
                    if (dismiss) {
                        ObjectAnimator translation = ObjectAnimator.ofFloat(container, View.TRANSLATION_X, container.getTranslationX(), dismissRight ? container.getMeasuredWidth() : -container.getMeasuredWidth());
                        translation.addUpdateListener(animation -> setSwipeEffect((float) animation.getAnimatedValue()));
                        translation.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                if (end != null) end.run();
                            }
                        });
                        translation.setInterpolator(TIME_INTERPOLATOR);
                        translation.setDuration(mAnimationTime);
                        translation.start();
                        dismissed = true;
                    } else {
                        ObjectAnimator translation = ObjectAnimator.ofFloat(container, View.TRANSLATION_X, container.getTranslationX(), 0);
                        translation.addUpdateListener(animation -> setSwipeEffect((float) animation.getAnimatedValue()));
                        translation.setDuration(mAnimationTime);
                        translation.setInterpolator(TIME_INTERPOLATOR);
                        translation.start();

                        dismissed = false;
                    }
                    mTranslationX = 0;
                    mDownX = 0;
                    mSwiping = false;
                    break;
                }
                case MotionEvent.ACTION_MOVE: {

                    if (Math.abs(deltaX) > mSlop) {
                        mSwiping = true;
                        container.getParent().requestDisallowInterceptTouchEvent(true);
                        MotionEvent cancelEvent = MotionEvent.obtain(motionEvent);
                        cancelEvent.setAction(MotionEvent.ACTION_CANCEL |
                                (motionEvent.getActionIndex() << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
                        container.onTouchEvent(cancelEvent);
                    }
                    if (mSwiping) {
                        mTranslationX = deltaX;
                        setSwipeEffect(mTranslationX);
                        return true;
                    }
                    break;
                }
            }
            return false;
        }
    }

    /**
     * Adapter for listener
     */
    abstract class AchievementListenerAdapter implements AchievementListener {
        @Override
        public void onAchievementDismissed(AchievementUnlocked achievement) {
        }

        @Override
        public void onViewCreated(AchievementUnlocked achievement, AchievementData[] data) {
        }

        @Override
        public void onAchievementMorphed(AchievementUnlocked achievement, AchievementData data) {
        }
    }

}
