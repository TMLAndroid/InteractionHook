package com.rexy.example.model;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.rexy.interactionhook.example.R;
import com.rexy.widgets.layout.WrapLayout;

import java.util.List;

/**
 * Created by rexy on 17/8/10.
 */

public class TestRecyclerAdapter extends RecyclerView.Adapter<TestRecyclerAdapter.TestRecyclerViewHolder> {

    List<TestData> mDatas;

    View.OnClickListener mClickListener;

    public TestRecyclerAdapter(List<TestData> datas,View.OnClickListener l) {
        mDatas = datas;
        mClickListener=l;
    }

    @Override
    public TestRecyclerViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        Context context = viewGroup.getContext();
        WrapLayout itemView = new WrapLayout(context);
        itemView.setSupportWeight(true);
        itemView.setGravity(Gravity.CENTER_VERTICAL);
        itemView.setEachLineCenterVertical(true);
        ImageView iconView = new ImageView(context);
        itemView.addView(iconView);
        TextView textView = new TextView(context);
        textView.setDuplicateParentStateEnabled(true);
        textView.setTextColor(context.getResources().getColorStateList(R.color.select_tab_item_text));
        WrapLayout.LayoutParams lp = new WrapLayout.LayoutParams(0, -2);
        lp.weight = 1;
        lp.leftMargin=50;
        itemView.addView(textView, lp);
        itemView.setOnClickListener(mClickListener);
        itemView.setLayoutParams(new RecyclerView.LayoutParams(-1,-2));
        return new TestRecyclerViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(TestRecyclerViewHolder testRecyclerViewHolder, int i) {
        TestData data=mDatas.get(i);
        ImageView imageView= (ImageView) ((ViewGroup)testRecyclerViewHolder.itemView).getChildAt(0);
        TextView textView= (TextView) ((ViewGroup)testRecyclerViewHolder.itemView).getChildAt(1);
        imageView.setImageResource(data.icon);
        textView.setText(data.title);
    }

    @Override
    public int getItemCount() {
        return mDatas == null ? 0 : mDatas.size();
    }

    public TestData getItem(int position){
        if(position>=0&&position<getItemCount()){
            return mDatas.get(position);
        }
        return null;
    }

    public static class TestRecyclerViewHolder extends RecyclerView.ViewHolder {
        public TestRecyclerViewHolder(View itemView) {
            super(itemView);
        }
    }
}
