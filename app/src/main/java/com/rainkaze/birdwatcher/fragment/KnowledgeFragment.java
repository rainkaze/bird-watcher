package com.rainkaze.birdwatcher.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rainkaze.birdwatcher.R;
import com.rainkaze.birdwatcher.activity.BirdDetailActivity;
import com.rainkaze.birdwatcher.adapter.BirdAdapter;
import com.rainkaze.birdwatcher.model.Bird;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class KnowledgeFragment extends Fragment implements BirdAdapter.OnBirdClickListener {

    private RecyclerView recyclerView;
    private BirdAdapter adapter;
    private List<Bird> birdList;
    private SearchView searchView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_knowledge, container, false);

        recyclerView = view.findViewById(R.id.recycler_view);
        searchView = view.findViewById(R.id.search_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        loadBirdData();

        if (birdList != null) {
            adapter = new BirdAdapter(birdList, this);
            recyclerView.setAdapter(adapter);
        }

        setupSearch();

        return view;
    }

    private void loadBirdData() {
        if (getContext() == null) return;
        try (InputStream inputStream = getContext().getAssets().open("bird_data.json");
             Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {

            Type listType = new TypeToken<ArrayList<Bird>>(){}.getType();
            birdList = new Gson().fromJson(reader, listType);

        } catch (IOException e) {
            e.printStackTrace();
            birdList = new ArrayList<>();
        }
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (adapter != null) {
                    adapter.filter(query);
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (adapter != null) {
                    adapter.filter(newText);
                }
                return true;
            }
        });
    }

    @Override
    public void onBirdClick(Bird bird) {
        if (getActivity() == null) return;
        Intent intent = new Intent(getActivity(), BirdDetailActivity.class);
        intent.putExtra("bird_details", bird);
        startActivity(intent);
    }
}