package com.rexy.example.model;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;

/**
 * This class can only be used in the RecyclerView which use a LinearLayoutManager or
 * its subclass.
 */
public class DecorationDividerLinear extends RecyclerView.ItemDecoration {
    private final SparseArray<DrawableCreator> mTypeDrawableFactories = new SparseArray<>();
    private boolean isHorizontal;
    private boolean isNoMoreData = true;
    private Drawable mContentDrawable;
    private Drawable mContentDrawableStart;
    private Drawable mContentDrawableEnd;

/*    //目前对于 BaseQuickAdapter 添加的头和尾部都会当作一个Item 项，可以试途径从相对开始和结尾来解决问题。
    //或是自己对家registerTypeDrawable 来处理。暂时不实现。
    private int mRelativeStart=-1;
    private int mRelativeEnd=-1;
    public void setRelativeStartAndEnd(int start,int end){
        mRelativeStart=start;
        mRelativeEnd=end;
    }*/

    public DecorationDividerLinear(boolean horizontal, Drawable contentDrawable) {
        this(horizontal, contentDrawable, null, null);
    }

    public DecorationDividerLinear(boolean horizontal, Drawable contentDrawable, Drawable contentDrawableStart, Drawable contentDrawableEnd) {
        isHorizontal = horizontal;
        mContentDrawable = contentDrawable;
        mContentDrawableStart = contentDrawableStart;
        mContentDrawableEnd = contentDrawableEnd;
    }

    public void registerTypeDrawable(int itemType, DrawableCreator drawableCreator) {
        mTypeDrawableFactories.put(itemType, drawableCreator);
    }

    public void setNoMoreData(boolean noMoreData) {
        isNoMoreData = noMoreData;
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        if (isHorizontal) {
            drawHorizontalDividers(c, parent);
        } else {
            drawVerticalDividers(c, parent);
        }
    }

    public void drawVerticalDividers(Canvas c, RecyclerView parent) {
        final int left = parent.getPaddingLeft();
        final int right = parent.getWidth() - parent.getPaddingRight();
        final int childCount = parent.getChildCount();
        final int adapterLastItemIndex = parent.getAdapter() == null ? -1 : parent.getAdapter().getItemCount() - 1;
        for (int i = 0; i < childCount; i++) {
            final View child = parent.getChildAt(i);
            final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
            int adapterPosition = params.getViewAdapterPosition();
            if (adapterPosition == 0 && mContentDrawableStart != null) {
                int bottom = child.getTop() + params.topMargin + Math.round(ViewCompat.getTranslationY(child));
                int top = bottom - mContentDrawableStart.getIntrinsicHeight();
                mContentDrawableStart.setBounds(left, top, right, bottom);
                mContentDrawableStart.draw(c);
            }
            final Drawable divider = (adapterPosition == adapterLastItemIndex && isNoMoreData) ? mContentDrawableEnd : getDividerDrawable(parent, adapterPosition);
            if (divider != null) {
                int top = child.getBottom() + params.bottomMargin + Math.round(ViewCompat.getTranslationY(child));
                int bottom = top + divider.getIntrinsicHeight();
                divider.setBounds(left, top, right, bottom);
                divider.draw(c);
            }
        }
    }

    public void drawHorizontalDividers(Canvas c, RecyclerView parent) {
        final int top = parent.getPaddingTop();
        final int bottom = parent.getHeight() - parent.getPaddingBottom();
        final int childCount = parent.getChildCount();
        final int adapterLastItemIndex = parent.getAdapter() == null ? -1 : parent.getAdapter().getItemCount() - 1;
        for (int i = 0; i < childCount; i++) {
            final View child = parent.getChildAt(i);
            final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
            int adapterPosition = params.getViewAdapterPosition();
            if (adapterPosition == 0 && mContentDrawableStart != null) {
                int right = child.getLeft() + params.leftMargin + Math.round(ViewCompat.getTranslationX(child));
                int left = right - mContentDrawableStart.getIntrinsicWidth();
                mContentDrawableStart.setBounds(left, top, right, bottom);
                mContentDrawableStart.draw(c);
            }
            final Drawable divider = (adapterPosition == adapterLastItemIndex && isNoMoreData) ? mContentDrawableEnd : getDividerDrawable(parent, adapterPosition);
            if (divider != null) {
                int left = child.getRight() + params.rightMargin + Math.round(ViewCompat.getTranslationX(child));
                int right = left + divider.getIntrinsicWidth();
                divider.setBounds(left, top, right, bottom);
                divider.draw(c);
            }
        }
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int adapterPosition = parent.getChildAdapterPosition(view);
        int adapterLastCountIndex = state.getItemCount() - 1;
        boolean contentItemFirst = adapterPosition == 0;
        boolean contentItemLast = adapterPosition == adapterLastCountIndex;
        boolean dataLastItem = contentItemLast && isNoMoreData;
        Drawable divider = dataLastItem ? mContentDrawableEnd : getDividerDrawable(parent, adapterPosition);
        if (isHorizontal) {
            outRect.right = divider == null ? 0 : divider.getIntrinsicWidth();
            if (contentItemFirst) {
                outRect.left = mContentDrawableStart == null ? 0 : mContentDrawableStart.getIntrinsicWidth();
            }
        } else {
            outRect.bottom = divider == null ? 0 : divider.getIntrinsicHeight();
            if (contentItemFirst) {
                outRect.top = mContentDrawableStart == null ? 0 : mContentDrawableStart.getIntrinsicHeight();
            }
        }
    }

    private Drawable getDividerDrawable(RecyclerView parent, int adapterPosition) {
        if (mTypeDrawableFactories.size() == 0) {
            return mContentDrawable;
        }
        final int itemType = parent.getAdapter() == null ? 0 : parent.getAdapter().getItemViewType(adapterPosition);
        final DecorationDividerLinear.DrawableCreator offsetsCreator = mTypeDrawableFactories.get(itemType);
        if (offsetsCreator != null) {
            return offsetsCreator.create(parent, adapterPosition);
        }
        return mContentDrawable;
    }
    public interface DrawableCreator {
        Drawable create(RecyclerView parent, int adapterPosition);
    }
}
