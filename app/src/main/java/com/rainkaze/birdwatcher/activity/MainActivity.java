package com.rainkaze.birdwatcher.activity;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.baidu.mapapi.SDKInitializer;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.rainkaze.birdwatcher.R;
import com.rainkaze.birdwatcher.fragment.HomeFragment;
import com.rainkaze.birdwatcher.fragment.IdentifyFragment;
import com.rainkaze.birdwatcher.fragment.KnowledgeFragment;
import com.rainkaze.birdwatcher.fragment.MapFragment;
import com.rainkaze.birdwatcher.fragment.RecordFragment;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private List<Fragment> list;
    private BottomNavigationView bottomNavigationView;
    private boolean isMapInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        try {
            SDKInitializer.setAgreePrivacy(getApplicationContext(), true);
            SDKInitializer.initialize(getApplicationContext());
            isMapInitialized = true;
        } catch (Exception e) {
            e.printStackTrace();
            isMapInitialized = false;
        }

        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        list = new ArrayList<>();
        list.add(new HomeFragment());
        list.add(new MapFragment());
        list.add(new IdentifyFragment());
        list.add(new RecordFragment());
        list.add(new KnowledgeFragment());

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                selectedFragment = list.get(0);
            } else if (itemId == R.id.navigation_map) {
                selectedFragment = list.get(1);
            } else if (itemId == R.id.navigation_identify) {
                selectedFragment = list.get(2);
            } else if (itemId == R.id.navigation_record) {
                selectedFragment = list.get(3);
            } else if (itemId == R.id.navigation_knowledge) {
                selectedFragment = list.get(4);
            }

            if (selectedFragment != null) {
                showFragment(selectedFragment);
            }
            return true;
        });

        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_home);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBarInsets.left, systemBarInsets.top, systemBarInsets.right, 0);
            return insets;
        });
    }

    private void showFragment(Fragment fragment) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_container, fragment);
        ft.commit();
    }
}