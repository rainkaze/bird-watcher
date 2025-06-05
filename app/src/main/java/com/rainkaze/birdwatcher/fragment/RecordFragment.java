package com.rainkaze.birdwatcher.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
// import androidx.appcompat.app.AlertDialog; // 用于长按删除确认

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.rainkaze.birdwatcher.R;
import com.rainkaze.birdwatcher.activity.AddEditRecordActivity;
import com.rainkaze.birdwatcher.adapter.RecordAdapter;
import com.rainkaze.birdwatcher.model.BirdRecord;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RecordFragment extends Fragment implements RecordAdapter.OnItemClickListener {

    private RecyclerView rvRecords;
    private RecordAdapter recordAdapter;
    private List<BirdRecord> recordList = new ArrayList<>();
    private TextView tvEmptyRecords;
    private FloatingActionButton fabAddRecord;

    private ActivityResultLauncher<Intent> addEditRecordLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 初始化 ActivityResultLauncher
        addEditRecordLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // TODO: 当从 AddEditRecordActivity 返回时，刷新列表
                        //  如果 AddEditRecordActivity 通过 Intent 返回了数据，可以在这里处理
                        Toast.makeText(getContext(), "记录操作完成，刷新列表 (待实现)", Toast.LENGTH_SHORT).show();
                        loadRecords(); // 重新加载数据
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

        setupRecyclerView();

        fabAddRecord.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddEditRecordActivity.class);
            addEditRecordLauncher.launch(intent);
        });

        loadRecords(); // 加载数据（目前是示例数据）

        return view;
    }

    private void setupRecyclerView() {
        recordAdapter = new RecordAdapter(getContext(), recordList, this);
        rvRecords.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRecords.setAdapter(recordAdapter);
    }

    private void loadRecords() {
        // TODO: 后续从数据库加载真实数据
        // 目前使用示例数据
        if (recordList.isEmpty()) { // 简单防止重复添加示例数据
            recordList.add(new BirdRecord("公园的喜鹊", "喜鹊", "今天在公园看到一群喜鹊，非常活泼。", new Date()));
            BirdRecord br2 = new BirdRecord("清晨的麻雀", "麻雀", "窗台上的常客。", new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24)); // 昨天
            br2.setDetailedLocation("我家窗台");
            br2.setScientificName("Passer montanus");
            recordList.add(br2);
        }
        // recordList.add(new BirdRecord(...)); // 可以添加更多

        updateEmptyViewVisibility();
        recordAdapter.setRecords(recordList); // 使用 setRecords 更新适配器数据
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
        // TODO: 点击列表项，可以跳转到记录详情页或编辑页
        Toast.makeText(getContext(), "点击了: " + record.getTitle(), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getActivity(), AddEditRecordActivity.class);
        intent.putExtra(AddEditRecordActivity.EXTRA_RECORD_ID, record.getId()); // 假设 getId() 返回唯一ID
        addEditRecordLauncher.launch(intent);
    }

    @Override
    public void onItemLongClick(BirdRecord record, View view) {
        // TODO: 长按列表项，可以弹出菜单进行删除、编辑等操作
        PopupMenu popup = new PopupMenu(requireContext(), view);
        popup.getMenuInflater().inflate(R.menu.menu_record_item_context, popup.getMenu()); // 需要创建此菜单文件
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_edit_record_item) {
                onItemClick(record); // 复用点击逻辑跳转到编辑
                return true;
            } else if (itemId == R.id.action_delete_record_item) {
                // 显示确认对话框
                // new AlertDialog.Builder(requireContext())
                //    .setTitle("删除记录")
                //    .setMessage("确定要删除 \"" + record.getTitle() + "\" 这条记录吗？")
                //    .setPositiveButton("删除", (dialog, which) -> {
                //        deleteRecord(record);
                //    })
                //    .setNegativeButton("取消", null)
                //    .show();
                Toast.makeText(getContext(), "删除功能待实现: " + record.getTitle(), Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
        popup.show();
    }

    // private void deleteRecord(BirdRecord record) {
    //    // TODO: 从数据库删除记录
    //    recordList.remove(record);
    //    recordAdapter.notifyDataSetChanged(); // 或者更精确的 notifyItemRemoved
    //    updateEmptyViewVisibility();
    //    Toast.makeText(getContext(), "\"" + record.getTitle() + "\" 已删除 (逻辑待数据库实现)", Toast.LENGTH_SHORT).show();
    // }

    @Override
    public void onResume() {
        super.onResume();
        // 当Fragment重新可见时，可以考虑刷新列表，以防数据在其他地方被修改
        // loadRecords(); // 谨慎使用，避免不必要的重复加载
    }
}