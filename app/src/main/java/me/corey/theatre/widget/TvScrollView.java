package me.corey.theatre.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

import java.util.ArrayList;

/**
 * @author corey
 * @date 2018/8/2
 * 自定义的 ScrollView, 拓展了如下功能：
 * 1）可为其添加一些监听，如滚动过程监听、闲置状态监听、触顶或触底监听等
 * 2）垂直方向的"十字交互"，即获焦的 view 始终处于中间位置
 * 3）可添加吸顶 view，吸顶的实现方案参考自：https://github.com/emilsjolander/StickyScrollViewItems
 */
public class TvScrollView extends ScrollView {

    private static final String TAG = "TvScrollView";

    /**
     * true if the scrollview is scrolled to the top
     */
    private boolean isScrolledToTop = true;

    /**
     * true if the scrollview is scrolled to the bottom
     */
    private boolean isScrolledToBottom = false;

    /**
     * scrollview's only child view
     */
    private View contentView;


    /**
     * 是否滚动到了 吸顶 view 需要显示的距离
     */
    private boolean pinned = false;

    /**
     * 闲置状态值
     */
    public static int SCROLL_STATE_IDLE = 0;

    /**
     * 闲置状态的判断原理是：停止滚动后，开启一个计时器判断滚动距离是否发生变化
     * CHECK_SCROLL_STOP_DELAY_MILLIS 这个值，即为计时器的判断间隔
     */
    private static final int CHECK_SCROLL_STOP_DELAY_MILLIS = 80;

    /**
     * Handler 消息 id
     */
    private static final int MSG_SCROLL = 1;

    /**
     * 滚动过程监听
     */
    private OnScrollChangeListener onScrollChangeListener;

    /**
     * callback listener
     */
    private OnBoardReachedListener onBoardReachedListener;

    /**
     * 是否滚动到了"应该显示吸顶view"的状态
     */
    private OnPinnedListener onPinnedListener;

    /**
     * 滚动状态监听，目前只做到了监听"闲置"状态
     */
    private OnScrollStateChangeListener onScrollStateChangeListener;

    /**
     * 吸顶 view 添加该 tag
     */
    public static final String STICKY_TAG = "sticky";

    private ArrayList<View> stickyViews;

    private View currentlyStickingView;

    private float stickyViewTopOffset;

    private int stickyViewLeftOffset;

    private boolean clippingToPadding;

    private boolean clipToPaddingHasBeenSet;

