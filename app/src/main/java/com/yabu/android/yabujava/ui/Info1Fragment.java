package com.yabu.android.yabujava.ui;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.yabu.android.yabujava.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class Info1Fragment extends Fragment {


    public Info1Fragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_info1, container, false);
    }

}
