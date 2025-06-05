package com.rainkaze.birdwatcher.fragment;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar; // 如果使用了 Toolbar
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

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
    private List<BirdRecord> recordList = new ArrayList<>(); // 当前显示在列表中的数据
    private TextView tvEmptyRecords;
    private FloatingActionButton fabAddRecord;
    private SearchView searchViewRecords;
    private Toolbar toolbarRecord; // 如果你添加了Toolbar

    private BirdRecordDao birdRecordDao;

    private ActivityResultLauncher<Intent> addEditRecordLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        birdRecordDao = new BirdRecordDao(requireContext());
        setHasOptionsMenu(true); // 告诉 Fragment 它想要参与选项菜单的处理 (如果搜索在Toolbar菜单中)

        addEditRecordLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // 记录已成功添加或编辑
                        Log.d(TAG, "Returned from AddEditRecordActivity with RESULT_OK");
                        loadRecordsFromDb(); // 重新从数据库加载数据
                        Toast.makeText(getContext(), "记录已更新", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.d(TAG, "Returned from AddEditRecordActivity with code: " + result.getResultCode());
                    }
                });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_record, container, false);

        toolbarRecord = view.findViewById(R.id.toolbar_record);
        // 如果你的 MainActivity 已经有 Toolbar，并且你希望 Fragment 控制自己的 Toolbar，
        // 你可能需要 ((AppCompatActivity)getActivity()).setSupportActionBar(toolbarRecord);
        // 但通常 Fragment 内的 Toolbar 是作为 AppBarLayout 的一部分。

        rvRecords = view.findViewById(R.id.rv_records);
        tvEmptyRecords = view.findViewById(R.id.tv_empty_records);
        fabAddRecord = view.findViewById(R.id.fab_add_record);
        searchViewRecords = view.findViewById(R.id.search_view_records); // 从 Toolbar 中获取

        setupRecyclerView();
        setupSearchView();

        fabAddRecord.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddEditRecordActivity.class);
            // 不需要传递 EXTRA_RECORD_ID 表示是新增
            addEditRecordLauncher.launch(intent);
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadRecordsFromDb(); // 在视图创建后加载数据
    }

    private void setupRecyclerView() {
        recordAdapter = new RecordAdapter(getContext(), recordList, this);
        rvRecords.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRecords.setAdapter(recordAdapter);
    }

    private void setupSearchView() {
        searchViewRecords.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // 用户提交搜索时（例如按下回车或搜索按钮）
                performSearch(query);
                searchViewRecords.clearFocus(); // 收起键盘
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // 用户输入文本时实时搜索
                performSearch(newText);
                return true;
            }
        });

        // 当关闭搜索视图时，重新加载所有记录
        searchViewRecords.setOnCloseListener(() -> {
            loadRecordsFromDb();
            return false; // 返回 false 表示 SearchView 可以正常关闭
        });
    }

    private void performSearch(String query) {
        birdRecordDao.open();
        List<BirdRecord> searchResults = birdRecordDao.searchRecords(query);
        birdRecordDao.close();

        recordList.clear();
        if (searchResults != null) {
            recordList.addAll(searchResults);
        }
        recordAdapter.setRecords(recordList); // 使用 adapter 的方法更新数据
        updateEmptyViewVisibility(); // 更新空状态视图的可见性
    }


    private void loadRecordsFromDb() {
        Log.d(TAG, "Loading records from DB...");
        birdRecordDao.open();
        List<BirdRecord> allRecords = birdRecordDao.getAllRecords();
        birdRecordDao.close();

        recordList.clear();
        if (allRecords != null) {
            recordList.addAll(allRecords);
        }
        recordAdapter.setRecords(recordList); // 更新适配器数据
        updateEmptyViewVisibility();
        Log.d(TAG, "Loaded " + recordList.size() + " records into adapter.");
    }

    private void updateEmptyViewVisibility() {
        if (recordList.isEmpty()) {
            rvRecords.setVisibility(View.GONE);
            tvEmptyRecords.setVisibility(View.VISIBLE);
        } else {
            rvRecords.setVisibility(View.VISIBLE);
            tvEmptyRecords.setVisibility(View.GONE);
        }
    }

    // RecordAdapter.OnItemClickListener 实现
    @Override
    public void onItemClick(BirdRecord record) {
        Log.d(TAG, "Clicked record: " + record.getTitle() + " (ID: " + record.getId() + ")");
        Intent intent = new Intent(getActivity(), AddEditRecordActivity.class);
        intent.putExtra(AddEditRecordActivity.EXTRA_RECORD_ID, record.getId());
        addEditRecordLauncher.launch(intent);
    }

    @Override
    public void onItemLongClick(BirdRecord record, View anchorView) {
        Log.d(TAG, "Long-clicked record: " + record.getTitle() + " (ID: " + record.getId() + ")");
        PopupMenu popup = new PopupMenu(requireContext(), anchorView); // 使用 anchorView
        popup.getMenuInflater().inflate(R.menu.menu_record_item_context, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_edit_record_item) {
                onItemClick(record); // 复用点击逻辑跳转到编辑
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
                .setIcon(android.R.drawable.ic_dialog_alert) // 可选：添加警告图标
                .show();
    }

    private void deleteRecordFromDb(BirdRecord record) {
        birdRecordDao.open();
        boolean success = birdRecordDao.deleteRecord(record.getId());
        birdRecordDao.close();

        if (success) {
            Log.d(TAG, "Record deleted successfully from DB: ID " + record.getId());
            recordAdapter.removeRecord(record.getId()); // 从适配器中移除
            updateEmptyViewVisibility();
            Toast.makeText(getContext(), "\"" + record.getTitle() + "\" 已删除", Toast.LENGTH_SHORT).show();
        } else {
            Log.e(TAG, "Failed to delete record from DB: ID " + record.getId());
            Toast.makeText(getContext(), "删除失败", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // 当 Fragment 重新可见时，可以考虑刷新列表，以防数据在其他地方被修改
        // 但由于有 ActivityResultLauncher，通常在返回时已经刷新了
        // 如果搜索框是激活状态并且内容为空，可能需要重新加载所有数据
        if (searchViewRecords != null && searchViewRecords.getQuery().length() == 0 && !searchViewRecords.isIconified()) {
            // loadRecordsFromDb(); // 如果之前是搜索结果为空，返回时应显示所有
        } else if (searchViewRecords != null && searchViewRecords.getQuery().length() > 0) {
            // 如果有搜索词，则保持搜索结果
            performSearch(searchViewRecords.getQuery().toString());
        } else {
            // 默认情况，重新加载 (或依赖 AddEdit返回时的刷新)
            loadRecordsFromDb();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 清理资源，例如关闭数据库连接（如果 DAO 在 Fragment 生命周期内管理）
        // birdRecordDao.close(); // 通常 DAO 的 open/close 在每个方法调用前后进行
    }

    // --- 处理 Toolbar 菜单 (如果需要，例如将 SearchView 放入菜单项) ---
    // @Override
    // public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
    //     inflater.inflate(R.menu.menu_record_fragment, menu); // 创建 menu_record_fragment.xml
    //     MenuItem searchItem = menu.findItem(R.id.action_search_records);
    //     searchViewRecords = (SearchView) searchItem.getActionView();
    //     setupSearchView(); // 在这里设置监听器
    //     super.onCreateOptionsMenu(menu, inflater);
    // }
}