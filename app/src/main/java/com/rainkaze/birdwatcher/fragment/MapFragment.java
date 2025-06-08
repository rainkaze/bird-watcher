package com.rainkaze.birdwatcher.fragment;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
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
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.model.LatLngBounds;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchResult;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.rainkaze.birdwatcher.R;
import com.rainkaze.birdwatcher.adapter.PoiResultAdapter;
import com.rainkaze.birdwatcher.adapter.RecordSearchAdapter;
import com.rainkaze.birdwatcher.db.BirdRecordDao;
import com.rainkaze.birdwatcher.model.BirdRecord;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class MapFragment extends Fragment implements OnGetPoiSearchResultListener, BaiduMap.OnMapLoadedCallback {

    private static final String TAG = "MapFragment";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private enum SearchMode { RECORDS, LOCATION }
    private SearchMode currentSearchMode = SearchMode.RECORDS;

    private MapView mMapView;
    private BaiduMap mBaiduMap;
    private FloatingActionButton btnMyLocation, fabToggleRecords;
    private SearchView searchView;
    private ImageView ivSearchModeToggle;
    private RecyclerView rvSearchResults;
    private InfoWindow mInfoWindow;

    private BirdRecordDao birdRecordDao;
    private List<BirdRecord> allRecordsWithLocation = new ArrayList<>();
    private List<BirdRecord> displayedRecords = new ArrayList<>();
    private RecordSearchAdapter recordSearchAdapter;
    private PoiResultAdapter poiResultAdapter;
    private List<PoiInfo> poiResultsList = new ArrayList<>();

    private PoiSearch mPoiSearch;
    private LocationClient mLocationClient;
    private boolean isFirstLocate = true;
    private boolean areRecordsVisible = true;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        birdRecordDao = new BirdRecordDao(requireContext());
        mPoiSearch = PoiSearch.newInstance();
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
        updateSearchUI();
    }

    private void initViews(View view) {
        mMapView = view.findViewById(R.id.bmapView);
        btnMyLocation = view.findViewById(R.id.btn_my_location);
        fabToggleRecords = view.findViewById(R.id.fab_toggle_records);
        searchView = view.findViewById(R.id.sv_map_search);
        ivSearchModeToggle = view.findViewById(R.id.iv_search_mode_toggle);
        rvSearchResults = view.findViewById(R.id.rv_map_search_results);
    }

    private void initMap() {
        mBaiduMap = mMapView.getMap();
        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        mBaiduMap.setMyLocationEnabled(true);

        recordSearchAdapter = new RecordSearchAdapter(displayedRecords, this::onRecordSearchResultClick);
        poiResultAdapter = new PoiResultAdapter(poiResultsList, this::onPoiSearchResultClick);

        rvSearchResults.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSearchResults.setAdapter(recordSearchAdapter);
    }

    private void setupListeners() {
        mBaiduMap.setOnMapLoadedCallback(this);
        mPoiSearch.setOnGetPoiSearchResultListener(this);

        btnMyLocation.setOnClickListener(v -> {
            requestLocationUpdate();
            panToMyLocation();
        });

        fabToggleRecords.setOnClickListener(v -> toggleRecordMarkersVisibility());

        ivSearchModeToggle.setOnClickListener(v -> toggleSearchMode());

        mBaiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                mBaiduMap.hideInfoWindow();
                hideSearchResults();
            }
            @Override
            public void onMapPoiClick(MapPoi mapPoi) {}
        });

        mBaiduMap.setOnMarkerClickListener(marker -> {
            Bundle bundle = marker.getExtraInfo();
            if (bundle != null) {
                long recordId = bundle.getLong("record_id", -1);
                if (recordId != -1) {
                    allRecordsWithLocation.stream()
                            .filter(r -> r.getId() == recordId)
                            .findFirst()
                            .ifPresent(this::showRecordInfoWindow);
                }
            }
            return true;
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (TextUtils.isEmpty(query)) return true;
                if (currentSearchMode == SearchMode.RECORDS) {
                    performRecordSearch(query);
                } else {
                    performLocationSearch(query);
                }
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (TextUtils.isEmpty(newText)) {
                    hideSearchResults();
                    if(currentSearchMode == SearchMode.RECORDS){
                        updateMapWithRecordsAndZoom(allRecordsWithLocation);
                    }
                }
                return true;
            }
        });
    }

    @Override
    public void onMapLoaded() {
        Log.d(TAG, "Map has been loaded. Loading records...");
        loadRecordsForMap();
    }

    private void toggleRecordMarkersVisibility() {
        areRecordsVisible = !areRecordsVisible;
        if (areRecordsVisible) {
            fabToggleRecords.setImageResource(R.drawable.ic_layers);
            addRecordMarkersToMap(allRecordsWithLocation);
            Toast.makeText(getContext(), "记录已显示", Toast.LENGTH_SHORT).show();
        } else {
            fabToggleRecords.setImageResource(R.drawable.ic_layers_off);
            mBaiduMap.clear();
            Toast.makeText(getContext(), "记录已隐藏", Toast.LENGTH_SHORT).show();
        }
    }


    private void toggleSearchMode() {
        currentSearchMode = (currentSearchMode == SearchMode.RECORDS) ? SearchMode.LOCATION : SearchMode.RECORDS;
        updateSearchUI();
        hideSearchResults();
    }

    private void updateSearchUI() {
        if (currentSearchMode == SearchMode.RECORDS) {
            ivSearchModeToggle.setImageResource(R.drawable.ic_bird);
            searchView.setQueryHint(getString(R.string.search_birds));
        } else {
            ivSearchModeToggle.setImageResource(R.drawable.ic_location);
            searchView.setQueryHint(getString(R.string.search_location));
        }
        searchView.setQuery("", false);
    }

    private void loadRecordsForMap() {
        birdRecordDao.open();
        List<BirdRecord> allRecords = birdRecordDao.getAllRecords();
        birdRecordDao.close();

        allRecordsWithLocation = allRecords.stream()
                .filter(r -> !Double.isNaN(r.getLatitude()) && !Double.isNaN(r.getLongitude()))
                .collect(Collectors.toList());
        Log.d(TAG, "Loaded " + allRecordsWithLocation.size() + " records with location.");

        if (areRecordsVisible) {
            updateMapWithRecordsAndZoom(allRecordsWithLocation);
        }
    }

    private void performRecordSearch(String query) {
        birdRecordDao.open();
        List<BirdRecord> searchResults = birdRecordDao.searchRecords(query);
        birdRecordDao.close();

        List<BirdRecord> filteredResults = searchResults.stream()
                .filter(r -> !Double.isNaN(r.getLatitude()) && !Double.isNaN(r.getLongitude()))
                .collect(Collectors.toCollection(ArrayList::new));

        displayedRecords.clear();
        displayedRecords.addAll(filteredResults);
        recordSearchAdapter.notifyDataSetChanged();

        updateMapWithRecordsAndZoom(filteredResults);

        rvSearchResults.setAdapter(recordSearchAdapter);
        rvSearchResults.setVisibility(filteredResults.isEmpty() ? View.GONE : View.VISIBLE);
    }


    private void performLocationSearch(String keyword) {
        String city = "全国";
        BDLocation lastLocation = mLocationClient.getLastKnownLocation();
        if (lastLocation != null && lastLocation.getCity() != null) {
            city = lastLocation.getCity();
        }
        mPoiSearch.searchInCity(new PoiCitySearchOption().city(city).keyword(keyword).scope(2));
    }

    private void addRecordMarkersToMap(List<BirdRecord> records) {
        if (records == null) return;
        for (BirdRecord record : records) {
            createCustomMarker(record);
        }
    }

    private void updateMapWithRecordsAndZoom(List<BirdRecord> records) {
        mBaiduMap.clear();
        this.displayedRecords.clear();
        if (records != null) {
            this.displayedRecords.addAll(records);
            addRecordMarkersToMap(records);
            zoomToFitRecords(records);
        }
        recordSearchAdapter.notifyDataSetChanged();
    }

    private void createCustomMarker(final BirdRecord record) {
        if (getContext() == null) return;

        View markerView = LayoutInflater.from(getContext()).inflate(R.layout.view_custom_marker, null);
        ImageView ivThumbnail = markerView.findViewById(R.id.iv_marker_thumbnail);
        TextView tvTitle = markerView.findViewById(R.id.tv_marker_title);
        tvTitle.setText(record.getBirdName());

        if (record.getPhotoUris() != null && !record.getPhotoUris().isEmpty()) {
            final int widthInPx = (int) (90 * getResources().getDisplayMetrics().density);
            final int heightInPx = (int) (65 * getResources().getDisplayMetrics().density);

            Glide.with(getContext().getApplicationContext())
                    .asBitmap()
                    .load(Uri.parse(record.getPhotoUris().get(0)))
                    .placeholder(R.drawable.ic_bird_default)
                    .error(R.drawable.ic_picture_error)
                    .override(widthInPx, heightInPx)
                    .centerCrop()
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            ivThumbnail.setImageBitmap(resource);
                            addMarkerToMap(record, markerView);
                        }
                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) { }
                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            super.onLoadFailed(errorDrawable);
                            ivThumbnail.setImageDrawable(errorDrawable);
                            addMarkerToMap(record, markerView);
                        }
                    });
        } else {
            ivThumbnail.setImageResource(R.drawable.ic_bird_default);
            addMarkerToMap(record, markerView);
        }
    }


    private void addMarkerToMap(BirdRecord record, View markerView) {
        markerView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        markerView.layout(0, 0, markerView.getMeasuredWidth(), markerView.getMeasuredHeight());

        BitmapDescriptor descriptor = BitmapDescriptorFactory.fromView(markerView);

        if (descriptor == null) {
            Log.e(TAG, "Failed to create BitmapDescriptor from view for record: " + record.getTitle());
            descriptor = BitmapDescriptorFactory.fromResource(R.drawable.ic_bird_default);
        }
        LatLng position = new LatLng(record.getLatitude(), record.getLongitude());
        MarkerOptions options = new MarkerOptions().position(position).icon(descriptor).anchor(0.5f, 1.0f);

        Marker marker = (Marker) mBaiduMap.addOverlay(options);
        if (marker != null) {
            Bundle bundle = new Bundle();
            bundle.putLong("record_id", record.getId());
            marker.setExtraInfo(bundle);
        }
    }

    // 修改点: 核心逻辑修改，填充更详细信息并修复图片尺寸
    private void showRecordInfoWindow(final BirdRecord record) {
        final LatLng position = new LatLng(record.getLatitude(), record.getLongitude());
        View infoView = LayoutInflater.from(getContext()).inflate(R.layout.info_window_record, null, false);

        // 获取所有视图
        ImageView ivThumbnail = infoView.findViewById(R.id.iv_info_thumbnail);
        TextView tvTitle = infoView.findViewById(R.id.tv_info_title);
        TextView tvBirdName = infoView.findViewById(R.id.tv_info_bird_name);
        TextView tvContent = infoView.findViewById(R.id.tv_info_content); // 新增
        TextView tvDateLocation = infoView.findViewById(R.id.tv_info_date_location);

        // 填充标题
        tvTitle.setText(record.getTitle());

        // 填充鸟名和学名
        String birdNameText = record.getBirdName();
        if (!TextUtils.isEmpty(record.getScientificName())) {
            birdNameText += " (" + record.getScientificName() + ")";
        }
        tvBirdName.setText(birdNameText);

        // 填充详细内容
        if (!TextUtils.isEmpty(record.getContent())) {
            tvContent.setVisibility(View.VISIBLE);
            tvContent.setText(record.getContent());
        } else {
            tvContent.setVisibility(View.GONE);
        }

        // 填充日期和地点
        String date = record.getRecordDate() != null ? dateFormat.format(record.getRecordDate()) : "未知日期";
        String location = TextUtils.isEmpty(record.getDetailedLocation()) ? "未知地点" : record.getDetailedLocation();
        tvDateLocation.setText(date + " | " + location);

        // 准备一个在图片加载完成后显示InfoWindow的动作
        Runnable showInfoWindowRunnable = () -> {
            mBaiduMap.hideInfoWindow();
            mInfoWindow = new InfoWindow(BitmapDescriptorFactory.fromView(infoView), position, -200, null);
            mBaiduMap.showInfoWindow(mInfoWindow);
        };

        // 加载图片
        if (record.getPhotoUris() != null && !record.getPhotoUris().isEmpty()) {
            ivThumbnail.setVisibility(View.VISIBLE);

            // 将dp尺寸转换为像素
            final int widthInPx = (int) (250 * getResources().getDisplayMetrics().density);
            final int heightInPx = (int) (120 * getResources().getDisplayMetrics().density);

            Glide.with(requireContext())
                    .asBitmap()
                    .load(Uri.parse(record.getPhotoUris().get(0)))
                    .override(widthInPx, heightInPx) // **修改点: 明确尺寸**
                    .centerCrop()                    // **修改点: 让Glide裁剪**
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_picture_error)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            ivThumbnail.setImageBitmap(resource);
                            showInfoWindowRunnable.run();
                        }
                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) { }
                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            ivThumbnail.setVisibility(View.GONE);
                            showInfoWindowRunnable.run();
                        }
                    });
        } else {
            ivThumbnail.setVisibility(View.GONE);
            showInfoWindowRunnable.run();
        }
        panToRecord(record);
    }



    private void hideSearchResults() {
        if(rvSearchResults != null) {
            rvSearchResults.setVisibility(View.GONE);
        }
        if (searchView != null) {
            searchView.clearFocus();
        }
    }

    private void onRecordSearchResultClick(BirdRecord record) {
        hideSearchResults();
        mainHandler.post(() -> {
            panToRecord(record);
            mainHandler.postDelayed(() -> showRecordInfoWindow(record), 500);
        });
    }

    private void onPoiSearchResultClick(PoiInfo poi) {
        hideSearchResults();
        if (poi.location != null) {
            MapStatusUpdate update = MapStatusUpdateFactory.newLatLngZoom(poi.location, 17f);
            mBaiduMap.animateMapStatus(update);
        }
    }

    @Override
    public void onGetPoiResult(PoiResult poiResult) {
        poiResultsList.clear();
        if (poiResult != null && poiResult.error == SearchResult.ERRORNO.NO_ERROR) {
            if (poiResult.getAllPoi() != null) {
                poiResultsList.addAll(poiResult.getAllPoi());
            }
        }

        if (poiResultsList.isEmpty()) {
            Toast.makeText(getContext(), "未找到相关地点", Toast.LENGTH_SHORT).show();
            rvSearchResults.setVisibility(View.GONE);
        } else {
            rvSearchResults.setAdapter(poiResultAdapter);
            poiResultAdapter.notifyDataSetChanged();
            rvSearchResults.setVisibility(View.VISIBLE);
        }
    }
    @Override
    public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {}
    @Override
    public void onGetPoiDetailResult(PoiDetailSearchResult poiDetailSearchResult) {}
    @Override
    public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {}

    private void panToRecord(BirdRecord record) {
        if (record == null || Double.isNaN(record.getLatitude()) || Double.isNaN(record.getLongitude())) return;
        LatLng position = new LatLng(record.getLatitude(), record.getLongitude());
        MapStatusUpdate update = MapStatusUpdateFactory.newLatLng(position);
        mBaiduMap.animateMapStatus(update);
    }

    private void zoomToFitRecords(List<BirdRecord> records) {
        if (mMapView == null || records == null || records.isEmpty()) {
            return;
        }

        if (records.size() == 1) {
            LatLng position = new LatLng(records.get(0).getLatitude(), records.get(0).getLongitude());
            mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLngZoom(position, 16f));
            return;
        }

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (BirdRecord record : records) {
            builder.include(new LatLng(record.getLatitude(), record.getLongitude()));
        }
        try {
            LatLngBounds bounds = builder.build();
            MapStatusUpdate update = MapStatusUpdateFactory.newLatLngBounds(bounds, mMapView.getWidth() - 150, mMapView.getHeight() - 150,
                    mMapView.getWidth() - 150, mMapView.getHeight() - 150);
            mBaiduMap.animateMapStatus(update);
        } catch(Exception e) {
            Log.e(TAG, "Error zooming to fit records", e);
        }
    }

    private void initLocation() {
        try {
            LocationClient.setAgreePrivacy(true);
            mLocationClient = new LocationClient(requireContext().getApplicationContext());
            mLocationClient.registerLocationListener(mLocationListener);
            LocationClientOption option = new LocationClientOption();
            option.setOpenGps(true);
            option.setCoorType("bd09ll");
            option.setScanSpan(0);
            option.setIsNeedAddress(true);
            mLocationClient.setLocOption(option);
        } catch (Exception e) {
            Log.e(TAG, "Failed to init Baidu Location client", e);
        }
    }

    private void requestLocationUpdate() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        if (mLocationClient != null && !mLocationClient.isStarted()) {
            mLocationClient.start();
        } else if (mLocationClient != null) {
            mLocationClient.requestLocation();
        }
    }

    private void panToMyLocation() {
        MyLocationData myLocationData = mBaiduMap.getLocationData();
        if (myLocationData != null && myLocationData.latitude != 0.0 && myLocationData.longitude != 0.0) {
            LatLng latLng = new LatLng(myLocationData.latitude, myLocationData.longitude);
            MapStatusUpdate u = MapStatusUpdateFactory.newLatLngZoom(latLng, 17f);
            mBaiduMap.animateMapStatus(u);
        } else {
            Toast.makeText(getContext(), "正在获取您的位置...", Toast.LENGTH_SHORT).show();
            isFirstLocate = true;
            requestLocationUpdate();
        }
    }


    private final BDAbstractLocationListener mLocationListener = new BDAbstractLocationListener() {
        @Override
        public void onReceiveLocation(BDLocation location) {
            if (location == null || mMapView == null) return;
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    .latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            mBaiduMap.setMyLocationData(locData);

            if (isFirstLocate) {
                isFirstLocate = false;
                if(location.getLatitude() != 0.0 && location.getLongitude() != 0.0){
                    MapStatusUpdate u = MapStatusUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 16f);
                    mBaiduMap.animateMapStatus(u);
                }
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mLocationClient.start();
                panToMyLocation();
            } else {
                Toast.makeText(getContext(), "定位权限被拒绝", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationClient.start();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
        mLocationClient.stop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mPoiSearch.destroy();
        mLocationClient.unRegisterLocationListener(mLocationListener);
        mLocationClient.stop();
        mBaiduMap.setMyLocationEnabled(false);
        mMapView.onDestroy();
        mMapView = null;
    }
}