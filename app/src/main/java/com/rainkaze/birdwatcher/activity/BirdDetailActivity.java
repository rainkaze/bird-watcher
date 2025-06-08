package com.rainkaze.birdwatcher.activity;

import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar; // 导入Toolbar类

import com.rainkaze.birdwatcher.R;
import com.rainkaze.birdwatcher.model.Bird;

public class BirdDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bird_detail);

        // 1. 找到Toolbar并设置为ActionBar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 2. 现在可以安全地使用SupportActionBar了
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        TextView chineseName = findViewById(R.id.detail_chinese_name);
        TextView scientificName = findViewById(R.id.detail_scientific_name);
        TextView order = findViewById(R.id.detail_order);
        TextView family = findViewById(R.id.detail_family);
        TextView iucn = findViewById(R.id.detail_iucn);
        TextView protection = findViewById(R.id.detail_protection);
        TextView details = findViewById(R.id.detail_bird_details);
        TextView detailsUrl = findViewById(R.id.detail_url);

        Bird bird = getIntent().getParcelableExtra("bird_details");

        if (bird != null) {
            // 设置标题栏标题
            getSupportActionBar().setTitle(bird.getChineseName());

            chineseName.setText(bird.getChineseName());
            scientificName.setText(bird.getScientificName());
            order.setText(Html.fromHtml("<b>所属目:</b> " + bird.getOrder()));
            family.setText(Html.fromHtml("<b>所属科:</b> " + bird.getFamily()));
            iucn.setText(Html.fromHtml("<b>IUCN红色名录:</b> " + bird.getIucnRedList()));
            protection.setText(Html.fromHtml("<b>国家保护等级:</b> " + bird.getNationalProtectionLevel()));
            details.setText(bird.getBirdDetails());
            detailsUrl.setText(Html.fromHtml("<b>详情链接:</b> " + "<a href='" + bird.getDetailsUrl() + "'>" + bird.getDetailsUrl() + "</a>"));
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}