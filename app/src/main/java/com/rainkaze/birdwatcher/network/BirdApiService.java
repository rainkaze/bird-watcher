package com.rainkaze.birdwatcher.network;

import com.rainkaze.birdwatcher.model.Bird;
import com.rainkaze.birdwatcher.model.BirdDescription;
import com.rainkaze.birdwatcher.model.BirdMedia;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface BirdApiService {
    // 获取区域内的鸟类观察记录
    @GET("data/obs/{regionCode}/recent")
    Call<List<Bird>> getRecentObservations(
            @Path("regionCode") String regionCode,
            @Query("back") int daysBack,
            @Query("maxResults") int maxResults
    );

    // 获取特定鸟类的详细信息
    @GET("product/sppinfo/{speciesCode}")
    Call<Bird> getSpeciesInfo(
            @Path("speciesCode") String speciesCode
    );

    // 获取鸟类分类信息
    @GET("ref/taxonomy/ebird")
    Call<List<Bird>> getTaxonomy(
            @Query("cat") String category,
            @Query("fmt") String format
    );

    // 搜索鸟类 (已修正：使用 @Query("q") 注解)
    @GET("ref/taxon/find")
    Call<List<Bird>> searchBirds(
            @Query("q") String query,
            @Query("locale") String locale
    );

    // 获取鸟类图片 (已修正：使用正确的端点和参数)
    @GET("ref/media/find")
    Call<List<BirdMedia>> getBirdMedia(
            @Query("speciesCode") String speciesCode,
            @Query("mediaType") String mediaType,
            @Query("fmt") String format,
            @Query("locale") String locale,
            @Query("maxResults") int maxResults
    );

    // 获取鸟类描述
    @GET("product/sppinfo/{speciesCode}/descriptions")
    Call<BirdDescription> getBirdDescription(
            @Path("speciesCode") String speciesCode,
            @Query("locale") String locale
    );
}