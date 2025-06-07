package com.rainkaze.birdwatcher.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rainkaze.birdwatcher.R;
import com.rainkaze.birdwatcher.activity.BirdDetailActivity;
import com.rainkaze.birdwatcher.adapter.BirdKnowledgeAdapter;
import com.rainkaze.birdwatcher.data.BirdData;
import com.rainkaze.birdwatcher.model.zoology.BirdSpecies;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class KnowledgeFragment extends Fragment implements BirdKnowledgeAdapter.OnBirdClickListener {

    private RecyclerView recyclerView;
    private BirdKnowledgeAdapter adapter;
    private SearchView searchView;
    private TextView emptyView;

    private final List<BirdSpecies> allSpecies = new ArrayList<>();
    private final List<BirdSpecies> displayedSpecies = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_knowledge, container, false);
        initViews(view);
        setupRecyclerView();
        setupSearchView();
        loadDataFromLocal();
        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.rv_knowledge);
        searchView = view.findViewById(R.id.search_view_knowledge);
        emptyView = view.findViewById(R.id.tv_empty_knowledge);
        ProgressBar progressBar = view.findViewById(R.id.progress_bar_knowledge);
        progressBar.setVisibility(View.GONE);
    }

    private void setupRecyclerView() {
        adapter = new BirdKnowledgeAdapter(getContext(), displayedSpecies, this);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.setAdapter(adapter);
    }

    private void loadDataFromLocal() {
        allSpecies.clear();
        allSpecies.addAll(BirdData.getBuiltInBirds());
        updateDisplayedBirds(allSpecies);
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterBirds(query);
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterBirds(newText);
                return true;
            }
        });
    }

    private void updateDisplayedBirds(List<BirdSpecies> birds) {
        displayedSpecies.clear();
        displayedSpecies.addAll(birds);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        if (displayedSpecies.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setText("未找到相关鸟类");
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    private void filterBirds(String query) {
        if (TextUtils.isEmpty(query)) {
            updateDisplayedBirds(allSpecies);
            return;
        }
        List<BirdSpecies> filteredList = allSpecies.stream()
                .filter(bird -> bird.getName().contains(query) || bird.getScientificName().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
        updateDisplayedBirds(filteredList);
    }

    @Override
    public void onBirdClick(BirdSpecies bird) {
        Intent intent = new Intent(getActivity(), BirdDetailActivity.class);
        intent.putExtra(BirdDetailActivity.EXTRA_BIRD_SPECIES, bird);
        startActivity(intent);
    }
}