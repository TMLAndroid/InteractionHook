package com.rexy.example;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.rexy.example.extend.BaseFragment;
import com.rexy.example.extend.ViewUtils;
import com.rexy.interactionhook.example.R;

/**
 * TODO:功能说明
 *
 * @author: rexy
 * @date: 2017-08-07 17:52
 */
public class FragmentPageTab extends BaseFragment implements View.OnClickListener {

    public static final String TAB_INDEX = "TAB_INDEX";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_page_one, container, false);
        TextView message= (TextView) root.findViewById(R.id.message);
        message.setGravity(Gravity.CENTER);
        message.setTextSize(25);
        message.setTextColor(0xFFFF0000);
        message.setText("TAB FRAGMENT " + getArguments().getInt(TAB_INDEX));
        Button button= ViewUtils.view(root,R.id.jump);
        button.setText("go back");
        button.setOnClickListener(this);
        root.setBackgroundColor(0x330000ff);
        return root;
    }

    @Override
    public void onClick(View v) {
        if (getParentFragment() != null) {
            getParentFragment().getFragmentManager().popBackStack();
        }
    }
}
