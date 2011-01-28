/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This code is base on the Android Gallery widget and was modify
 * by Neil Davies neild001 'at' gmail dot com to be a Coverflow widget
 *
 * @author Neil Davies
 */

package pl.polidea.coverflow;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Camera;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Transformation;
import android.widget.ImageView;
import android.widget.Scroller;
import android.widget.SpinnerAdapter;

/**
 * A view that shows items in a center-locked, horizontally scrolling list. In a
 * Coverflow like Style.
 * <p>
 * The default values for the Gallery assume you will be using
 * {@link android.R.styleable#Theme_galleryItemBackground} as the background for
 * each View given to the Gallery from the Adapter. If you are not doing this,
 * you may need to adjust some Gallery properties, such as the spacing.
 * <p>
 * Views given to the Gallery should use Gallery.LayoutParams as their layout
 * parameters type.
 * 
 */

public class CoverFlow extends AbstractCoverAbsSpinner implements
        GestureDetector.OnGestureListener {

    /** The Constant TAG. */
    private static final String TAG = CoverFlow.class.getSimpleName();

    /** The Constant LOCAL_LOGV. */
    private static final boolean LOCAL_LOGV = false;

    /**
     * Duration in milliseconds from the start of a scroll during which we're
     * unsure whether the user is scrolling or flinging.
     */
    private static final int SCROLL_TO_FLING_UNCERTAINTY_TIMEOUT = 250;

    /**
     * Horizontal spacing between items.
     */
    private int mSpacing = 0;

    /**
     * How long the transition animation should run when a child view changes
     * position, measured in milliseconds.
     */
    private int mAnimationDuration = 2000;

    /**
     * The alpha of items that are not selected.
     */
    private float mUnselectedAlpha;

    /**
     * Left most edge of a child seen so far during layout.
     */
    private int mLeftMost;

    /**
     * Right most edge of a child seen so far during layout.
     */
    private int mRightMost;

    /** The m gravity. */
    private int mGravity;

    /**
     * Helper for detecting touch gestures.
     */
    private final GestureDetector mGestureDetector;

    /**
     * The position of the item that received the user's down touch.
     */
    private int mDownTouchPosition;

    /**
     * The view of the item that received the user's down touch.
     */
    private View mDownTouchView;

    /**
     * Executes the delta scrolls from a fling or scroll movement.
     */
    private final FlingRunnable mFlingRunnable = new FlingRunnable();

    /**
     * Sets mSuppressSelectionChanged = false. This is used to set it to false
     * in the future. It will also trigger a selection changed.
     */
    private final Runnable mDisableSuppressSelectionChangedRunnable = new Runnable() {
        @Override
        public void run() {
            mSuppressSelectionChanged = false;
            selectionChanged();
        }
    };

    /**
     * When fling runnable runs, it resets this to false. Any method along the
     * path until the end of its run() can set this to true to abort any
     * remaining fling. For example, if we've reached either the leftmost or
     * rightmost item, we will set this to true.
     */
    private boolean mShouldStopFling;

    /**
     * The currently selected item's child.
     */
    private View mSelectedChild;

    /**
     * Whether to continuously callback on the item selected listener during a
     * fling.
     */
    private boolean mShouldCallbackDuringFling = true;

    /**
     * Whether to callback when an item that is not selected is clicked.
     */
    private boolean mShouldCallbackOnUnselectedItemClick = true;

    /**
     * If true, do not callback to item selected listener.
     */
    private boolean mSuppressSelectionChanged;

    /**
     * If true, we have received the "invoke" (center or enter buttons) key
     * down. This is checked before we action on the "invoke" key up, and is
     * subsequently cleared.
     */
    private boolean mReceivedInvokeKeyDown;

    /** The m context menu info. */
    private AdapterContextMenuInfo mContextMenuInfo;

    /**
     * If true, this onScroll is the first for this user's drag (remember, a
     * drag sends many onScrolls).
     */
    private boolean mIsFirstScroll;

    /** The maximum angle the Child ImageView will be rotated by. */
    private static final int MAX_ROTATION_ANGLE = 60;

    /** The image height. */
    private float imageHeight;

    /** The image width. */
    private float imageWidth;

    /** The reflection gap. */
    private float reflectionGap;

    /** The with reflection. */
    private boolean withReflection;

    /** The image reflection ratio. */
    private float imageReflectionRatio;

    /**
     * Gets the image height.
     * 
     * @return the image height
     */
    public float getImageHeight() {
        return imageHeight;
    }

    /**
     * Sets the image height.
     * 
     * @param imageHeight
     *            the new image height
     */
    public void setImageHeight(final float imageHeight) {
        this.imageHeight = imageHeight;
    }

    /**
     * Gets the image width.
     * 
     * @return the image width
     */
    public float getImageWidth() {
        return imageWidth;
    }

    /**
     * Sets the image width.
     * 
     * @param imageWidth
     *            the new image width
     */
    public void setImageWidth(final float imageWidth) {
        this.imageWidth = imageWidth;
    }

    /**
     * Gets the reflection gap.
     * 
     * @return the reflection gap
     */
    public float getReflectionGap() {
        return reflectionGap;
    }

    /**
     * Sets the reflection gap.
     * 
     * @param reflectionGap
     *            the new reflection gap
     */
    public void setReflectionGap(final float reflectionGap) {
        this.reflectionGap = reflectionGap;
    }

    /**
     * Checks if is with reflection.
     * 
     * @return true, if is with reflection
     */
    public boolean isWithReflection() {
        return withReflection;
    }

    /**
     * Sets the with reflection.
     * 
     * @param withReflection
     *            the new with reflection
     */
    public void setWithReflection(final boolean withReflection) {
        this.withReflection = withReflection;
    }

    /**
     * Sets the image reflection ratio.
     * 
     * @param imageReflectionRatio
     *            the new image reflection ratio
     */
    public void setImageReflectionRatio(final float imageReflectionRatio) {
        this.imageReflectionRatio = imageReflectionRatio;
    }

    /**
     * Gets the image reflection ratio.
     * 
     * @return the image reflection ratio
     */
    public float getImageReflectionRatio() {
        return imageReflectionRatio;
    }

    /** The maximum zoom on the centre Child. */
    private static int mMaxZoom = -120;

    /**
     * Instantiates a new cover flow.
     * 
     * @param context
     *            the context
     */
    public CoverFlow(final Context context) {
        this(context, null);
    }

    /**
     * Instantiates a new cover flow.
     * 
     * @param context
     *            the context
     * @param attrs
     *            the attrs
     */
    public CoverFlow(final Context context, final AttributeSet attrs) {
        this(context, attrs, android.R.attr.galleryStyle);
    }

    /**
     * Parses the attributes.
     * 
     * @param context
     *            the context
     * @param attrs
     *            the attrs
     */
    private void parseAttributes(final Context context, final AttributeSet attrs) {
        final TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.CoverFlow);
        try {
            imageWidth = a.getDimension(R.styleable.CoverFlow_imageWidth, 480);
            imageHeight = a
                    .getDimension(R.styleable.CoverFlow_imageHeight, 320);
            withReflection = a.getBoolean(R.styleable.CoverFlow_withReflection,
                    false);
            imageReflectionRatio = a.getFloat(
                    R.styleable.CoverFlow_imageReflectionRatio, 0.2f);
            reflectionGap = a.getDimension(R.styleable.CoverFlow_reflectionGap,
                    4);
            setSpacing(-15);
        } finally {
            a.recycle();
        }
    }

    /**
     * Sets the.
     * 
     * @param adapter
     *            the new adapter
     */
    @Override
    public void setAdapter(final SpinnerAdapter adapter) {
        if (!(adapter instanceof AbstractCoverFlowImageAdapter)) {
            throw new IllegalArgumentException(
                    "The adapter should derive from "
                            + AbstractCoverFlowImageAdapter.class.getName());
        }
        final AbstractCoverFlowImageAdapter coverAdapter = (AbstractCoverFlowImageAdapter) adapter;
        coverAdapter.setWidth(imageWidth);
        coverAdapter.setHeight(imageHeight);
        if (withReflection) {
            final ReflectingImageAdapter reflectAdapter = new ReflectingImageAdapter(
                    getContext(), coverAdapter);
            reflectAdapter.setReflectionGap(reflectionGap);
            reflectAdapter.setWidthRatio(imageReflectionRatio);
            reflectAdapter.setWidth(imageWidth);
            reflectAdapter.setHeight(imageHeight * (1 + imageReflectionRatio));

            super.setAdapter(reflectAdapter);
        } else {
            super.setAdapter(adapter);
        }
        requestLayout();
    }

    /**
     * Instantiates a new cover flow.
     * 
     * @param context
     *            the context
     * @param attrs
     *            the attrs
     * @param defStyle
     *            the def style
     */
    public CoverFlow(final Context context, final AttributeSet attrs,
            final int defStyle) {
        super(context, attrs, defStyle);
        parseAttributes(context, attrs);
        mGestureDetector = new GestureDetector(this);
        mGestureDetector.setIsLongpressEnabled(true);
    }

    /**
     * Whether or not to callback on any {@link #getOnItemSelectedListener()}
     * while the items are being flinged. If false, only the final selected item
     * will cause the callback. If true, all items between the first and the
     * final will cause callbacks.
     * 
     * @param shouldCallback
     *            Whether or not to callback on the listener while the items are
     *            being flinged.
     */
    public void setCallbackDuringFling(final boolean shouldCallback) {
        mShouldCallbackDuringFling = shouldCallback;
    }

    /**
     * Whether or not to callback when an item that is not selected is clicked.
     * If false, the item will become selected (and re-centered). If true, the
     * 
     * @param shouldCallback
     *            Whether or not to callback on the listener when a item that is
     *            not selected is clicked. {@link #getOnItemClickListener()}
     *            will get the callback.
     * @hide
     */
    public void setCallbackOnUnselectedItemClick(final boolean shouldCallback) {
        mShouldCallbackOnUnselectedItemClick = shouldCallback;
    }

    /**
     * Sets how long the transition animation should run when a child view
     * changes position. Only relevant if animation is turned on.
     * 
     * @param animationDurationMillis
     *            The duration of the transition, in milliseconds.
     * 
     * @attr ref android.R.styleable#Gallery_animationDuration
     */
    public void setAnimationDuration(final int animationDurationMillis) {
        mAnimationDuration = animationDurationMillis;
    }

    /**
     * Sets the spacing between items in a Gallery.
     * 
     * @param spacing
     *            The spacing in pixels between items in the Gallery
     * @attr ref android.R.styleable#Gallery_spacing
     */
    public final void setSpacing(final int spacing) {
        mSpacing = spacing;
    }

    /**
     * Sets the alpha of items that are not selected in the Gallery.
     * 
     * @param unselectedAlpha
     *            the alpha for the items that are not selected.
     * 
     * @attr ref android.R.styleable#Gallery_unselectedAlpha
     */
    public void setUnselectedAlpha(final float unselectedAlpha) {
        mUnselectedAlpha = unselectedAlpha;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * android.view.ViewGroup#getChildStaticTransformation(android.view.View,
     * android.view.animation.Transformation)
     */
    @Override
    protected boolean getChildStaticTransformation(final View child,
            final Transformation t) {

        t.clear();
        t.setAlpha(child == mSelectedChild ? 1.0f : mUnselectedAlpha);

        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.view.View#computeHorizontalScrollExtent()
     */
    @Override
    protected int computeHorizontalScrollExtent() {
        // Only 1 item is considered to be selected
        return 1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.view.View#computeHorizontalScrollOffset()
     */
    @Override
    protected int computeHorizontalScrollOffset() {
        // Current scroll position is the same as the selected position
        return getSelectedItemPosition();
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.view.View#computeHorizontalScrollRange()
     */
    @Override
    protected int computeHorizontalScrollRange() {
        // Scroll range is the same as the item count
        return getCount();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * android.view.ViewGroup#checkLayoutParams(android.view.ViewGroup.LayoutParams
     * )
     */
    @Override
    protected boolean checkLayoutParams(final ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.view.ViewGroup#generateLayoutParams(android.view.ViewGroup.
     * LayoutParams)
     */
    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(
            final ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * android.view.ViewGroup#generateLayoutParams(android.util.AttributeSet)
     */
    @Override
    public ViewGroup.LayoutParams generateLayoutParams(final AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pl.polidea.coverflow.CoverAbsSpinner#generateDefaultLayoutParams()
     */
    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        /*
         * Gallery expects Gallery.LayoutParams.
         */
        return new CoverFlow.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pl.polidea.coverflow.CoverAdapterView#onLayout(boolean, int, int,
     * int, int)
     */
    @Override
    protected void onLayout(final boolean changed, final int l, final int t,
            final int r, final int b) {
        super.onLayout(changed, l, t, r, b);

        /*
         * Remember that we are in layout to prevent more layout request from
         * being generated.
         */
        setmInLayout(true);
        layout(0, false);
        setmInLayout(false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * pl.polidea.coverflow.CoverAbsSpinner#getChildHeight(android.view.View)
     */
    @Override
    int getChildHeight(final View child) {
        return child.getMeasuredHeight();
    }

    /**
     * Tracks a motion scroll. In reality, this is used to do just about any
     * movement to items (touch scroll, arrow-key scroll, set an item as
     * selected).
     * 
     * @param deltaX
     *            Change in X from the previous event.
     */
    void trackMotionScroll(final int deltaX) {

        if (getChildCount() == 0) {
            return;
        }

        final boolean toLeft = deltaX < 0;

        final int limitedDeltaX = getLimitedMotionScrollAmount(toLeft, deltaX);
        if (limitedDeltaX != deltaX) {
            // The above call returned a limited amount, so stop any
            // scrolls/flings
            mFlingRunnable.endFling(false);
            onFinishedMovement();
        }

        offsetChildrenLeftAndRight(limitedDeltaX);

        detachOffScreenChildren(toLeft);

        if (toLeft) {
            // If moved left, there will be empty space on the right
            fillToGalleryRight();
        } else {
            // Similarly, empty space on the left
            fillToGalleryLeft();
        }

        // Clear unused views
        getmRecycler().clear();

        setSelectionToCenterChild();

        invalidate();
    }

    /**
     * Gets the limited motion scroll amount.
     * 
     * @param motionToLeft
     *            the motion to left
     * @param deltaX
     *            the delta x
     * @return the limited motion scroll amount
     */
    int getLimitedMotionScrollAmount(final boolean motionToLeft,
            final int deltaX) {
        final int extremeItemPosition = motionToLeft ? getmItemCount() - 1 : 0;
        final View extremeChild = getChildAt(extremeItemPosition
                - getmFirstPosition());

        if (extremeChild == null) {
            return deltaX;
        }

        final int extremeChildCenter = getCenterOfView(extremeChild);
        final int galleryCenter = getCenterOfGallery();

        if (motionToLeft) {
            if (extremeChildCenter <= galleryCenter) {

                // The extreme child is past his boundary point!
                return 0;
            }
        } else {
            if (extremeChildCenter >= galleryCenter) {

                // The extreme child is past his boundary point!
                return 0;
            }
        }

        final int centerDifference = galleryCenter - extremeChildCenter;

        return motionToLeft ? Math.max(centerDifference, deltaX) : Math.min(
                centerDifference, deltaX);
    }

    /**
     * Offset the horizontal location of all children of this view by the
     * specified number of pixels. Modified to also rotate and scale images
     * depending on screen position.
     * 
     * @param offset
     *            the number of pixels to offset
     */
    private void offsetChildrenLeftAndRight(final int offset) {

        ImageView child;
        final int childCount = getChildCount();
        int rotationAngle = 0;
        int childCenter;
        final int galleryCenter = getCenterOfGallery();
        float childWidth;

        for (int i = childCount - 1; i >= 0; i--) {
            child = (ImageView) getChildAt(i);
            childCenter = getCenterOfView(child);
            childWidth = child.getWidth();

            if (childCenter == galleryCenter) {
                transformImageBitmap(child, 0, false, 0);
            } else {
                rotationAngle = (int) ((galleryCenter - childCenter)
                        / childWidth * MAX_ROTATION_ANGLE);
                if (Math.abs(rotationAngle) > MAX_ROTATION_ANGLE) {
                    rotationAngle = rotationAngle < 0 ? -MAX_ROTATION_ANGLE
                            : MAX_ROTATION_ANGLE;
                }
                transformImageBitmap(child, 0, false, rotationAngle);
            }
            child.offsetLeftAndRight(offset);
        }
    }

    /**
     * Gets the center of gallery.
     * 
     * @return The center of this Gallery.
     */
    private int getCenterOfGallery() {
        return (getWidth() - getPaddingLeft() - getPaddingRight()) / 2
                + getPaddingLeft();
    }

    /**
     * Gets the center of view.
     * 
     * @param view
     *            the view
     * @return The center of the given view.
     */
    private static int getCenterOfView(final View view) {
        return view.getLeft() + view.getWidth() / 2;
    }

    /**
     * Detaches children that are off the screen (i.e.: Gallery bounds).
     * 
     * @param toLeft
     *            Whether to detach children to the left of the Gallery, or to
     *            the right.
     */
    private void detachOffScreenChildren(final boolean toLeft) {
        final int numChildren = getChildCount();
        final int firstPosition = getmFirstPosition();
        int start = 0;
        int count = 0;

        if (toLeft) {
            final int galleryLeft = getPaddingLeft();
            for (int i = 0; i < numChildren; i++) {
                final View child = getChildAt(i);
                if (child.getRight() >= galleryLeft) {
                    break;
                } else {
                    count++;
                    getmRecycler().put(firstPosition + i, child);
                }
            }
        } else {
            final int galleryRight = getWidth() - getPaddingRight();
            for (int i = numChildren - 1; i >= 0; i--) {
                final View child = getChildAt(i);
                if (child.getLeft() <= galleryRight) {
                    break;
                } else {
                    start = i;
                    count++;
                    getmRecycler().put(firstPosition + i, child);
                }
            }
        }

        detachViewsFromParent(start, count);

        if (toLeft) {
            setmFirstPosition(getmFirstPosition() + count);
        }
    }

    /**
     * Scrolls the items so that the selected item is in its 'slot' (its center
     * is the gallery's center).
     */
    private void scrollIntoSlots() {

        if (getChildCount() == 0 || mSelectedChild == null) {
            return;
        }

        final int selectedCenter = getCenterOfView(mSelectedChild);
        final int targetCenter = getCenterOfGallery();

        final int scrollAmount = targetCenter - selectedCenter;
        if (scrollAmount == 0) {
            onFinishedMovement();
        } else {
            mFlingRunnable.startUsingDistance(scrollAmount);
        }
    }

    /**
     * On finished movement.
     */
    private void onFinishedMovement() {
        if (mSuppressSelectionChanged) {
            mSuppressSelectionChanged = false;

            // We haven't been callbacking during the fling, so do it now
            super.selectionChanged();
        }
        invalidate();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pl.polidea.coverflow.CoverAdapterView#selectionChanged()
     */
    @Override
    void selectionChanged() {
        if (!mSuppressSelectionChanged) {
            super.selectionChanged();
        }
    }

    /**
     * Looks for the child that is closest to the center and sets it as the
     * selected child.
     */
    private void setSelectionToCenterChild() {

        final View selView = mSelectedChild;
        if (mSelectedChild == null) {
            return;
        }

        final int galleryCenter = getCenterOfGallery();

        // Common case where the current selected position is correct
        if (selView.getLeft() <= galleryCenter
                && selView.getRight() >= galleryCenter) {
            return;
        }

        // TODO better search
        int closestEdgeDistance = Integer.MAX_VALUE;
        int newSelectedChildIndex = 0;
        for (int i = getChildCount() - 1; i >= 0; i--) {

            final View child = getChildAt(i);

            if (child.getLeft() <= galleryCenter
                    && child.getRight() >= galleryCenter) {
                // This child is in the center
                newSelectedChildIndex = i;
                break;
            }

            final int childClosestEdgeDistance = Math.min(
                    Math.abs(child.getLeft() - galleryCenter),
                    Math.abs(child.getRight() - galleryCenter));
            if (childClosestEdgeDistance < closestEdgeDistance) {
                closestEdgeDistance = childClosestEdgeDistance;
                newSelectedChildIndex = i;
            }
        }

        final int newPos = getmFirstPosition() + newSelectedChildIndex;

        if (newPos != getmSelectedPosition()) {
            setSelectedPositionInt(newPos);
            setNextSelectedPositionInt(newPos);
            checkSelectionChanged();
        }
    }

    /**
     * Creates and positions all views for this Gallery.
     * <p>
     * We layout rarely, most of the time {@link #trackMotionScroll(int)} takes
     * care of repositioning, adding, and removing children.
     * 
     * @param delta
     *            Change in the selected position. +1 means the selection is
     *            moving to the right, so views are scrolling to the left. -1
     *            means the selection is moving to the left.
     * @param animate
     *            the animate
     */
    @Override
    void layout(final int delta, final boolean animate) {

        final int childrenLeft = getmSpinnerPadding().left;
        final int childrenWidth = getRight() - getLeft()
                - getmSpinnerPadding().left - getmSpinnerPadding().right;

        if (ismDataChanged()) {
            handleDataChanged();
        }

        // Handle an empty gallery by removing all views.
        if (getmItemCount() == 0) {
            resetList();
            return;
        }

        // Update to the new selected position.
        if (getmNextSelectedPosition() >= 0) {
            setSelectedPositionInt(getmNextSelectedPosition());
        }

        // All views go in recycler while we are in layout
        recycleAllViews();

        // Clear out old views
        // removeAllViewsInLayout();
        detachAllViewsFromParent();

        /*
         * These will be used to give initial positions to views entering the
         * gallery as we scroll
         */
        mRightMost = 0;
        mLeftMost = 0;

        // Make selected view and center it

        /*
         * mFirstPosition will be decreased as we add views to the left later
         * on. The 0 for x will be offset in a couple lines down.
         */
        setmFirstPosition(getmSelectedPosition());
        final View sel = makeAndAddView(getmSelectedPosition(), 0, 0, true);

        // Put the selected child in the center
        final int selectedOffset = childrenLeft + childrenWidth / 2
                - sel.getWidth() / 2;
        sel.offsetLeftAndRight(selectedOffset);

        fillToGalleryRight();
        fillToGalleryLeft();

        // Flush any cached views that did not get reused above
        getmRecycler().clear();

        invalidate();
        checkSelectionChanged();

        setmDataChanged(false);
        setmNeedSync(false);
        setNextSelectedPositionInt(getmSelectedPosition());

        updateSelectedItemMetadata();
    }

    /**
     * Fill to gallery left.
     */
    private void fillToGalleryLeft() {
        final int itemSpacing = mSpacing;
        final int galleryLeft = getPaddingLeft();

        // Set state for initial iteration
        View prevIterationView = getChildAt(0);
        int curPosition;
        int curRightEdge;

        if (prevIterationView == null) {
            // No children available!
            curPosition = 0;
            curRightEdge = getRight() - getLeft() - getPaddingRight();
            mShouldStopFling = true;
        } else {
            curPosition = getmFirstPosition() - 1;
            curRightEdge = prevIterationView.getLeft() - itemSpacing;
        }

        while (curRightEdge > galleryLeft && curPosition >= 0) {
            prevIterationView = makeAndAddView(curPosition, curPosition
                    - getmSelectedPosition(), curRightEdge, false);

            // Remember some state
            setmFirstPosition(curPosition);

            // Set state for next iteration
            curRightEdge = prevIterationView.getLeft() - itemSpacing;
            curPosition--;
        }
    }

    /**
     * Fill to gallery right.
     */
    private void fillToGalleryRight() {
        final int itemSpacing = mSpacing;
        final int galleryRight = getRight() - getLeft() - getPaddingRight();
        final int numChildren = getChildCount();
        final int numItems = getmItemCount();

        // Set state for initial iteration
        View prevIterationView = getChildAt(numChildren - 1);
        int curPosition;
        int curLeftEdge;

        if (prevIterationView == null) {
            curPosition = getmItemCount() - 1;
            setmFirstPosition(curPosition);
            curLeftEdge = getPaddingLeft();
            mShouldStopFling = true;
        } else {
            curPosition = getmFirstPosition() + numChildren;
            curLeftEdge = prevIterationView.getRight() + itemSpacing;
        }

        while (curLeftEdge < galleryRight && curPosition < numItems) {
            prevIterationView = makeAndAddView(curPosition, curPosition
                    - getmSelectedPosition(), curLeftEdge, true);

            // Set state for next iteration
            curLeftEdge = prevIterationView.getRight() + itemSpacing;
            curPosition++;
        }
    }

    /**
     * Transform the Image Bitmap by the Angle passed.
     * 
     * @param imageView
     *            ImageView the ImageView whose bitmap we want to rotate
     * @param offset
     *            Offset from the selected position
     * @param initialLayout
     *            Is this a called from an initial layout
     * @param rotationAngle
     *            the Angle by which to rotate the Bitmap
     */
    private static void transformImageBitmap(final ImageView imageView,
            final int offset, final boolean initialLayout,
            final int rotationAngle) {
        final Camera camera = new Camera();
        Matrix imageMatrix;
        int imageHeight;
        int imageWidth;
        int bitMapHeight;
        int bitMapWidth;
        float scaleWidth;
        float scaleHeight;

        imageMatrix = imageView.getImageMatrix();

        camera.translate(0.0f, 0.0f, 100.0f);

        if (initialLayout) {
            if (offset < 0) {
                camera.rotateY(rotationAngle);
            } else if (offset > 0) {
                camera.rotateY(-rotationAngle);
            } else {
                // Just zoom in a little for the central View
                camera.translate(0.0f, 0.0f, mMaxZoom);

            }
        } else {
            if (offset == 0) {
                // As the angle of the view gets less, zoom in
                final int rotation = Math.abs(rotationAngle);
                if (rotation < 30) {
                    final float zoomAmount = (float) (mMaxZoom + rotation * 1.5);
                    camera.translate(0.0f, 0.0f, zoomAmount);
                }
                camera.rotateY(rotationAngle);
            }
        }

        camera.getMatrix(imageMatrix);

        imageHeight = imageView.getLayoutParams().height;
        imageWidth = imageView.getLayoutParams().width;
        bitMapHeight = imageView.getDrawable().getIntrinsicHeight();
        bitMapWidth = imageView.getDrawable().getIntrinsicWidth();
        scaleHeight = (float) imageHeight / bitMapHeight;
        scaleWidth = (float) imageWidth / bitMapWidth;

        imageMatrix.preTranslate(-(imageWidth / 2), -(imageHeight / 2));
        imageMatrix.preScale(scaleWidth, scaleHeight);
        imageMatrix.postTranslate((imageWidth / 2.0f), (imageHeight / 2.0f));

    }

    /**
     * Obtain a view, either by pulling an existing view from the recycler or by
     * getting a new one from the adapter. If we are animating, make sure there
     * is enough information in the view's layout parameters to animate from the
     * old to new positions.
     * 
     * @param position
     *            Position in the gallery for the view to obtain
     * @param offset
     *            Offset from the selected position
     * @param x
     *            X-coordinate indicating where this view should be placed. This
     *            will either be the left or right edge of the view, depending
     *            on the fromLeft parameter
     * @param fromLeft
     *            Are we positioning views based on the left edge? (i.e.,
     *            building from left to right)?
     * @return A view that has been added to the gallery
     */
    private View makeAndAddView(final int position, final int offset,
            final int x, final boolean fromLeft) {
        Log.d(TAG, "Add view for position: " + position);
        ImageView child;

        if (!ismDataChanged()) {
            child = (ImageView) getmRecycler().get(position);

            if (child != null) {
                // Can reuse an existing view
                final int childLeft = child.getLeft();

                // Remember left and right edges of where views have been placed
                mRightMost = Math.max(mRightMost,
                        childLeft + child.getMeasuredWidth());
                mLeftMost = Math.min(mLeftMost, childLeft);

                transformImageBitmap(child, offset, true, MAX_ROTATION_ANGLE);
                // Position the view
                setUpChild(child, offset, x, fromLeft);

                return child;
            }
        }

        // Nothing found in the recycler -- ask the adapter for a view
        child = (ImageView) getmAdapter().getView(position, null, this);

        // Make sure we set anti-aliasing otherwise we get jaggies
        final BitmapDrawable drawable = (BitmapDrawable) child.getDrawable();
        drawable.setAntiAlias(true);
        transformImageBitmap(child, offset, true, MAX_ROTATION_ANGLE);
        // Position the view
        setUpChild(child, offset, x, fromLeft);

        return child;
    }

    /**
     * Helper for makeAndAddView to set the position of a view and fill out its
     * layout paramters.
     * 
     * @param child
     *            The view to position
     * @param offset
     *            Offset from the selected position
     * @param x
     *            X-coordintate indicating where this view should be placed.
     *            This will either be the left or right edge of the view,
     *            depending on the fromLeft paramter
     * @param fromLeft
     *            Are we posiitoning views based on the left edge? (i.e.,
     *            building from left to right)?
     */
    private void setUpChild(final View child, final int offset, final int x,
            final boolean fromLeft) {

        // Respect layout params that are already in the view. Otherwise
        // make some up...
        CoverFlow.LayoutParams lp = (CoverFlow.LayoutParams) child
                .getLayoutParams();
        if (lp == null) {
            lp = (CoverFlow.LayoutParams) generateDefaultLayoutParams();
        }

        addViewInLayout(child, fromLeft ? -1 : 0, lp);

        child.setSelected(offset == 0);

        // Get measure specs
        final int childHeightSpec = ViewGroup.getChildMeasureSpec(
                getmHeightMeasureSpec(), getmSpinnerPadding().top
                        + getmSpinnerPadding().bottom, lp.height);
        final int childWidthSpec = ViewGroup.getChildMeasureSpec(
                getmWidthMeasureSpec(), getmSpinnerPadding().left
                        + getmSpinnerPadding().right, lp.width);

        // Measure child
        child.measure(childWidthSpec, childHeightSpec);

        int childLeft;
        int childRight;

        // Position vertically based on gravity setting
        final int childTop = calculateTop(child, true);
        final int childBottom = childTop + child.getMeasuredHeight();

        final int width = child.getMeasuredWidth();
        if (fromLeft) {
            childLeft = x;
            childRight = childLeft + width;
        } else {
            childLeft = x - width;
            childRight = x;
        }

        child.layout(childLeft, childTop, childRight, childBottom);
    }

    /**
     * Figure out vertical placement based on mGravity.
     * 
     * @param child
     *            Child to place
     * @param duringLayout
     *            the during layout
     * @return Where the top of the child should be
     */
    private int calculateTop(final View child, final boolean duringLayout) {
        final int myHeight = duringLayout ? getMeasuredHeight() : getHeight();
        final int childHeight = duringLayout ? child.getMeasuredHeight()
                : child.getHeight();

        int childTop = 0;

        switch (mGravity) {
        case Gravity.TOP:
            childTop = getmSpinnerPadding().top;
            break;
        case Gravity.CENTER_VERTICAL:
            final int availableSpace = myHeight - getmSpinnerPadding().bottom
                    - getmSpinnerPadding().top - childHeight;
            childTop = getmSpinnerPadding().top + availableSpace / 2;
            break;
        case Gravity.BOTTOM:
            childTop = myHeight - getmSpinnerPadding().bottom - childHeight;
            break;
        default:
            // silently skip
            break;
        }
        return childTop;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.view.View#onTouchEvent(android.view.MotionEvent)
     */
    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        mGestureDetector.onTouchEvent(event);

        final int action = event.getAction();
        if (action == MotionEvent.ACTION_UP) {
            // Helper method for lifted finger
            onUp();
        } else if (action == MotionEvent.ACTION_CANCEL) {
            onCancel();
        }

        // return retValue;
        return true;
    }

    /**
     * On single tap up.
     * 
     * @param e
     *            the e
     * @return true, if successful {@inheritDoc}
     */
    @Override
    public boolean onSingleTapUp(final MotionEvent e) {

        if (mDownTouchPosition >= 0) {

            // An item tap should make it selected, so scroll to this child.
            scrollToChild(mDownTouchPosition - getmFirstPosition());

            // Also pass the click so the client knows, if it wants to.
            if (mShouldCallbackOnUnselectedItemClick
                    || mDownTouchPosition == getmSelectedPosition()) {
                performItemClick(mDownTouchView, mDownTouchPosition,
                        getmAdapter().getItemId(mDownTouchPosition));
            }

            return true;
        }

        return false;
    }

    /**
     * On fling.
     * 
     * @param e1
     *            the e1
     * @param e2
     *            the e2
     * @param velocityX
     *            the velocity x
     * @param velocityY
     *            the velocity y
     * @return true, if successful {@inheritDoc}
     */
    @Override
    public boolean onFling(final MotionEvent e1, final MotionEvent e2,
            final float velocityX, final float velocityY) {

        if (!mShouldCallbackDuringFling) {
            // We want to suppress selection changes

            // Remove any future code to set mSuppressSelectionChanged = false
            removeCallbacks(mDisableSuppressSelectionChangedRunnable);

            // This will get reset once we scroll into slots
            if (!mSuppressSelectionChanged) {
                mSuppressSelectionChanged = true;
            }
        }

        // Fling the gallery!
        mFlingRunnable.startUsingVelocity((int) -velocityX);

        return true;
    }

    /**
     * On scroll.
     * 
     * @param e1
     *            the e1
     * @param e2
     *            the e2
     * @param distanceX
     *            the distance x
     * @param distanceY
     *            the distance y
     * @return true, if successful {@inheritDoc}
     */
    @Override
    public boolean onScroll(final MotionEvent e1, final MotionEvent e2,
            final float distanceX, final float distanceY) {

        if (LOCAL_LOGV) {
            Log.v(TAG, String.valueOf(e2.getX() - e1.getX()));
        }

        /*
         * Now's a good time to tell our parent to stop intercepting our events!
         * The user has moved more than the slop amount, since GestureDetector
         * ensures this before calling this method. Also, if a parent is more
         * interested in this touch's events than we are, it would have
         * intercepted them by now (for example, we can assume when a Gallery is
         * in the ListView, a vertical scroll would not end up in this method
         * since a ListView would have intercepted it by now).
         */
        getParent().requestDisallowInterceptTouchEvent(true);

        // As the user scrolls, we want to callback selection changes so
        // related-
        // info on the screen is up-to-date with the gallery's selection
        if (mShouldCallbackDuringFling) {
            if (mSuppressSelectionChanged) {
                mSuppressSelectionChanged = false;
            }
        } else {
            if (mIsFirstScroll) {
                /*
                 * We're not notifying the client of selection changes during
                 * the fling, and this scroll could possibly be a fling. Don't
                 * do selection changes until we're sure it is not a fling.
                 */
                if (!mSuppressSelectionChanged) {
                    mSuppressSelectionChanged = true;
                }
                postDelayed(mDisableSuppressSelectionChangedRunnable,
                        SCROLL_TO_FLING_UNCERTAINTY_TIMEOUT);
            }
        }

        // Track the motion
        trackMotionScroll(-1 * (int) distanceX);

        mIsFirstScroll = false;
        return true;
    }

    /**
     * On down.
     * 
     * @param e
     *            the e
     * @return true, if successful {@inheritDoc}
     */
    @Override
    public boolean onDown(final MotionEvent e) {

        // Kill any existing fling/scroll
        mFlingRunnable.stop(false);

        // Get the item's view that was touched
        mDownTouchPosition = pointToPosition((int) e.getX(), (int) e.getY());

        if (mDownTouchPosition >= 0) {
            mDownTouchView = getChildAt(mDownTouchPosition
                    - getmFirstPosition());
            mDownTouchView.setPressed(true);
        }

        // Reset the multiple-scroll tracking state
        mIsFirstScroll = true;

        // Must return true to get matching events for this down event.
        return true;
    }

    /**
     * Called when a touch event's action is MotionEvent.ACTION_UP.
     */
    void onUp() {

        if (mFlingRunnable.mScroller.isFinished()) {
            scrollIntoSlots();
        }

        dispatchUnpress();
    }

    /**
     * Called when a touch event's action is MotionEvent.ACTION_CANCEL.
     */
    void onCancel() {
        onUp();
    }

    /**
     * On long press.
     * 
     * @param e
     *            the e {@inheritDoc}
     */
    @Override
    public void onLongPress(final MotionEvent e) {

        if (mDownTouchPosition < 0) {
            return;
        }

        // performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        final long id = getItemIdAtPosition(mDownTouchPosition);
        dispatchLongPress(mDownTouchView, mDownTouchPosition, id);
    }

    /**
     * On show press.
     * 
     * @param e
     *            the e {@inheritDoc}
     */
    @Override
    public void onShowPress(final MotionEvent e) {
        // unused
    }

    /**
     * Dispatch press.
     * 
     * @param child
     *            the child
     */
    private void dispatchPress(final View child) {

        if (child != null) {
            child.setPressed(true);
        }

        setPressed(true);
    }

    /**
     * Dispatch unpress.
     */
    private void dispatchUnpress() {

        for (int i = getChildCount() - 1; i >= 0; i--) {
            getChildAt(i).setPressed(false);
        }

        setPressed(false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.view.ViewGroup#dispatchSetSelected(boolean)
     */
    @Override
    public void dispatchSetSelected(final boolean selected) {
        /*
         * We don't want to pass the selected state given from its parent to its
         * children since this widget itself has a selected state to give to its
         * children.
         */
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.view.ViewGroup#dispatchSetPressed(boolean)
     */
    @Override
    protected void dispatchSetPressed(final boolean pressed) {

        // Show the pressed state on the selected child
        if (mSelectedChild != null) {
            mSelectedChild.setPressed(pressed);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.view.View#getContextMenuInfo()
     */
    @Override
    protected ContextMenuInfo getContextMenuInfo() {
        return mContextMenuInfo;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.view.ViewGroup#showContextMenuForChild(android.view.View)
     */
    @Override
    public boolean showContextMenuForChild(final View originalView) {

        final int longPressPosition = getPositionForView(originalView);
        if (longPressPosition < 0) {
            return false;
        }

        final long longPressId = getmAdapter().getItemId(longPressPosition);
        return dispatchLongPress(originalView, longPressPosition, longPressId);
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.view.View#showContextMenu()
     */
    @Override
    public boolean showContextMenu() {

        if (isPressed() && getmSelectedPosition() >= 0) {
            final int index = getmSelectedPosition() - getmFirstPosition();
            final View v = getChildAt(index);
            return dispatchLongPress(v, getmSelectedPosition(),
                    getmSelectedRowId());
        }

        return false;
    }

    /**
     * Dispatch long press.
     * 
     * @param view
     *            the view
     * @param position
     *            the position
     * @param id
     *            the id
     * @return true, if successful
     */
    private boolean dispatchLongPress(final View view, final int position,
            final long id) {
        boolean handled = false;

        if (getmOnItemLongClickListener() != null) {
            handled = getmOnItemLongClickListener().onItemLongClick(this,
                    mDownTouchView, mDownTouchPosition, id);
        }

        if (!handled) {
            mContextMenuInfo = new AdapterContextMenuInfo(view, position, id);
            handled = super.showContextMenuForChild(this);
        }
        return handled;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.view.ViewGroup#dispatchKeyEvent(android.view.KeyEvent)
     */
    @Override
    public boolean dispatchKeyEvent(final KeyEvent event) {
        // Gallery steals all key events
        return event.dispatch(this);
    }

    /**
     * Handles left, right, and clicking.
     * 
     * @param keyCode
     *            the key code
     * @param event
     *            the event
     * @return true, if successful
     * @see android.view.View#onKeyDown
     */
    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        switch (keyCode) {

        case KeyEvent.KEYCODE_DPAD_LEFT:
            if (movePrevious()) {
                playSoundEffect(SoundEffectConstants.NAVIGATION_LEFT);
            }
            return true;

        case KeyEvent.KEYCODE_DPAD_RIGHT:
            if (moveNext()) {
                playSoundEffect(SoundEffectConstants.NAVIGATION_RIGHT);
            }
            return true;

        case KeyEvent.KEYCODE_DPAD_CENTER:
        case KeyEvent.KEYCODE_ENTER:
            mReceivedInvokeKeyDown = true;
            // fallthrough to default handling
        default:
            // silently skip
            break;
        }

        return super.onKeyDown(keyCode, event);
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.view.View#onKeyUp(int, android.view.KeyEvent)
     */
    @Override
    public boolean onKeyUp(final int keyCode, final KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_DPAD_CENTER:
        case KeyEvent.KEYCODE_ENTER:

            if (mReceivedInvokeKeyDown && getmItemCount() > 0) {

                dispatchPress(mSelectedChild);
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        dispatchUnpress();
                    }
                }, ViewConfiguration.getPressedStateDuration());

                final int selectedIndex = getmSelectedPosition()
                        - getmFirstPosition();
                performItemClick(getChildAt(selectedIndex),
                        getmSelectedPosition(),
                        getmAdapter().getItemId(getmSelectedPosition()));
            }

            // Clear the flag
            mReceivedInvokeKeyDown = false;

            return true;

        default:
            // silently skip
            break;
        }

        return super.onKeyUp(keyCode, event);
    }

    /**
     * Move previous.
     * 
     * @return true, if successful
     */
    boolean movePrevious() {
        if (getmItemCount() > 0 && getmSelectedPosition() > 0) {
            scrollToChild(getmSelectedPosition() - getmFirstPosition() - 1);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Move next.
     * 
     * @return true, if successful
     */
    boolean moveNext() {
        if (getmItemCount() > 0 && getmSelectedPosition() < getmItemCount() - 1) {
            scrollToChild(getmSelectedPosition() - getmFirstPosition() + 1);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Scroll to child.
     * 
     * @param childPosition
     *            the child position
     * @return true, if successful
     */
    private boolean scrollToChild(final int childPosition) {
        final View child = getChildAt(childPosition);

        if (child != null) {
            final int distance = getCenterOfGallery() - getCenterOfView(child);
            mFlingRunnable.startUsingDistance(distance);
            return true;
        }

        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pl.polidea.coverflow.CoverAdapterView#setSelectedPositionInt(int)
     */
    @Override
    void setSelectedPositionInt(final int position) {
        super.setSelectedPositionInt(position);

        // Updates any metadata we keep about the selected item.
        updateSelectedItemMetadata();
    }

    /**
     * Update selected item metadata.
     */
    private void updateSelectedItemMetadata() {

        final View oldSelectedChild = mSelectedChild;
        mSelectedChild = getChildAt(getmSelectedPosition()
                - getmFirstPosition());
        final View child = mSelectedChild;
        if (child == null) {
            return;
        }

        child.setSelected(true);
        child.setFocusable(true);

        if (hasFocus()) {
            child.requestFocus();
        }

        // We unfocus the old child down here so the above hasFocus check
        // returns true
        if (oldSelectedChild != null) {

            // Make sure its drawable state doesn't contain 'selected'
            oldSelectedChild.setSelected(false);

            // Make sure it is not focusable anymore, since otherwise arrow keys
            // can make this one be focused
            oldSelectedChild.setFocusable(false);
        }

    }

    /**
     * Describes how the child views are aligned.
     * 
     * @param gravity
     *            the new gravity
     * @attr ref android.R.styleable#Gallery_gravity
     */
    public void setGravity(final int gravity) {
        if (mGravity != gravity) {
            mGravity = gravity;
            requestLayout();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.view.ViewGroup#getChildDrawingOrder(int, int)
     */
    @Override
    protected int getChildDrawingOrder(final int childCount, final int i) {
        final int selectedIndex = getmSelectedPosition() - getmFirstPosition();

        // Just to be safe
        if (selectedIndex < 0) {
            return i;
        }

        if (i == childCount - 1) {
            // Draw the selected child last
            return selectedIndex;
        } else if (i >= selectedIndex) {
            // Move the children to the right of the selected child earlier one
            return i + 1;
        } else {
            // Keep the children to the left of the selected child the same
            return i;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.view.View#onFocusChanged(boolean, int,
     * android.graphics.Rect)
     */
    @Override
    protected void onFocusChanged(final boolean gainFocus, final int direction,
            final Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);

        /*
         * The gallery shows focus by focusing the selected item. So, give focus
         * to our selected item instead. We steal keys from our selected item
         * elsewhere.
         */
        if (gainFocus && mSelectedChild != null) {
            mSelectedChild.requestFocus(direction);
        }

    }

    /**
     * Responsible for fling behavior. Use {@link #startUsingVelocity(int)} to
     * initiate a fling. Each frame of the fling is handled in {@link #run()}. A
     * FlingRunnable will keep re-posting itself until the fling is done.
     * 
     */
    private class FlingRunnable implements Runnable {

        /** Tracks the decay of a fling scroll. */
        private final Scroller mScroller;

        /** X value reported by mScroller on the previous fling. */
        private int mLastFlingX;

        /**
         * Instantiates a new fling runnable.
         */
        public FlingRunnable() {
            mScroller = new Scroller(getContext());
        }

        /**
         * Start common.
         */
        private void startCommon() {
            // Remove any pending flings
            removeCallbacks(this);
        }

        /**
         * Start using velocity.
         * 
         * @param initialVelocity
         *            the initial velocity
         */
        public void startUsingVelocity(final int initialVelocity) {
            if (initialVelocity == 0) {
                return;
            }

            startCommon();

            final int initialX = initialVelocity < 0 ? Integer.MAX_VALUE : 0;
            mLastFlingX = initialX;
            mScroller.fling(initialX, 0, initialVelocity, 0, 0,
                    Integer.MAX_VALUE, 0, Integer.MAX_VALUE);
            post(this);
        }

        /**
         * Start using distance.
         * 
         * @param distance
         *            the distance
         */
        public void startUsingDistance(final int distance) {
            if (distance == 0) {
                return;
            }

            startCommon();

            mLastFlingX = 0;
            mScroller.startScroll(0, 0, -distance, 0, mAnimationDuration);
            post(this);
        }

        /**
         * Stop.
         * 
         * @param scrollIntoSlots
         *            the scroll into slots
         */
        public void stop(final boolean scrollIntoSlots) {
            removeCallbacks(this);
            endFling(scrollIntoSlots);
        }

        /**
         * End fling.
         * 
         * @param scrollIntoSlots
         *            the scroll into slots
         */
        private void endFling(final boolean scrollIntoSlots) {
            /*
             * Force the scroller's status to finished (without setting its
             * position to the end)
             */
            mScroller.forceFinished(true);

            if (scrollIntoSlots) {
                scrollIntoSlots();
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {

            if (getmItemCount() == 0) {
                endFling(true);
                return;
            }

            mShouldStopFling = false;

            final Scroller scroller = mScroller;
            final boolean more = scroller.computeScrollOffset();
            final int x = scroller.getCurrX();

            // Flip sign to convert finger direction to list items direction
            // (e.g. finger moving down means list is moving towards the top)
            int delta = mLastFlingX - x;

            // Pretend that each frame of a fling scroll is a touch scroll
            if (delta > 0) {
                // Moving towards the left. Use first view as mDownTouchPosition
                mDownTouchPosition = getmFirstPosition();

                // Don't fling more than 1 screen
                delta = Math.min(getWidth() - getPaddingLeft()
                        - getPaddingRight() - 1, delta);
            } else {
                // Moving towards the right. Use last view as mDownTouchPosition
                final int offsetToLast = getChildCount() - 1;
                mDownTouchPosition = getmFirstPosition() + offsetToLast;

                // Don't fling more than 1 screen
                delta = Math.max(-(getWidth() - getPaddingRight()
                        - getPaddingLeft() - 1), delta);
            }

            trackMotionScroll(delta);

            if (more && !mShouldStopFling) {
                mLastFlingX = x;
                post(this);
            } else {
                endFling(true);
            }
        }

    }

    /**
     * Gallery extends LayoutParams to provide a place to hold current
     * Transformation information along with previous position/transformation
     * info.
     * 
     */
    public static class LayoutParams extends ViewGroup.LayoutParams {

        /**
         * Instantiates a new layout params.
         * 
         * @param c
         *            the c
         * @param attrs
         *            the attrs
         */
        public LayoutParams(final Context c, final AttributeSet attrs) {
            super(c, attrs);
        }

        /**
         * Instantiates a new layout params.
         * 
         * @param w
         *            the w
         * @param h
         *            the h
         */
        public LayoutParams(final int w, final int h) {
            super(w, h);
        }

        /**
         * Instantiates a new layout params.
         * 
         * @param source
         *            the source
         */
        public LayoutParams(final ViewGroup.LayoutParams source) {
            super(source);
        }
    }
}