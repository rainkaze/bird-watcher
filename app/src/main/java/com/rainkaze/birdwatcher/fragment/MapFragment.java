package com.rainkaze.birdwatcher.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.model.LatLngBounds;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.rainkaze.birdwatcher.R;
import com.rainkaze.birdwatcher.adapter.RecordSearchAdapter;
import com.rainkaze.birdwatcher.db.BirdRecordDao;
import com.rainkaze.birdwatcher.model.BirdRecord;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class MapFragment extends Fragment {

    private static final String TAG = "MapFragment";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    // --- 地图和UI组件 ---
    private MapView mMapView;
    private BaiduMap mBaiduMap;
    private FloatingActionButton btnMyLocation;
    private SearchView searchView;
    private RecyclerView rvSearchResults;
    private InfoWindow mInfoWindow;

    // --- 数据和适配器 ---
    private BirdRecordDao birdRecordDao;
    private RecordSearchAdapter searchAdapter;
    private List<BirdRecord> allRecordsWithLocation = new ArrayList<>(); // 存储所有带坐标的记录
    private List<BirdRecord> displayedRecords = new ArrayList<>(); // 当前显示在地图和列表中的记录

    // --- 地图标记和图标 ---
    private BitmapDescriptor recordMarkerIcon;

    // --- 定位相关 ---
    private LocationClient mLocationClient;
    private boolean isFirstLocate = true;
    private boolean hasLocationPermission = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 初始化DAO
        birdRecordDao = new BirdRecordDao(requireContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        initViews(view);
        initMap();
        initLocation();
        setupListeners();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // 视图创建后加载数据
        loadRecordsForMap();
    }

    private void initViews(View view) {
        mMapView = view.findViewById(R.id.bmapView);
        btnMyLocation = view.findViewById(R.id.btn_my_location);
        searchView = view.findViewById(R.id.sv_record_search); // 确保ID匹配
        rvSearchResults = view.findViewById(R.id.rv_search_results); // 确保ID匹配

        // 修改fragment_map.xml，将搜索框和结果列表的ID改为sv_record_search和rv_search_results
    }

    private void initMap() {
        mBaiduMap = mMapView.getMap();
        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        mBaiduMap.setMyLocationEnabled(true); // 启用定位图层

        // 创建一个通用的标记图标
        recordMarkerIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_location_on);

        // 初始化搜索结果列表
        rvSearchResults.setLayoutManager(new LinearLayoutManager(getContext()));
        searchAdapter = new RecordSearchAdapter(displayedRecords, record -> {
            // 点击搜索结果项，移动地图到对应位置并显示信息窗口
            panToRecord(record, true);
            rvSearchResults.setVisibility(View.GONE);
            searchView.clearFocus();
        });
        rvSearchResults.setAdapter(searchAdapter);
    }

    private void setupListeners() {
        btnMyLocation.setOnClickListener(v -> requestLocationUpdate());

        // 地图点击监听，用于隐藏信息窗口
        mBaiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                mBaiduMap.hideInfoWindow();
            }
            @Override
            public void onMapPoiClick(MapPoi mapPoi) {
                // Do nothing
            }
        });

        // 标记点击监听
        mBaiduMap.setOnMarkerClickListener(marker -> {
            Bundle bundle = marker.getExtraInfo();
            if (bundle != null) {
                long recordId = bundle.getLong("record_id", -1);
                if (recordId != -1) {
                    // 查找对应的记录并显示信息窗口
                    displayedRecords.stream()
                            .filter(r -> r.getId() == recordId)
                            .findFirst()
                            .ifPresent(this::showRecordInfoWindow);
                }
            }
            return true;
        });

        // 搜索框监听
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                performSearch(newText);
                return true;
            }
        });
        searchView.setOnCloseListener(() -> {
            loadRecordsForMap(); // 关闭搜索时，恢复显示所有记录
            return false;
        });
    }

    /**
     * 从数据库加载所有带坐标的记录
     */
    private void loadRecordsForMap() {
        birdRecordDao.open();
        List<BirdRecord> allRecords = birdRecordDao.getAllRecords();
        birdRecordDao.close();

        // 筛选出有经纬度的记录
        allRecordsWithLocation = allRecords.stream()
                .filter(r -> !Double.isNaN(r.getLatitude()) && !Double.isNaN(r.getLongitude()))
                .collect(Collectors.toList());

        Log.d(TAG, "Loaded " + allRecordsWithLocation.size() + " records with location.");
        updateMapWithRecords(allRecordsWithLocation);
    }

    /**
     * 执行搜索
     * @param query 搜索关键词
     */
    private void performSearch(String query) {
        if (TextUtils.isEmpty(query)) {
            // 如果搜索词为空，显示所有带位置的记录
            updateMapWithRecords(allRecordsWithLocation);
            rvSearchResults.setVisibility(View.GONE);
            return;
        }

        birdRecordDao.open();
        List<BirdRecord> searchResults = birdRecordDao.searchRecords(query);
        birdRecordDao.close();

        // 从搜索结果中再次筛选出有坐标的
        List<BirdRecord> filteredResults = searchResults.stream()
                .filter(r -> !Double.isNaN(r.getLatitude()) && !Double.isNaN(r.getLongitude()))
                .collect(Collectors.toList());

        updateMapWithRecords(filteredResults);

        // 更新搜索结果列表的可见性
        if (filteredResults.isEmpty()) {
            rvSearchResults.setVisibility(View.GONE);
        } else {
            rvSearchResults.setVisibility(View.VISIBLE);
        }
    }


    /**
     * 使用给定的记录列表更新地图和搜索结果列表
     * @param records 要显示的记录列表
     */
    private void updateMapWithRecords(List<BirdRecord> records) {
        // 更新当前显示的记录列表
        this.displayedRecords.clear();
        this.displayedRecords.addAll(records);

        // 清空地图上的旧标记
        mBaiduMap.clear();

        // 添加新标记
        for (BirdRecord record : this.displayedRecords) {
            LatLng point = new LatLng(record.getLatitude(), record.getLongitude());
            // 增加一个空值检查，确保图标已成功加载
            if (recordMarkerIcon != null) {
                MarkerOptions options = new MarkerOptions()
                        .position(point)
                        .icon(recordMarkerIcon)
                        .zIndex(9)
                        .draggable(false);
                Marker marker = (Marker) mBaiduMap.addOverlay(options);
                Bundle bundle = new Bundle();
                bundle.putLong("record_id", record.getId());
                marker.setExtraInfo(bundle);
            } else {
                // 如果图标为空，在日志中记录一个错误，但不会导致应用闪退
                Log.e(TAG, "recordMarkerIcon is null, cannot create a marker for record: " + record.getTitle());
            }
        }

        // 更新搜索结果列表的适配器
        searchAdapter.notifyDataSetChanged();

        // 如果有结果，调整地图视野以包含所有标记
        if (!this.displayedRecords.isEmpty()) {
            zoomToFitRecords(this.displayedRecords);
        }
    }

    /**
     * 显示指定记录的信息窗口
     * @param record 要显示信息的记录
     */
    private void showRecordInfoWindow(BirdRecord record) {
        View infoView = LayoutInflater.from(getContext()).inflate(R.layout.info_window_record, null);
        TextView tvTitle = infoView.findViewById(R.id.tv_info_title);
        TextView tvBirdName = infoView.findViewById(R.id.tv_info_bird_name);
        TextView tvDateLocation = infoView.findViewById(R.id.tv_info_date_location);

        tvTitle.setText(record.getTitle());
        tvBirdName.setText("鸟类: " + record.getBirdName());
        String date = record.getRecordDate() != null ? dateFormat.format(record.getRecordDate()) : "未知日期";
        String location = !TextUtils.isEmpty(record.getDetailedLocation()) ? record.getDetailedLocation() : "未知地点";
        tvDateLocation.setText(date + " | " + location);

        LatLng position = new LatLng(record.getLatitude(), record.getLongitude());
        mInfoWindow = new InfoWindow(infoView, position, -150); // y偏移量，防止挡住标记
        mBaiduMap.showInfoWindow(mInfoWindow);

        // 平移地图以确保信息窗口可见
        panToRecord(record, false);
    }

    /**
     * 移动地图到指定记录的位置
     * @param record 目标记录
     * @param showInfoWindow 是否在移动后显示信息窗口
     */
    private void panToRecord(BirdRecord record, boolean showInfoWindow) {
        if (record == null || Double.isNaN(record.getLatitude()) || Double.isNaN(record.getLongitude())) return;
        LatLng position = new LatLng(record.getLatitude(), record.getLongitude());
        MapStatusUpdate update = MapStatusUpdateFactory.newLatLngZoom(position, 17f); // 缩放到一个较近的级别
        mBaiduMap.animateMapStatus(update);

        if (showInfoWindow) {
            // 延迟显示，确保地图移动动画流畅
            mainHandler.postDelayed(() -> showRecordInfoWindow(record), 400);
        }
    }

    /**
     * 调整地图视野以包含所有记录
     * @param records 记录列表
     */
    private void zoomToFitRecords(List<BirdRecord> records) {
        if (records == null || records.size() < 2) {
            // 如果只有一个或没有记录，就移动到那一个记录的位置
            if (records != null && records.size() == 1) {
                panToRecord(records.get(0), false);
            }
            return;
        }

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (BirdRecord record : records) {
            builder.include(new LatLng(record.getLatitude(), record.getLongitude()));
        }
        LatLngBounds bounds = builder.build();
        MapStatusUpdate update = MapStatusUpdateFactory.newLatLngBounds(bounds,
                mMapView.getWidth() - 200, mMapView.getHeight() - 400); // 留出边距
        mBaiduMap.animateMapStatus(update);
    }

    // --- 定位相关方法 ---

    private void initLocation() {
        checkLocationPermission();
        try {
            LocationClient.setAgreePrivacy(true);
            mLocationClient = new LocationClient(requireContext().getApplicationContext());
            mLocationClient.registerLocationListener(mLocationListener);
            LocationClientOption option = new LocationClientOption();
            option.setOpenGps(true);
            option.setCoorType("bd09ll");
            option.setScanSpan(5000); // 5秒更新一次位置
            option.setIsNeedAddress(true);
            mLocationClient.setLocOption(option);
        } catch (Exception e) {
            Log.e(TAG, "Failed to init Baidu Location client", e);
        }
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            hasLocationPermission = true;
            startLocation();
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                hasLocationPermission = true;
                startLocation();
            } else {
                hasLocationPermission = false;
                Toast.makeText(getContext(), "定位权限被拒绝", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startLocation() {
        if (mLocationClient != null && hasLocationPermission && !mLocationClient.isStarted()) {
            mLocationClient.start();
        }
    }

    private void requestLocationUpdate() {
        if (mLocationClient != null && hasLocationPermission) {
            isFirstLocate = true; // 强制更新地图中心
            mLocationClient.requestLocation();
            Toast.makeText(getContext(), "正在定位...", Toast.LENGTH_SHORT).show();
        } else if (!hasLocationPermission) {
            checkLocationPermission();
        }
    }

    private final BDAbstractLocationListener mLocationListener = new BDAbstractLocationListener() {
        @Override
        public void onReceiveLocation(BDLocation location) {
            if (location == null || mMapView == null) {
                return;
            }
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    .direction(location.getDirection())
                    .latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            mBaiduMap.setMyLocationData(locData);

            if (isFirstLocate) {
                isFirstLocate = false;
                LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
                MapStatusUpdate u = MapStatusUpdateFactory.newLatLngZoom(ll, 16f);
                mBaiduMap.animateMapStatus(u);
            }
        }
    };


    // --- Fragment生命周期管理 ---

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
        startLocation(); // 页面可见时启动定位
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
        if (mLocationClient != null && mLocationClient.isStarted()) {
            mLocationClient.stop(); // 页面不可见时停止定位，节省电量
        }
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 销毁地图和定位资源
        if (mLocationClient != null) {
            mLocationClient.unRegisterLocationListener(mLocationListener);
            mLocationClient.stop();
        }
        mBaiduMap.setMyLocationEnabled(false);
        if (recordMarkerIcon != null) {
            recordMarkerIcon.recycle();
        }
        mMapView.onDestroy();
        mMapView = null;
    }
}