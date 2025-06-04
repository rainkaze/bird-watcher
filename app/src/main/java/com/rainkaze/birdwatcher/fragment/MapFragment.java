//package com.rainkaze.birdwatcher.fragment;
//
//import android.os.Bundle;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.TextView;
//
//import androidx.fragment.app.Fragment;
//
//import com.baidu.mapapi.map.BaiduMap;
//import com.baidu.mapapi.map.BitmapDescriptor;
//import com.baidu.mapapi.map.BitmapDescriptorFactory;
//import com.baidu.mapapi.map.InfoWindow;
//import com.baidu.mapapi.map.MapPoi;
//import com.baidu.mapapi.map.MapStatusUpdateFactory;
//import com.baidu.mapapi.map.MapView;
//import com.baidu.mapapi.map.Marker;
//import com.baidu.mapapi.map.MarkerOptions;
//import com.baidu.mapapi.map.OverlayOptions;
//import com.baidu.mapapi.model.LatLng;
//import com.rainkaze.birdwatcher.R;
//import com.rainkaze.birdwatcher.model.BirdLocation;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class MapFragment extends Fragment {
//
//    private MapView mMapView;
//    private BaiduMap mBaiduMap;
//    private List<BirdLocation> birdLocations = new ArrayList<>();
//    private InfoWindow mInfoWindow;
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.fragment_map, container, false);
//
//        // 初始化地图
//        mMapView = view.findViewById(R.id.bmapView);
//        mBaiduMap = mMapView.getMap();
//        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
//
//        // 设置地图中心点（示例位置）
//        LatLng center = new LatLng(39.915071, 116.403907); // 北京天安门
//        mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLngZoom(center, 15));
//
//        // 初始化鸟类位置数据
//        initBirdLocations();
//
//        // 添加鸟类标记
//        addBirdMarkers();
//
//        // 设置标记点击监听
//        setMarkerClickListener();
//
//        // 设置地图点击监听（关闭信息窗口）
//        setMapClickListener();
//
//        return view;
//    }
//
//    private void initBirdLocations() {
//        // 添加示例数据
//        birdLocations.add(new BirdLocation("红腹锦鸡", "国家一级保护动物，羽毛艳丽", 39.915071, 116.403907, 95));
//        birdLocations.add(new BirdLocation("白鹭", "常见于水边，全身白色羽毛", 39.925071, 116.413907, 85));
//        birdLocations.add(new BirdLocation("翠鸟", "小型鸟类，羽毛呈翠蓝色", 39.905071, 116.393907, 78));
//        birdLocations.add(new BirdLocation("喜鹊", "常见城市鸟类，黑白相间", 39.935071, 116.423907, 65));
//    }
//
//    private void addBirdMarkers() {
//        for (BirdLocation bird : birdLocations) {
//            // 创建位置点
//            LatLng point = new LatLng(bird.getLatitude(), bird.getLongitude());
//
//            // 根据人气值设置不同图标
//            BitmapDescriptor bitmap;
//            if (bird.getPopularity() > 90) {
//                bitmap = BitmapDescriptorFactory.fromResource(R.drawable.ic_bird_hot);
//            } else if (bird.getPopularity() > 70) {
//                bitmap = BitmapDescriptorFactory.fromResource(R.drawable.ic_bird_popular);
//            } else {
//                bitmap = BitmapDescriptorFactory.fromResource(R.drawable.ic_bird_normal);
//            }
//
//            // 构建Marker
//            OverlayOptions option = new MarkerOptions()
//                    .position(point)
//                    .icon(bitmap)
//                    .zIndex(9);
//
//            // 添加Marker到地图
//            Marker marker = (Marker) mBaiduMap.addOverlay(option);
//
//            // 创建Bundle并存储鸟类信息
//            Bundle bundle = new Bundle();
//            bundle.putString("name", bird.getName());
//            bundle.putString("description", bird.getDescription());
//            bundle.putInt("popularity", bird.getPopularity());
//            bundle.putDouble("latitude", bird.getLatitude());
//            bundle.putDouble("longitude", bird.getLongitude());
//
//            // 设置额外信息
//            marker.setExtraInfo(bundle);
//        }
//    }
//
//    private void setMarkerClickListener() {
//        mBaiduMap.setOnMarkerClickListener(marker -> {
//            // 获取绑定的鸟类信息Bundle
//            Bundle bundle = marker.getExtraInfo();
//            if (bundle == null) return true;
//
//            // 从Bundle中提取数据
//            String name = bundle.getString("name");
//            String description = bundle.getString("description");
//            int popularity = bundle.getInt("popularity");
//
//            // 创建信息窗口视图
//            View infoView = LayoutInflater.from(getContext()).inflate(R.layout.info_window_bird, null);
//            TextView tvName = infoView.findViewById(R.id.tv_bird_name);
//            TextView tvPopularity = infoView.findViewById(R.id.tv_popularity);
//            TextView tvDescription = infoView.findViewById(R.id.tv_description);
//
//            tvName.setText(name);
//            tvPopularity.setText("人气值: " + popularity + "%");
//            tvDescription.setText(description);
//
//            // 关闭之前的信息窗口
//            if (mInfoWindow != null) {
//                mBaiduMap.hideInfoWindow();
//            }
//
//            // 创建新的信息窗口
//            mInfoWindow = new InfoWindow(infoView, marker.getPosition(), -80);
//            mBaiduMap.showInfoWindow(mInfoWindow);
//
//            return true;
//        });
//    }
//
//    private void setMapClickListener() {
//        mBaiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
//            /**
//             * 地图单击事件回调函数
//             * @param point 点击的地理坐标
//             */
//            @Override
//            public void onMapClick(LatLng point) {
//                // 点击地图关闭信息窗口
//                if (mInfoWindow != null) {
//                    mBaiduMap.hideInfoWindow();
//                    mInfoWindow = null;
//                }
//            }
//
//            /**
//             * 地图内 Poi 单击事件回调函数
//             * @param poi 点击的 poi 信息
//             */
//            @Override
//            public void onMapPoiClick(MapPoi poi) {
//                // 点击POI点也关闭信息窗口
//                if (mInfoWindow != null) {
//                    mBaiduMap.hideInfoWindow();
//                    mInfoWindow = null;
//                }
//            }
//        });
//    }
//
//    @Override
//    public void onResume() {
//        super.onResume();
//        mMapView.onResume();
//    }
//
//    @Override
//    public void onPause() {
//        super.onPause();
//        mMapView.onPause();
//    }
//
//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//        mMapView.onDestroy();
//    }
//}
package com.rainkaze.birdwatcher.fragment;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

        // 设置搜索按钮点击监听
        btnSearch.setOnClickListener(v -> performSearch());

        // 初始化POI搜索
        mPoiSearch = PoiSearch.newInstance();
        mPoiSearch.setOnGetPoiSearchResultListener(this);

        // 设置地图中心点
        LatLng center = new LatLng(39.915071, 116.403907); // 北京天安门
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLngZoom(center, 15));

        // 初始化圆形标记图标（缩小尺寸）
        hotBirdIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_bird_hot_circle);
        popularBirdIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_bird_popular_circle);
        normalBirdIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_bird_normal_circle);

        // 初始化鸟类位置数据
        initBirdLocations();

        // 添加鸟类标记
        addBirdMarkers();

        // 设置标记点击监听
        setMarkerClickListener();

        // 设置地图点击监听
        setMapClickListener();

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
        int iconResId;
        if (bird.getPopularity() > 90) {
            iconResId = R.drawable.ic_bird_hot_circle;
        } else if (bird.getPopularity() > 70) {
            iconResId = R.drawable.ic_bird_popular_circle;
        } else {
            iconResId = R.drawable.ic_bird_normal_circle;
        }

        // 创建圆形标记位图
        BitmapDescriptor bitmap = createCircularMarker(iconResId, bird.getPopularity());

        // 构建Marker
        OverlayOptions option = new MarkerOptions()
                .position(point)
                .icon(bitmap)
                .zIndex(9);

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
    private BitmapDescriptor createCircularMarker(int iconResId, int popularity) {
        // 1. 创建圆形背景
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);

        // 根据人气值设置不同背景色
        if (popularity > 90) {
            background.setColor(ContextCompat.getColor(requireContext(), R.color.bird_hot));
        } else if (popularity > 70) {
            background.setColor(ContextCompat.getColor(requireContext(), R.color.bird_popular));
        } else {
            background.setColor(ContextCompat.getColor(requireContext(), R.color.bird_normal));
        }

        // 2. 获取图标
        Drawable iconDrawable = ContextCompat.getDrawable(requireContext(), iconResId);
        if (iconDrawable != null) {
            iconDrawable.setTint(Color.WHITE); // 设置图标为白色
        }

        // 3. 创建图层列表
        LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{background, iconDrawable});

        // 设置图标位置居中
        if (iconDrawable != null) {
            int inset = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics());
            layerDrawable.setLayerInset(1, inset, inset, inset, inset);
        }

        // 4. 转换为位图
        int size = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 48, getResources().getDisplayMetrics());

        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        layerDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        layerDrawable.draw(canvas);

        // 5. 创建BitmapDescriptor
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    // 调整地图视野以显示所有匹配的鸟类
    private void zoomToFilteredBirds() {
        if (filteredBirds.isEmpty()) return;

        // 使用百度地图SDK的LatLngBounds.Builder（推荐方式）
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (BirdLocation bird : filteredBirds) {
            builder.include(new LatLng(bird.getLatitude(), bird.getLongitude()));
        }

        try {
            LatLngBounds bounds = builder.build();
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
        Animation anim = new ScaleAnimation(
                1.0f, 1.3f, // X轴缩放
                1.0f, 1.3f, // Y轴缩放
                Animation.RELATIVE_TO_SELF, 0.5f, // 缩放中心X
                Animation.RELATIVE_TO_SELF, 0.5f); // 缩放中心Y

        anim.setDuration(100);
        anim.setRepeatCount(1);
        anim.setRepeatMode(Animation.REVERSE);

        // 在动画结束后恢复原始状态
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                marker.setIcon(getMarkerIconForBird(marker));
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        // 暂时改变图标大小以配合动画
        BitmapDescriptor originalIcon = marker.getIcon();
        BitmapDescriptor scaledIcon = getScaledIcon(originalIcon, 1.3f);
        marker.setIcon(scaledIcon);
    }

    // 获取缩放后的图标
    private BitmapDescriptor getScaledIcon(BitmapDescriptor original, float scale) {
        // 在实际应用中，您需要创建一个缩放后的Bitmap
        // 这里为了简化，我们直接返回原始图标
        return original;
    }

    // 根据鸟类信息获取图标
    private BitmapDescriptor getMarkerIconForBird(Marker marker) {
        Bundle bundle = marker.getExtraInfo();
        if (bundle == null) return normalBirdIcon;

        int popularity = bundle.getInt("popularity");
        if (popularity > 90) {
            return hotBirdIcon;
        } else if (popularity > 70) {
            return popularBirdIcon;
        } else {
            return normalBirdIcon;
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

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        mPoiSearch.destroy();
    }
}