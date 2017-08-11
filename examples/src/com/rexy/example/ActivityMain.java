package com.rexy.example;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;

import com.rexy.example.extend.BaseActivity;
import com.rexy.example.extend.ViewUtils;
import com.rexy.example.model.DecorationDividerLinear;
import com.rexy.example.model.TestData;
import com.rexy.example.model.TestRecyclerAdapter;
import com.rexy.interactionhook.example.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by rexy on 17/4/11.
 */
public class ActivityMain extends BaseActivity implements View.OnClickListener {
    RecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewUtils.view(this, R.id.button1).setOnClickListener(this);
        ViewUtils.view(this, R.id.button2).setOnClickListener(this);
        ViewUtils.view(this, R.id.jump).setOnClickListener(this);
        mRecyclerView = ViewUtils.view(this, R.id.recyclerView);
        initRecyclerView(mRecyclerView, 12);
    }


    private void initRecyclerView(RecyclerView recyclerView, int count) {
        Random random = new Random(System.currentTimeMillis());
        int[] icons = new int[]{
                android.R.drawable.ic_menu_camera,
                android.R.drawable.ic_menu_call,
                android.R.drawable.ic_menu_edit,
                android.R.drawable.ic_menu_search,
                android.R.drawable.ic_menu_send,
        };
        List<TestData> datas = new ArrayList(count);
        for (int i = 0; i < count; i++) {
            datas.add(new TestData("test title " + (i + 1), icons[random.nextInt(icons.length)]));
        }
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new TestRecyclerAdapter(datas, this));
        recyclerView.addItemDecoration(new DecorationDividerLinear(false,
                getResources().getDrawable(android.R.drawable.ic_menu_edit)));
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.jump) {
            ActivityCommon.launch(this, FragmentPageOne.class);
        } else {
            if(v.getParent()==mRecyclerView){
                TestRecyclerAdapter.TestRecyclerViewHolder holder= (TestRecyclerAdapter.TestRecyclerViewHolder) mRecyclerView.getChildViewHolder(v);
                if (holder!=null) {
                    TestRecyclerAdapter adapter = (TestRecyclerAdapter) mRecyclerView.getAdapter();
                    Toast.makeText(v.getContext(), adapter.getItem(holder.getAdapterPosition()).title, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
