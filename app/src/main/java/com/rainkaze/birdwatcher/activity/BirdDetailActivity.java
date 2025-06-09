package com.rainkaze.birdwatcher.activity;

import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.rainkaze.birdwatcher.R;
import com.rainkaze.birdwatcher.databinding.ActivityBirdDetailBinding;
import com.rainkaze.birdwatcher.model.Bird;

/**
 * 显示单个鸟类详细信息的 Activity。
 *
 * 该页面从 Intent 中接收一个 Bird 对象，并将其详细信息（如名称、科目、保护等级等）
 * 展示给用户。
 */
public class BirdDetailActivity extends AppCompatActivity {

    /**
     * 用于通过 Intent 传递 Bird 对象的 Extra Key。
     */
    public static final String EXTRA_BIRD_DETAILS = "bird_details";

    // ViewBinding 对象
    private ActivityBirdDetailBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 使用 View Binding 初始化视图
        binding = ActivityBirdDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 设置 Toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // 从 Intent 中获取 Bird 对象
        Bird bird = getIntent().getParcelableExtra(EXTRA_BIRD_DETAILS);

        if (bird != null) {
            populateUi(bird);
        }
    }

    /**
     * 将 Bird 对象的数据填充到UI组件中。
     *
     * @param bird 包含要显示数据的 Bird 对象。
     */
    private void populateUi(Bird bird) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(bird.getChineseName());
        }

        binding.detailChineseName.setText(bird.getChineseName());
        binding.detailScientificName.setText(bird.getScientificName());

        // 使用字符串资源和 Html.fromHtml 来格式化文本
        // 注意：Html.fromHtml(String) 在 API 24 中被弃用，我们使用兼容版本
        String orderText = getString(R.string.bird_detail_order, bird.getOrder());
        binding.detailOrder.setText(Html.fromHtml(orderText, Html.FROM_HTML_MODE_LEGACY));

        String familyText = getString(R.string.bird_detail_family, bird.getFamily());
        binding.detailFamily.setText(Html.fromHtml(familyText, Html.FROM_HTML_MODE_LEGACY));

        String iucnText = getString(R.string.bird_detail_iucn, bird.getIucnRedList());
        binding.detailIucn.setText(Html.fromHtml(iucnText, Html.FROM_HTML_MODE_LEGACY));

        String protectionText = getString(R.string.bird_detail_protection, bird.getNationalProtectionLevel());
        binding.detailProtection.setText(Html.fromHtml(protectionText, Html.FROM_HTML_MODE_LEGACY));

        binding.detailBirdDetails.setText(bird.getBirdDetails());

        // 设置可点击的链接
        String urlText = getString(R.string.bird_detail_url, bird.getDetailsUrl(), bird.getDetailsUrl());
        binding.detailUrl.setText(Html.fromHtml(urlText, Html.FROM_HTML_MODE_LEGACY));
        binding.detailUrl.setMovementMethod(LinkMovementMethod.getInstance());
    }


    /**
     * 处理 Toolbar 上的返回按钮点击事件。
     * @return 总是返回 true，表示事件已被处理。
     */
    @Override
    public boolean onSupportNavigateUp() {
        // 当点击 Toolbar 的返回箭头时，结束当前 Activity
        onBackPressed();
        return true;
    }
}