package com.rainkaze.birdwatcher.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.rainkaze.birdwatcher.R;

public class IdentifyFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // 加载首页布局
        return inflater.inflate(R.layout.fragment_identify, container, false);
    }
}