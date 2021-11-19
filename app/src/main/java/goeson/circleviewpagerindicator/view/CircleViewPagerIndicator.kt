/*
 * Copyright (C) 2011 Patrik Akerfeldt
 * Copyright (C) 2011 Jake Wharton
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
 */
package goeson.circleviewpagerindicator.view

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import android.graphics.RectF
import android.view.MotionEvent
import android.os.Parcelable
import android.os.Parcel
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.AccelerateDecelerateInterpolator
import goeson.circleviewpagerindicator.R
import kotlin.math.abs
import kotlin.math.min

class CircleViewPagerIndicator(
    context: Context,
    attrs: AttributeSet
) : View(context, attrs), PageIndicator {

    // Draw
    private val paintPageFill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintStroke = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintFill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rectFill = RectF()

    // Pager
    private var viewPager: ViewPager? = null
    private var listener: OnPageChangeListener? = null
    private var currentPage = 0
    private var prevPage = -1

    // Touch
    private val touchSlop: Int = ViewConfiguration.get(context).scaledTouchSlop
    private var lastMotionX = -1f
    private var activePointerId = INVALID_POINTER
    private var isDragging = false

    // animation
    private var animator: ObjectAnimator? = null
    private var animProgress: Float = 1f

    // Attrs
    private var mSnap: Boolean
    private var mCentered: Boolean
    private var mFillExtendWidth: Float
    private var mRadius: Float
    private var mPageSpace: Float

    var pageColor: Int
        get() = paintPageFill.color
        set(pageColor) {
            paintPageFill.color = pageColor
            invalidate()
        }

    var fillColor: Int
        get() = paintFill.color
        set(fillColor) {
            paintFill.color = fillColor
            invalidate()
        }

    var strokeColor: Int
        get() = paintStroke.color
        set(strokeColor) {
            paintStroke.color = strokeColor
            invalidate()
        }

    var strokeWidth: Float
        get() = paintStroke.strokeWidth
        set(strokeWidth) {
            paintStroke.strokeWidth = strokeWidth
            invalidate()
        }

    var radius: Float
        get() = mRadius
        set(value) {
            mRadius = value
            invalidate()
        }

    var pageSpace: Float
        get() = mPageSpace
        set(value) {
            mPageSpace = value
            invalidate()
        }

    var isSnap: Boolean
        get() = mSnap
        set(value) {
            mSnap = value
            invalidate()
        }

    var isCentered: Boolean
        get() = mCentered
        set(value) {
            mCentered = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        viewPager ?: return

        val count = viewPager!!.adapter!!.count
        if (count == 0) {
            return
        }

        if (currentPage >= count) {
            setCurrentItem(count - 1)
            return
        }

        val twoRadiusWithPageSpace = (mRadius * 2) + mPageSpace

        val leftOffset = if (mCentered) {
            (paddingLeft + mRadius) + (width - paddingLeft - paddingRight) * 0.5f - (count * twoRadiusWithPageSpace) * 0.5f - (mFillExtendWidth) * 0.5f
        } else {
            (paddingLeft + mRadius)
        }

        val topOffset = paddingTop + mRadius

        val pageFillRadius = if (paintStroke.strokeWidth > 0) {
            mRadius - paintStroke.strokeWidth * 0.5f
        } else {
            mRadius
        }

        val pageMoveDirection = if (currentPage > prevPage) {
            1
        } else {
            -1
        }

        for (iLoop in 0 until count) {

            val cX = leftOffset + (iLoop * twoRadiusWithPageSpace)

            if (iLoop == currentPage) {

                val offsetX = if (mSnap) {
                    mFillExtendWidth
                } else {
                    mFillExtendWidth * animProgress
                }

                rectFill.set(cX - mRadius, topOffset - mRadius, cX + mRadius, topOffset + mRadius)

                if (pageMoveDirection < 0) {
                    rectFill.right += offsetX
                } else {
                    rectFill.left += (mFillExtendWidth - offsetX)
                    rectFill.right += mFillExtendWidth
                }

                if (paintFill.alpha > 0) {
                    canvas.drawRoundRect(rectFill, mRadius, mRadius, paintFill)
                }

                if (pageFillRadius != mRadius) {
                    canvas.drawRoundRect(rectFill, mRadius, mRadius, paintStroke)
                }
            }
            else if (iLoop == prevPage) {

                val dX = if (iLoop <= currentPage) {
                    cX
                } else {
                    cX + mFillExtendWidth
                }

                val offsetX = if (mSnap) {
                    mFillExtendWidth
                } else {
                    mFillExtendWidth * animProgress
                }

                rectFill.set(dX - mRadius, topOffset - mRadius, dX + mRadius, topOffset + mRadius)

                if (pageMoveDirection > 0) {
                    rectFill.right += (mFillExtendWidth - offsetX)
                } else {
                    rectFill.left += (-mFillExtendWidth + offsetX)
                }

                if (paintPageFill.alpha > 0) {
                    canvas.drawRoundRect(rectFill, mRadius, mRadius, paintPageFill)
                }

                if (pageFillRadius != mRadius) {
                    canvas.drawRoundRect(rectFill, mRadius, mRadius, paintStroke)
                }
            }
            else {
                val dX = if (mSnap) {
                    if (iLoop <= currentPage) {
                        cX
                    } else {
                        cX + mFillExtendWidth
                    }
                } else {
                    if (pageMoveDirection > 0 && iLoop in (prevPage + 1) until currentPage) {
                        cX + (mFillExtendWidth * (1f - animProgress))
                    } else if (pageMoveDirection < 0 && iLoop in (currentPage + 1) until prevPage) {
                        cX + (mFillExtendWidth * animProgress)
                    } else {
                        if (iLoop <= currentPage) {
                            cX
                        } else {
                            cX + mFillExtendWidth
                        }
                    }
                }

                if (paintPageFill.alpha > 0) {
                    canvas.drawCircle(dX, topOffset, pageFillRadius, paintPageFill)
                }

                if (pageFillRadius != mRadius) {
                    canvas.drawCircle(dX, topOffset, mRadius, paintStroke)
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (super.onTouchEvent(ev)) {
            return true
        }

        if (viewPager == null || viewPager!!.adapter!!.count == 0) {
            return false
        }

        when (val action = ev.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = ev.getPointerId(0)
                lastMotionX = ev.x
            }
            MotionEvent.ACTION_MOVE -> {
                val activePointerIndex = ev.findPointerIndex(activePointerId)
                val x = ev.getX(activePointerIndex)
                val deltaX = x - lastMotionX

                if (!isDragging) {
                    if (abs(deltaX) > touchSlop) {
                        isDragging = true
                    }
                }

                if (isDragging) {
                    lastMotionX = x
                    if (viewPager!!.isFakeDragging || viewPager!!.beginFakeDrag()) {
                        viewPager!!.fakeDragBy(deltaX)
                    }
                }
            }
            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    val count = viewPager!!.adapter!!.count
                    val width = width
                    val halfWidth = width / 2f
                    val sixthWidth = width / 6f

                    if (currentPage > 0 && ev.x < halfWidth - sixthWidth) {
                        if (action != MotionEvent.ACTION_CANCEL) {
                            viewPager!!.currentItem = currentPage - 1
                        }
                        return true
                    } else if (currentPage < count - 1 && ev.x > halfWidth + sixthWidth) {
                        if (action != MotionEvent.ACTION_CANCEL) {
                            viewPager!!.currentItem = currentPage + 1
                        }
                        return true
                    }
                }

                isDragging = false
                activePointerId = INVALID_POINTER

                if (viewPager!!.isFakeDragging) {
                    viewPager!!.endFakeDrag()
                }
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                val index = ev.actionIndex
                lastMotionX = ev.getX(index)
                activePointerId = ev.getPointerId(index)
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = ev.actionIndex
                val pointerId = ev.getPointerId(pointerIndex)

                if (pointerId == activePointerId) {
                    val newPointerIndex = if (pointerIndex == 0) 1 else 0
                    activePointerId = ev.getPointerId(newPointerIndex)
                }

                lastMotionX = ev.getX(ev.findPointerIndex(activePointerId))
            }
        }
        return true
    }

    override fun setViewPager(view: ViewPager) {
        if (viewPager === view) {
            return
        }

        viewPager?.setOnPageChangeListener(null)

        checkNotNull(view.adapter) { "ViewPager does not have adapter instance." }

        viewPager = view
        viewPager!!.setOnPageChangeListener(this)
        invalidate()
    }

    override fun setViewPager(view: ViewPager, initialPosition: Int) {
        setViewPager(view)
        setCurrentItem(initialPosition)
    }

    override fun setCurrentItem(item: Int) {
        checkNotNull(viewPager) { "ViewPager has not been bound." }

        viewPager!!.currentItem = item
        if (item != currentPage) {
            prevPage = currentPage
            currentPage = item
        }
        invalidate()
    }

    override fun notifyDataSetChanged() {
        invalidate()
    }

    override fun onPageScrollStateChanged(state: Int) {
        listener?.onPageScrollStateChanged(state)
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        listener?.onPageScrolled(position, positionOffset, positionOffsetPixels)
    }

    override fun onPageSelected(position: Int) {
        prevPage = currentPage
        currentPage = position

        if (mSnap) {
            invalidate()
        } else {
            cancelAnimation()
            startAnimation()
        }

        listener?.onPageSelected(position)
    }

    private fun startAnimation() {
        animator = ObjectAnimator.ofFloat(this, "animProgress", 0f, 1f).apply {
            interpolator = AccelerateDecelerateInterpolator()
            duration = 240
            start()
        }
    }

    private fun cancelAnimation() {
        animator?.cancel()
        animator = null
    }

    private fun setAnimProgress(progress: Float) {
        animProgress = progress
        invalidate()
    }

    override fun setOnPageChangeListener(listener: OnPageChangeListener?) {
        this.listener = listener
    }

    /*
     * (non-Javadoc)
     *
     * @see android.view.View#onMeasure(int, int)
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(measureLong(widthMeasureSpec), measureShort(heightMeasureSpec))
    }

    /**
     * Determines the width of this view
     *
     * @param measureSpec
     * A measureSpec packed into an int
     * @return The width of the view, honoring constraints from measureSpec
     */
    private fun measureLong(measureSpec: Int): Int {
        var result: Int
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)

        if (specMode == MeasureSpec.EXACTLY || viewPager == null) {
            //We were told how big to be
            result = specSize
        } else {
            //Calculate the width according the views count
            val count = viewPager!!.adapter!!.count

            result =
                (paddingLeft + paddingRight + (mRadius * 2 * count) + (count - 1) * mPageSpace + 1 + mFillExtendWidth).toInt()

            //Respect AT_MOST value if that was what is called for by measureSpec
            if (specMode == MeasureSpec.AT_MOST) {
                result = min(result, specSize)
            }
        }

        return result
    }

    /**
     * Determines the height of this view
     *
     * @param measureSpec
     * A measureSpec packed into an int
     * @return The height of the view, honoring constraints from measureSpec
     */
    private fun measureShort(measureSpec: Int): Int {
        var result: Int
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)

        if (specMode == MeasureSpec.EXACTLY) {
            //We were told how big to be
            result = specSize
        } else {
            //Measure the height
            result = (2 * mRadius + paddingTop + paddingBottom + 1).toInt()
            //Respect AT_MOST value if that was what is called for by measureSpec
            if (specMode == MeasureSpec.AT_MOST) {
                result = min(result, specSize)
            }
        }

        return result
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelAnimation()
    }

    public override fun onRestoreInstanceState(state: Parcelable) {
        val savedState = state as SavedState
        super.onRestoreInstanceState(savedState.superState)

        currentPage = savedState.currentPage
        requestLayout()
    }

    public override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val savedState = SavedState(superState)
        savedState.currentPage = currentPage
        return savedState
    }

    internal class SavedState : BaseSavedState {
        var currentPage = 0

        constructor(superState: Parcelable?) : super(superState)

        private constructor(`in`: Parcel) : super(`in`) {
            currentPage = `in`.readInt()
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeInt(currentPage)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(`in`: Parcel): SavedState {
                    return SavedState(`in`)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    companion object {
        private const val INVALID_POINTER = -1

        private const val DEFAULT_PAGE_FILL_COLOR = "#00000000"
        private const val DEFAULT_STROKE_COLOR = "#FFDDDDDD"
        private const val DEFAULT_FILL_COLOR = "#FFFFFFFF"
        private const val DEFAULT_FILL_EXTEND_WIDTH = 0f
        private const val DEFAULT_RADIUS_DP = 3
        private const val DEFAULT_STROKE_DP = 1
        private const val DEFAULT_CENTERED = true
        private const val DEFAULT_SNAP = false
    }

    init {
        //Retrieve styles attributes
        val a = context.obtainStyledAttributes(attrs, R.styleable.CircleViewPagerIndicator)

        mRadius = a.getDimension(
            R.styleable.CircleViewPagerIndicator_radius,
            DEFAULT_RADIUS_DP * resources.displayMetrics.density
        )
        mPageSpace = a.getDimension(R.styleable.CircleViewPagerIndicator_pageSpace, mRadius)
        mSnap = a.getBoolean(R.styleable.CircleViewPagerIndicator_snap, DEFAULT_SNAP)
        mCentered = a.getBoolean(R.styleable.CircleViewPagerIndicator_centered, DEFAULT_CENTERED)

        mFillExtendWidth =
            a.getDimension(R.styleable.CircleViewPagerIndicator_fillExtendWidth, DEFAULT_FILL_EXTEND_WIDTH)

        with(paintPageFill) {
            style = Paint.Style.FILL
            color = a.getColor(
                R.styleable.CircleViewPagerIndicator_pageColor,
                Color.parseColor(DEFAULT_PAGE_FILL_COLOR)
            )
        }

        with(paintStroke) {
            style = Paint.Style.STROKE
            color = a.getColor(
                R.styleable.CircleViewPagerIndicator_strokeColor,
                Color.parseColor(DEFAULT_STROKE_COLOR)
            )
            strokeWidth = a.getDimension(
                R.styleable.CircleViewPagerIndicator_strokeWidth,
                DEFAULT_STROKE_DP * context.resources.displayMetrics.density
            )
        }

        with(paintFill) {
            style = Paint.Style.FILL
            color = a.getColor(
                R.styleable.CircleViewPagerIndicator_fillColor,
                Color.parseColor(DEFAULT_FILL_COLOR)
            )
        }

        a.recycle()
    }
}

/**
 * A PageIndicator is responsible to show an visual indicator on the total views
 * number and the current visible view.
 */
interface PageIndicator : OnPageChangeListener {
    /**
     * Bind the indicator to a ViewPager.
     *
     * @param view
     */
    fun setViewPager(view: ViewPager)

    /**
     * Bind the indicator to a ViewPager.
     *
     * @param view
     * @param initialPosition
     */
    fun setViewPager(view: ViewPager, initialPosition: Int)

    /**
     *
     * Set the current page of both the ViewPager and indicator.
     *
     *
     * This **must** be used if you need to set the page before
     * the views are drawn on screen (e.g., default start page).
     *
     * @param item
     */
    fun setCurrentItem(item: Int)

    /**
     * Set a page change listener which will receive forwarded events.
     *
     * @param listener
     */
    fun setOnPageChangeListener(listener: OnPageChangeListener?)

    /**
     * Notify the indicator that the fragment list has changed.
     */
    fun notifyDataSetChanged()
}