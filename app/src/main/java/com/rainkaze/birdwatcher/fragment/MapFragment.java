package com.rainkaze.birdwatcher.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.Overlay;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.model.LatLngBounds;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchResult;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.rainkaze.birdwatcher.R;
import com.rainkaze.birdwatcher.model.BirdLocation;
import com.rainkaze.birdwatcher.adapter.BirdSearchAdapter;

import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment implements OnGetPoiSearchResultListener {

    private MapView mMapView;
    private BaiduMap mBaiduMap;
    private List<BirdLocation> birdLocations = new ArrayList<>();
    private List<BirdLocation> filteredBirds = new ArrayList<>();
    private InfoWindow mInfoWindow;

    // 搜索相关组件
    private TextInputEditText etSearch;
    private MaterialButton btnSearch;
    private ImageView btnSearchType;
    private TextView tvSearchMode;
    private RecyclerView rvBirdResults;

    // 搜索模式（位置搜索或鸟类搜索）
    private boolean isBirdSearchMode = false;
    private PoiSearch mPoiSearch;

    // 标记图标
    private BitmapDescriptor hotBirdIcon;
    private BitmapDescriptor popularBirdIcon;
    private BitmapDescriptor normalBirdIcon;
    private BitmapDescriptor mCurrentLocationIcon;

    // 定位相关变量
    private LocationClient mLocationClient;
    private MyLocationConfiguration.LocationMode mCurrentMode = MyLocationConfiguration.LocationMode.NORMAL;
    private boolean isFirstLocate = true;
    private FloatingActionButton btnMyLocation;
    private BDAbstractLocationListener locationListener;
    private Handler handler = new Handler();
    private boolean locationPermissionGranted = false;

    @SuppressLint("WrongViewCast")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        // 初始化地图
        mMapView = view.findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();
        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);

        // 初始化搜索组件
        etSearch = view.findViewById(R.id.et_search);
        btnSearch = view.findViewById(R.id.btn_search);
        btnSearchType = view.findViewById(R.id.btn_search_type);
        tvSearchMode = view.findViewById(R.id.tv_search_mode);
        rvBirdResults = view.findViewById(R.id.rv_bird_results);

        // 设置搜索图标点击监听
        btnSearchType.setOnClickListener(v -> toggleSearchMode());

        // 添加定位按钮
        btnMyLocation = view.findViewById(R.id.btn_my_location);
        btnMyLocation.setOnClickListener(v -> locateToMyPosition());

        // 设置搜索按钮点击监听
        btnSearch.setOnClickListener(v -> performSearch());

        // 初始化POI搜索
        mPoiSearch = PoiSearch.newInstance();
        mPoiSearch.setOnGetPoiSearchResultListener(this);

        // 初始化圆形标记图标
        hotBirdIcon = createCircularIcon(R.drawable.ic_bird_hot_circle, 48, "#FF5722");
        popularBirdIcon = createCircularIcon(R.drawable.ic_bird_popular_circle, 48, "#4CAF50");
        normalBirdIcon = createCircularIcon(R.drawable.ic_bird_normal_circle, 48, "#2196F3");
        mCurrentLocationIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_my_location);

        // 初始化鸟类位置数据
        initBirdLocations();
        // 添加鸟类标记
        addBirdMarkers();

        // 设置标记点击监听
        setMarkerClickListener();

        // 设置地图点击监听
        setMapClickListener();

        // 初始化定位服务
        initLocation();

        return view;
    }

    // 切换搜索模式
    private void toggleSearchMode() {
        isBirdSearchMode = !isBirdSearchMode;

        if (isBirdSearchMode) {
            // 鸟类搜索模式
            btnSearchType.setImageResource(R.drawable.ic_bird);
            btnSearchType.setContentDescription(getString(R.string.search_birds));
            etSearch.setHint(R.string.search_birds);
            tvSearchMode.setVisibility(View.VISIBLE);
            rvBirdResults.setVisibility(View.GONE);
        } else {
            // 位置搜索模式
            btnSearchType.setImageResource(R.drawable.ic_location);
            btnSearchType.setContentDescription(getString(R.string.search_location));
            etSearch.setHint(R.string.search_location);
            tvSearchMode.setVisibility(View.GONE);
            rvBirdResults.setVisibility(View.GONE);
        }
    }

    // 执行搜索
    private void performSearch() {
        String query = etSearch.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(getContext(), "请输入搜索内容", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isBirdSearchMode) {
            // 鸟类搜索
            filterBirdsByName(query);
        } else {
            // 位置搜索
            searchLocation(query);
        }
    }

    // 位置搜索
    private void searchLocation(String query) {
        // 在实际应用中，您可以根据用户当前城市或指定城市进行搜索
        mPoiSearch.searchInCity(new PoiCitySearchOption()
                .city("北京") // 替换为当前城市
                .keyword(query)
                .pageNum(0));
    }

    // 鸟类搜索
    private void filterBirdsByName(String name) {
        filteredBirds.clear();

        for (BirdLocation bird : birdLocations) {
            if (bird.getName().toLowerCase().contains(name.toLowerCase())) {
                filteredBirds.add(bird);
            }
        }

        if (filteredBirds.isEmpty()) {
            Toast.makeText(getContext(), "未找到匹配的鸟类", Toast.LENGTH_SHORT).show();
            rvBirdResults.setVisibility(View.GONE);
        } else {
            // 显示搜索结果
            showBirdResults();
            // 更新地图标记
            updateBirdMarkers();
            // 调整地图视野以显示所有匹配的鸟类
            zoomToFilteredBirds();
        }
    }

    // 显示鸟类搜索结果
    private void showBirdResults() {
        rvBirdResults.setVisibility(View.VISIBLE);
        rvBirdResults.setLayoutManager(new LinearLayoutManager(getContext()));
        BirdSearchAdapter adapter = new BirdSearchAdapter(filteredBirds, bird -> {
            // 点击搜索结果时定位到该鸟类位置
            LatLng position = new LatLng(bird.getLatitude(), bird.getLongitude());
            mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLngZoom(position, 16));
            rvBirdResults.setVisibility(View.GONE);
        });
        rvBirdResults.setAdapter(adapter);
    }

    // 更新地图上的鸟类标记
    private void updateBirdMarkers() {
        // 清除所有现有标记
        mBaiduMap.clear();

        // 添加过滤后的鸟类标记
        for (BirdLocation bird : filteredBirds) {
            addBirdMarker(bird);
        }
    }

    // 添加单个鸟类标记
    private void addBirdMarker(BirdLocation bird) {
        LatLng point = new LatLng(bird.getLatitude(), bird.getLongitude());

        // 根据人气值设置不同图标
        BitmapDescriptor bitmap;
        if (bird.getPopularity() > 90) {
            bitmap = hotBirdIcon;
        } else if (bird.getPopularity() > 70) {
            bitmap = popularBirdIcon;
        } else {
            bitmap = normalBirdIcon;
        }

        // 构建Marker
        OverlayOptions option = new MarkerOptions()
                .position(point)
                .icon(bitmap)
                .zIndex(9)
                .animateType(MarkerOptions.MarkerAnimateType.drop);

        // 添加Marker到地图
        Marker marker = (Marker) mBaiduMap.addOverlay(option);

        // 创建Bundle并存储鸟类信息
        Bundle bundle = new Bundle();
        bundle.putString("name", bird.getName());
        bundle.putString("description", bird.getDescription());
        bundle.putInt("popularity", bird.getPopularity());
        bundle.putDouble("latitude", bird.getLatitude());
        bundle.putDouble("longitude", bird.getLongitude());

        // 设置额外信息
        marker.setExtraInfo(bundle);
    }

    // 创建圆形标记
    private BitmapDescriptor createCircularIcon(int iconRes, int sizeDp, String bgColor) {
        // 将dp转换为px
        int sizePx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, sizeDp, getResources().getDisplayMetrics());

        // 创建位图
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // 绘制圆形背景
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.parseColor(bgColor));
        bgPaint.setAntiAlias(true);
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, bgPaint);

        // 绘制图标
        Drawable icon = ContextCompat.getDrawable(requireContext(), iconRes);
        if (icon != null) {
            int iconSize = (int) (sizePx * 0.6); // 图标大小为背景的60%
            int left = (sizePx - iconSize) / 2;
            int top = (sizePx - iconSize) / 2;
            int right = left + iconSize;
            int bottom = top + iconSize;

            icon.setBounds(left, top, right, bottom);
            icon.draw(canvas);
        }

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    // 调整地图视野以显示所有匹配的鸟类
    private void zoomToFilteredBirds() {
        if (filteredBirds.isEmpty()) return;

        // 使用百度地图SDK的LatLngBounds.Builder
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (BirdLocation bird : filteredBirds) {
            builder.include(new LatLng(bird.getLatitude(), bird.getLongitude()));
        }

        try {
            LatLngBounds bounds = builder.build();
            // 使用不带边距参数的newLatLngBounds方法
            MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newLatLngBounds(bounds);
            mBaiduMap.setMapStatus(mapStatusUpdate);
        } catch (Exception e) {
            // 备用方案：计算中心点和缩放级别
            double minLat = filteredBirds.get(0).getLatitude();
            double maxLat = minLat;
            double minLng = filteredBirds.get(0).getLongitude();
            double maxLng = minLng;

            for (BirdLocation bird : filteredBirds) {
                double lat = bird.getLatitude();
                double lng = bird.getLongitude();
                minLat = Math.min(minLat, lat);
                maxLat = Math.max(maxLat, lat);
                minLng = Math.min(minLng, lng);
                maxLng = Math.max(maxLng, lng);
            }

            LatLng center = new LatLng((minLat + maxLat) / 2, (minLng + maxLng) / 2);
            float zoomLevel = calculateZoomLevel(minLat, maxLat, minLng, maxLng);
            mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLngZoom(center, zoomLevel));
        }
    }

    // 辅助方法：计算合适的缩放级别
    private float calculateZoomLevel(double minLat, double maxLat, double minLng, double maxLng) {
        double latDiff = maxLat - minLat;
        double lngDiff = maxLng - minLng;
        double maxDiff = Math.max(latDiff, lngDiff);

        if (maxDiff > 0.2) return 10.0f;
        else if (maxDiff > 0.1) return 11.0f;
        else if (maxDiff > 0.05) return 12.0f;
        else if (maxDiff > 0.02) return 13.0f;
        else if (maxDiff > 0.01) return 14.0f;
        else if (maxDiff > 0.005) return 15.0f;
        else return 16.0f;
    }

    private void initBirdLocations() {
        // 添加示例数据
        birdLocations.add(new BirdLocation("红腹锦鸡", "国家一级保护动物，羽毛艳丽", 39.915071, 116.403907, 95));
        birdLocations.add(new BirdLocation("白鹭", "常见于水边，全身白色羽毛", 39.925071, 116.413907, 85));
        birdLocations.add(new BirdLocation("翠鸟", "小型鸟类，羽毛呈翠蓝色", 39.905071, 116.393907, 78));
        birdLocations.add(new BirdLocation("喜鹊", "常见城市鸟类，黑白相间", 39.935071, 116.423907, 65));
        birdLocations.add(new BirdLocation("红腹锦鸡", "国家一级保护动物，羽毛艳丽", 39.945071, 116.433907, 92));
        birdLocations.add(new BirdLocation("白鹭", "常见于水边，全身白色羽毛", 39.885071, 116.373907, 82));
    }

    private void addBirdMarkers() {
        for (BirdLocation bird : birdLocations) {
            addBirdMarker(bird);
        }
    }

    private void setMarkerClickListener() {
        mBaiduMap.setOnMarkerClickListener(marker -> {
            // 添加点击动画
            animateMarker(marker);

            // 获取绑定的鸟类信息Bundle
            Bundle bundle = marker.getExtraInfo();
            if (bundle == null) return true;

            // 从Bundle中提取数据
            String name = bundle.getString("name");
            String description = bundle.getString("description");
            int popularity = bundle.getInt("popularity");

            // 创建信息窗口视图
            View infoView = LayoutInflater.from(getContext()).inflate(R.layout.info_window_bird, null);
            TextView tvName = infoView.findViewById(R.id.tv_bird_name);
            TextView tvPopularity = infoView.findViewById(R.id.tv_popularity);
            TextView tvDescription = infoView.findViewById(R.id.tv_description);

            tvName.setText(name);
            tvPopularity.setText("人气值: " + popularity + "%");
            tvDescription.setText(description);

            // 关闭之前的信息窗口
            if (mInfoWindow != null) {
                mBaiduMap.hideInfoWindow();
            }

            // 创建新的信息窗口
            mInfoWindow = new InfoWindow(infoView, marker.getPosition(), -80);
            mBaiduMap.showInfoWindow(mInfoWindow);

            return true;
        });
    }

    // 标记点击动画
    private void animateMarker(Marker marker) {
        // 使用百度地图SDK提供的动画API
        if (marker != null) {
            // 先保存原始图标
            BitmapDescriptor originalIcon = marker.getIcon();

            // 创建一个缩放后的图标
            if (originalIcon != null) {
                Bitmap originalBitmap = originalIcon.getBitmap();
                if (originalBitmap != null) {
                    int width = originalBitmap.getWidth();
                    int height = originalBitmap.getHeight();
                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalBitmap,
                            (int) (width * 1.3), (int) (height * 1.3), true);
                    BitmapDescriptor scaledIcon = BitmapDescriptorFactory.fromBitmap(scaledBitmap);

                    // 设置缩放图标
                    marker.setIcon(scaledIcon);

                    // 使用百度地图SDK的动画
                    MapStatus.Builder builder = new MapStatus.Builder();
                    builder.target(marker.getPosition()).zoom(mBaiduMap.getMapStatus().zoom);
                    mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()), 300);

                    // 使用Handler延迟恢复原始图标
                    handler.postDelayed(() -> {
                        if (marker != null && !marker.isRemoved()) {
                            marker.setIcon(originalIcon);
                        }
                        // 回收临时位图资源
                        if (scaledBitmap != null && !scaledBitmap.isRecycled()) {
                            scaledBitmap.recycle();
                        }
                    }, 300);
                }
            }
        }
    }

    private void setMapClickListener() {
        mBaiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                // 点击地图关闭信息窗口和搜索结果
                if (mInfoWindow != null) {
                    mBaiduMap.hideInfoWindow();
                    mInfoWindow = null;
                }
                rvBirdResults.setVisibility(View.GONE);
            }

            @Override
            public void onMapPoiClick(MapPoi poi) {
                if (mInfoWindow != null) {
                    mBaiduMap.hideInfoWindow();
                    mInfoWindow = null;
                }
                rvBirdResults.setVisibility(View.GONE);
            }
        });
    }

    // POI搜索回调
    @Override
    public void onGetPoiResult(PoiResult poiResult) {
        if (poiResult == null || poiResult.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(getContext(), "未找到相关位置", Toast.LENGTH_SHORT).show();
            return;
        }

        // 获取第一个POI结果
        if (poiResult.getAllPoi() != null && !poiResult.getAllPoi().isEmpty()) {
            LatLng location = poiResult.getAllPoi().get(0).getLocation();
            mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLngZoom(location, 16));
        }
    }

    @Override
    public void onGetPoiDetailResult(PoiDetailSearchResult poiDetailSearchResult) {}

    @Override
    public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {}

    @Override
    public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {}

    // 初始化定位服务
    private void initLocation() {
        try {
            com.baidu.location.LocationClient.setAgreePrivacy(true);

            // 确保只初始化一次
            if (mLocationClient != null) {
                return;
            }

            // 初始化定位客户端
            mLocationClient = new LocationClient(requireContext().getApplicationContext());

            // 使用成员变量存储监听器引用
            locationListener = new BDAbstractLocationListener() {
                @Override
                public void onReceiveLocation(BDLocation location) {
                    if (location == null || mMapView == null) {
                        return;
                    }

                    // 获取位置数据
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();

                    // 创建位置数据
                    MyLocationData locData = new MyLocationData.Builder()
                            .accuracy(location.getRadius())
                            .direction(location.getDirection())
                            .latitude(latitude)
                            .longitude(longitude)
                            .build();

                    // 设置定位数据
                    mBaiduMap.setMyLocationData(locData);

                    // 配置定位图层
                    MyLocationConfiguration config = new MyLocationConfiguration(
                            MyLocationConfiguration.LocationMode.NORMAL,
                            true,
                            mCurrentLocationIcon
                    );
                    mBaiduMap.setMyLocationConfiguration(config);

                    // 如果是首次定位，移动到当前位置
                    if (isFirstLocate) {
                        moveToPosition(new LatLng(latitude, longitude), 16f);
                        isFirstLocate = false;
                    }
                }
            };

            mLocationClient.registerLocationListener(locationListener);

            // 设置定位参数
            LocationClientOption option = new LocationClientOption();
            option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
            option.setCoorType("bd09ll");
            option.setScanSpan(5000);
            option.setIsNeedAddress(true);
            option.setNeedDeviceDirect(true);
            option.setOpenGps(true);
            option.setLocationNotify(true);
            option.setIsNeedLocationDescribe(true);
            mLocationClient.setLocOption(option);

            // 检查定位权限
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
                startLocation();
            } else {
                Log.d("MapFragment", "请求定位权限");
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        } catch (Exception e) {
            Log.e("MapFragment", "定位初始化失败", e);
            Toast.makeText(getContext(), "定位服务初始化失败", Toast.LENGTH_SHORT).show();
        }
    }

    // 开始定位
    private void startLocation() {
        if (mLocationClient != null && locationPermissionGranted) {
            if (!mLocationClient.isStarted()) {
                mLocationClient.start();
            }
            // 立即请求一次位置更新
            mLocationClient.requestLocation();
            // 显示当前位置
            mBaiduMap.setMyLocationEnabled(true);
        }
    }

    // 定位到我的位置
    private void locateToMyPosition() {
        if (!locationPermissionGranted) {
            Toast.makeText(getContext(), "需要位置权限才能使用定位功能", Toast.LENGTH_SHORT).show();
            requestLocationPermission();
            return;
        }

        if (mLocationClient == null) {
            Log.d("MapFragment", "定位客户端未初始化，重新初始化");
            initLocation();
            return;
        }

        if (!mLocationClient.isStarted()) {
            Log.d("MapFragment", "定位服务未启动，正在启动");
            startLocation();
            Toast.makeText(getContext(), "正在启动定位服务...", Toast.LENGTH_SHORT).show();

            // 延迟获取位置，给服务启动时间
            handler.postDelayed(() -> {
                BDLocation location = mLocationClient.getLastKnownLocation();
                if (location != null) {
                    LatLng point = new LatLng(location.getLatitude(), location.getLongitude());
                    moveToPosition(point, 16f);
                    isFirstLocate = false;
                } else {
                    Toast.makeText(getContext(), "正在获取位置...", Toast.LENGTH_SHORT).show();
                    mLocationClient.requestLocation();
                }
            }, 1000); // 延迟1秒
        } else {
            BDLocation location = mLocationClient.getLastKnownLocation();
            if (location != null) {
                LatLng point = new LatLng(location.getLatitude(), location.getLongitude());
                moveToPosition(point, 16f);
                isFirstLocate = false;
            } else {
                Toast.makeText(getContext(), "正在获取位置...", Toast.LENGTH_SHORT).show();
                mLocationClient.requestLocation();
            }
        }
    }

    // 请求定位权限
    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    private void moveToPosition(LatLng position, float zoom) {
        MapStatusUpdate msu = MapStatusUpdateFactory.newLatLngZoom(position, zoom);
        mBaiduMap.animateMapStatus(msu);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MapFragment", "定位权限已授予");
                locationPermissionGranted = true;
                startLocation();
                // 启用定位按钮
                btnMyLocation.setEnabled(true);
                btnMyLocation.setAlpha(1.0f);
            } else {
                Log.d("MapFragment", "定位权限被拒绝");
                Toast.makeText(getContext(), "需要位置权限才能使用定位功能", Toast.LENGTH_LONG).show();
                locationPermissionGranted = false;

                // 禁用定位按钮
                btnMyLocation.setEnabled(false);
                btnMyLocation.setAlpha(0.5f);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();

        // 确保定位服务在 onResume 时启动
        if (mLocationClient != null && locationPermissionGranted) {
            if (!mLocationClient.isStarted()) {
                Log.d("MapFragment", "onResume: 启动定位服务");
                mLocationClient.start();
                mLocationClient.requestLocation();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();

        // 停止定位服务但保持客户端
        if (mLocationClient != null && mLocationClient.isStarted()) {
            Log.d("MapFragment", "onPause: 停止定位服务");
            mLocationClient.stop();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        mPoiSearch.destroy();

        // 释放BitmapDescriptor资源
        if (hotBirdIcon != null) {
            hotBirdIcon.recycle();
            hotBirdIcon = null;
        }
        if (popularBirdIcon != null) {
            popularBirdIcon.recycle();
            popularBirdIcon = null;
        }
        if (normalBirdIcon != null) {
            normalBirdIcon.recycle();
            normalBirdIcon = null;
        }
        if (mCurrentLocationIcon != null) {
            mCurrentLocationIcon.recycle();
            mCurrentLocationIcon = null;
        }

        // 移除所有回调
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }

        // 取消注册监听器
        if (mLocationClient != null && locationListener != null) {
            mLocationClient.unRegisterLocationListener(locationListener);
        }

        if (mLocationClient != null) {
            mLocationClient.stop();
            mLocationClient = null;
        }

        mBaiduMap.setMyLocationEnabled(false);
        mMapView = null;
    }
}