    private final Handler mHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {

        private int mLastY = Integer.MIN_VALUE;

        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == MSG_SCROLL) {
                final int scrollY = getScrollY();
                if (mLastY == scrollY) {
                    mLastY = Integer.MIN_VALUE;
                    setScrollState(SCROLL_STATE_IDLE);
                } else {
                    mLastY = scrollY;
                    restartCheckStopTiming();
                }
                return true;
            }
            return false;
        }
    });

    public TvScrollView(Context context) {
        this(context, null);
    }

    public TvScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TvScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (this.onScrollChangeListener != null) {
            onScrollChangeListener.onScrollChange(l, t);
        }
        notifyOnBorderListener();
        restartCheckStopTiming();
        doTheStickyThing();
    }

    /**
     * 控制滚动距离，实现十字交互
     *
     * @param child   请求获焦的 view
     * @param focused 真正获焦的 view
     */
    @Override
    public void requestChildFocus(View child, View focused) {
        int[] location = new int[2];
        focused.getLocationOnScreen(location);
        int scrollHeight = focused.getHeight() / 2 + location[1] - this.getHeight() / 2;
        smoothScrollBy(0, scrollHeight);
        super.requestChildFocus(child, focused);
    }

    /**
     * 通知滚动状态变化
     *
     * @param state
     */
    private void setScrollState(int state) {
        if (onScrollStateChangeListener != null) {
            onScrollStateChangeListener.onScrollStateChanged(this, state);
        }
    }

    /**
     * 重置计时器
     */
    private void restartCheckStopTiming() {
        mHandler.removeMessages(MSG_SCROLL);
        mHandler.sendEmptyMessageDelayed(MSG_SCROLL, CHECK_SCROLL_STOP_DELAY_MILLIS);
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setOnScrollChangeListener(OnScrollChangeListener onScrollChangeListener) {
        this.onScrollChangeListener = onScrollChangeListener;
    }

    public void setOnPinnedListener(OnPinnedListener listener) {
        this.onPinnedListener = listener;
    }

    public void setOnScrollStateChangeListener(OnScrollStateChangeListener onScrollStateChangeListener) {
        this.onScrollStateChangeListener = onScrollStateChangeListener;
    }


    /**
     * set an OnBoardReachedListener to this scrollview
     *
     * @param listener {@link OnBoardReachedListener}
     */
    public void setOnBoardReachedListener(OnBoardReachedListener onBoardReachedListener) {
        this.onBoardReachedListener = onBoardReachedListener;
    }

    /**
     * While scrolling, compute whether the scrollview is scrolled to the bottom or not
     */
    private void notifyOnBorderListener() {

        if (contentView == null) {
            contentView = getChildAt(0);
        }

        if (contentView != null && contentView.getMeasuredHeight() <= getScrollY() + getHeight()) {
            if (onBoardReachedListener != null) {
                onBoardReachedListener.onBottomReached();
            }
            isScrolledToBottom = true;
            isScrolledToTop = false;
        } else if (getScrollY() == 0) {
            if (onBoardReachedListener != null) {
                onBoardReachedListener.onTopReached();
            }
            isScrolledToTop = true;
            isScrolledToBottom = false;
        } else {
            isScrolledToBottom = false;
            isScrolledToTop = false;
        }
    }

    /**
     * get whether the scrollview is scrolled to the top
     *
     * @return true if the scrollview is scrolled to the top
     */
    public boolean isScrolledToTop() {
        return isScrolledToTop;
    }

    /**
     * get whether the scrollview is scrolled to the bottom
     *
     * @return true if the scrollview is scrolled to the bottom
     */
    public boolean isScrolledToBottom() {
        return isScrolledToBottom;
    }

    /**
     * callback interface to notify the scrollview's positionPair-status.
     */
    public interface OnBoardReachedListener {

        /**
         * invoked when the scrollview scroll to the top
         */
        void onTopReached();

        /**
         * invoked when the scrollview scroll to the bottom
         */
        void onBottomReached();
    }

    public interface OnScrollChangeListener {
        void onScrollChange(int offsetX, int offsetY);
    }

    public interface OnPinnedListener {
        void onPinned(boolean pinned);
    }

    public interface OnScrollStateChangeListener {
        void onScrollStateChanged(View view, int state);
    }

    private final Runnable invalidateRunnable = new Runnable() {

        @Override
        public void run() {
            if (currentlyStickingView != null) {
                int l = getLeftForViewRelativeOnlyChild(currentlyStickingView);
                int t = getBottomForViewRelativeOnlyChild(currentlyStickingView);
                int r = getRightForViewRelativeOnlyChild(currentlyStickingView);
                int b = (int) (getScrollY() + (currentlyStickingView.getHeight() + stickyViewTopOffset));
                invalidate(l, t, r, b);
            }
            postDelayed(this, 16);
        }
    };

    public void setup() {
        stickyViews = new ArrayList<View>();
    }

    private int getLeftForViewRelativeOnlyChild(View v) {
        int left = v.getLeft();
        while (v.getParent() != getChildAt(0)) {
            v = (View) v.getParent();
            left += v.getLeft();
        }
        return left;
    }

    private int getTopForViewRelativeOnlyChild(View v) {
        int top = v.getTop();
        while (v.getParent() != getChildAt(0)) {
            v = (View) v.getParent();
            top += v.getTop();
        }
        return top;
    }

    private int getRightForViewRelativeOnlyChild(View v) {
        int right = v.getRight();
        while (v.getParent() != getChildAt(0)) {
            v = (View) v.getParent();
            right += v.getRight();
        }
        return right;
    }

    private int getBottomForViewRelativeOnlyChild(View v) {
        int bottom = v.getBottom();
        while (v.getParent() != getChildAt(0)) {
            v = (View) v.getParent();
            bottom += v.getBottom();
        }
        return bottom;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (!clipToPaddingHasBeenSet) {
            clippingToPadding = true;
        }
        notifyHierarchyChanged();
    }

    @Override
    public void setClipToPadding(boolean clipToPadding) {
        super.setClipToPadding(clipToPadding);
        clippingToPadding = clipToPadding;
        clipToPaddingHasBeenSet = true;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (currentlyStickingView != null) {
            canvas.save();
            canvas.translate(getPaddingLeft() + stickyViewLeftOffset, getScrollY() + stickyViewTopOffset + (clippingToPadding ? getPaddingTop() : 0));
            canvas.clipRect(0, (clippingToPadding ? -stickyViewTopOffset : 0), getWidth() - stickyViewLeftOffset, currentlyStickingView.getHeight());
            showView(currentlyStickingView);
            currentlyStickingView.draw(canvas);
            hideView(currentlyStickingView);
            canvas.restore();
        }
    }

    private void doTheStickyThing() {
        View viewThatShouldStick = null;
        View approachingView = null;
        for (View v : stickyViews) {
            int viewTop = getTopForViewRelativeOnlyChild(v) - getScrollY() + (clippingToPadding ? 0 : getPaddingTop());
            if (viewTop <= 0) {
                if (viewThatShouldStick == null || viewTop > (getTopForViewRelativeOnlyChild(viewThatShouldStick) - getScrollY() + (clippingToPadding ? 0 : getPaddingTop()))) {
                    viewThatShouldStick = v;
                }
            } else {
                if (approachingView == null || viewTop < (getTopForViewRelativeOnlyChild(approachingView) - getScrollY() + (clippingToPadding ? 0 : getPaddingTop()))) {
                    approachingView = v;
                }
            }
        }
        if (viewThatShouldStick != null) {
            stickyViewTopOffset = approachingView == null ? 0 : Math.min(0, getTopForViewRelativeOnlyChild(approachingView) - getScrollY() + (clippingToPadding ? 0 : getPaddingTop()) - viewThatShouldStick.getHeight());
            if (viewThatShouldStick != currentlyStickingView) {
                if (currentlyStickingView != null) {
                    stopStickingCurrentlyStickingView();
                }
                // only compute the left offset when we start sticking.
                stickyViewLeftOffset = getLeftForViewRelativeOnlyChild(viewThatShouldStick);
                startStickingView(viewThatShouldStick);
            }
        } else if (currentlyStickingView != null) {
            stopStickingCurrentlyStickingView();
        }
    }

    private void startStickingView(View viewThatShouldStick) {
        Log.d(TAG, "startStickingView");
        currentlyStickingView = viewThatShouldStick;
        hideView(currentlyStickingView);
        post(invalidateRunnable);
        if (onPinnedListener != null) {
            onPinnedListener.onPinned(true);
        }
        pinned = true;
    }

    private void stopStickingCurrentlyStickingView() {
        Log.d(TAG, "stopStickingCurrentlyStickingView");
        showView(currentlyStickingView);
        currentlyStickingView = null;
        removeCallbacks(invalidateRunnable);
        if (onPinnedListener != null) {
            onPinnedListener.onPinned(false);
        }
        pinned = false;
    }

    private void notifyHierarchyChanged() {
        if (currentlyStickingView != null) {
            stopStickingCurrentlyStickingView();
        }
        stickyViews.clear();
        findStickyView(getChildAt(0));
        doTheStickyThing();
        invalidate();
    }

    private void findStickyView(View v) {
        View container = getChildAt(0);
        if (container instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                String tag = getStringTagForView(vg.getChildAt(i));
                if (tag != null && tag.contains(STICKY_TAG)) {
                    stickyViews.add(vg.getChildAt(i));
                }
            }
        }
    }

    private String getStringTagForView(View v) {
        Object tagObject = v.getTag();
        return String.valueOf(tagObject);
    }

    private void hideView(View v) {
        v.setAlpha(0);
    }

    private void showView(View v) {
        v.setAlpha(1);
    }
}
