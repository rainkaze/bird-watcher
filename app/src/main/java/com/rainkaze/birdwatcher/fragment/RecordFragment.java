package com.rainkaze.birdwatcher.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.rainkaze.birdwatcher.R;
import com.rainkaze.birdwatcher.activity.AddEditRecordActivity;
import com.rainkaze.birdwatcher.adapter.RecordAdapter;
import com.rainkaze.birdwatcher.db.BirdRecordDao;
import com.rainkaze.birdwatcher.model.BirdRecord;

import java.util.ArrayList;
import java.util.List;

public class RecordFragment extends Fragment implements RecordAdapter.OnItemClickListener {

    private static final String TAG = "RecordFragment";

    private RecyclerView rvRecords;
    private RecordAdapter recordAdapter;
    private List<BirdRecord> recordList = new ArrayList<>();
    private TextView tvEmptyRecords;
    private FloatingActionButton fabAddRecord;
    private SearchView searchViewRecords;
    private ImageButton btnSortRecords;

    private BirdRecordDao birdRecordDao;
    private ActivityResultLauncher<Intent> addEditRecordLauncher;

    private enum SortMethod {
        TIME_DESC, TIME_ASC, NAME_ASC, NAME_DESC, HAS_PHOTO
    }
    private SortMethod currentSortMethod = SortMethod.TIME_DESC;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        birdRecordDao = new BirdRecordDao(requireContext());

        addEditRecordLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        loadRecordsFromDb();
                        Toast.makeText(getContext(), "记录已更新", Toast.LENGTH_SHORT).show();
                    } else {
                    }
                });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_record, container, false);

        rvRecords = view.findViewById(R.id.rv_records);
        tvEmptyRecords = view.findViewById(R.id.tv_empty_records);
        fabAddRecord = view.findViewById(R.id.fab_add_record);
        searchViewRecords = view.findViewById(R.id.search_view_records);
        btnSortRecords = view.findViewById(R.id.btn_sort_records);

        setupRecyclerView();
        setupSearchView();

        fabAddRecord.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddEditRecordActivity.class);
            addEditRecordLauncher.launch(intent);
        });

        btnSortRecords.setOnClickListener(this::showSortMenu);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadRecordsFromDb();
    }

    private void setupRecyclerView() {
        recordAdapter = new RecordAdapter(getContext(), recordList, this);
        rvRecords.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRecords.setAdapter(recordAdapter);
    }

    private void setupSearchView() {
        try {
            EditText searchEditText = searchViewRecords.findViewById(androidx.appcompat.R.id.search_src_text);
            if (searchEditText != null) {
                searchEditText.setGravity(Gravity.CENTER_VERTICAL);
                searchEditText.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            }
        } catch (Exception e) {
        }

        searchViewRecords.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                searchViewRecords.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                performSearch(newText);
                return true;
            }
        });

        View closeButton = searchViewRecords.findViewById(androidx.appcompat.R.id.search_close_btn);
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> {
                searchViewRecords.setQuery("", false);
                searchViewRecords.clearFocus();
                loadRecordsFromDb();
            });
        }
    }

    private void performSearch(String query) {
        birdRecordDao.open();
        List<BirdRecord> searchResults = birdRecordDao.searchRecords(query);
        birdRecordDao.close();

        recordList.clear();
        if (searchResults != null) {
            recordList.addAll(searchResults);
        }
        sortRecordsAndUpdateAdapter();
        updateEmptyViewVisibility();
    }

    private void loadRecordsFromDb() {
        birdRecordDao.open();
        List<BirdRecord> allRecords = birdRecordDao.getAllRecords();
        birdRecordDao.close();

        recordList.clear();
        if (allRecords != null) {
            recordList.addAll(allRecords);
        }
        sortRecordsAndUpdateAdapter();
        updateEmptyViewVisibility();
    }

    private void updateEmptyViewVisibility() {
        if (recordList.isEmpty() && searchViewRecords.getQuery().toString().isEmpty()) {
            rvRecords.setVisibility(View.GONE);
            tvEmptyRecords.setVisibility(View.VISIBLE);
            tvEmptyRecords.setText("还没有任何观鸟记录\n点击右下角按钮添加吧！");
        } else if (recordList.isEmpty()) {
            rvRecords.setVisibility(View.GONE);
            tvEmptyRecords.setVisibility(View.VISIBLE);
            tvEmptyRecords.setText("没有找到相关记录");
        } else {
            rvRecords.setVisibility(View.VISIBLE);
            tvEmptyRecords.setVisibility(View.GONE);
        }
    }

    /**
     * 显示排序选项菜单
     * @param v 触发此操作的视图（排序按钮）
     */
    private void showSortMenu(View v) {
        PopupMenu popup = new PopupMenu(requireContext(), v);
        popup.getMenuInflater().inflate(R.menu.menu_record_sort, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.sort_time_desc) {
                currentSortMethod = SortMethod.TIME_DESC;
            } else if (itemId == R.id.sort_time_asc) {
                currentSortMethod = SortMethod.TIME_ASC;
            } else if (itemId == R.id.sort_name_asc) {
                currentSortMethod = SortMethod.NAME_ASC;
            } else if (itemId == R.id.sort_name_desc) {
                currentSortMethod = SortMethod.NAME_DESC;
            } else if (itemId == R.id.sort_has_photo) {
                currentSortMethod = SortMethod.HAS_PHOTO;
            }
            sortRecordsAndUpdateAdapter();
            Toast.makeText(getContext(), "已按 \"" + item.getTitle() + "\" 排序", Toast.LENGTH_SHORT).show();
            return true;
        });

        popup.show();
    }

    /**
     * 根据 currentSortMethod 对 recordList 进行排序并更新 Adapter
     */
    private void sortRecordsAndUpdateAdapter() {
        if (recordList == null || recordList.isEmpty()) {
            if (recordAdapter != null) {
                recordAdapter.setRecords(new ArrayList<>());
            }
            return;
        }

        switch (currentSortMethod) {
            case TIME_ASC:
                recordList.sort((r1, r2) -> Long.compare(r1.getRecordDateTimestamp(), r2.getRecordDateTimestamp()));
                break;
            case NAME_ASC:
                recordList.sort((r1, r2) -> r1.getBirdName().compareToIgnoreCase(r2.getBirdName()));
                break;
            case NAME_DESC:
                recordList.sort((r1, r2) -> r2.getBirdName().compareToIgnoreCase(r1.getBirdName()));
                break;
            case HAS_PHOTO:
                recordList.sort((r1, r2) -> {
                    boolean r1HasPhoto = r1.getPhotoUris() != null && !r1.getPhotoUris().isEmpty();
                    boolean r2HasPhoto = r2.getPhotoUris() != null && !r2.getPhotoUris().isEmpty();
                    return Boolean.compare(r2HasPhoto, r1HasPhoto);
                });
                break;
            case TIME_DESC:
            default:
                recordList.sort((r1, r2) -> Long.compare(r2.getRecordDateTimestamp(), r1.getRecordDateTimestamp()));
                break;
        }
        recordAdapter.setRecords(recordList);
    }


    @Override
    public void onItemClick(BirdRecord record) {
        Intent intent = new Intent(getActivity(), AddEditRecordActivity.class);
        intent.putExtra(AddEditRecordActivity.EXTRA_RECORD_ID, record.getId());
        addEditRecordLauncher.launch(intent);
    }

    @Override
    public void onItemLongClick(BirdRecord record, View anchorView) {
        PopupMenu popup = new PopupMenu(requireContext(), anchorView);
        popup.getMenuInflater().inflate(R.menu.menu_record_item_context, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_edit_record_item) {
                onItemClick(record);
                return true;
            } else if (itemId == R.id.action_delete_record_item) {
                showDeleteConfirmationDialog(record);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void showDeleteConfirmationDialog(final BirdRecord record) {
        new AlertDialog.Builder(requireContext())
                .setTitle("删除记录")
                .setMessage("确定要删除 \"" + record.getTitle() + "\" 这条记录吗？此操作无法撤销。")
                .setPositiveButton("删除", (dialog, which) -> deleteRecordFromDb(record))
                .setNegativeButton("取消", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteRecordFromDb(BirdRecord record) {
        birdRecordDao.open();
        boolean success = birdRecordDao.deleteRecord(record.getId());
        birdRecordDao.close();

        if (success) {
            recordList.removeIf(r -> r.getId() == record.getId());
            sortRecordsAndUpdateAdapter();
            updateEmptyViewVisibility();
            Toast.makeText(getContext(), "\"" + record.getTitle() + "\" 已删除", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "删除失败", Toast.LENGTH_SHORT).show();
        }
    }
}