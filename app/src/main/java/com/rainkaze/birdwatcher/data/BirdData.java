package com.rainkaze.birdwatcher.data;

import com.rainkaze.birdwatcher.R;
import com.rainkaze.birdwatcher.model.zoology.BirdSpecies;

import java.util.ArrayList;
import java.util.List;

public class BirdData {

    // 使用正确的方法名 getBuiltInBirds
    public static List<BirdSpecies> getBuiltInBirds() {
        ArrayList<BirdSpecies> birdList = new ArrayList<>();
        // 格式: 中文名, 学名, R.drawable.图片文件名
        birdList.add(new BirdSpecies("麻雀", "Passer montanus", R.drawable.bird_sparrow));
        birdList.add(new BirdSpecies("喜鹊", "Pica pica", R.drawable.bird_magpie));
        birdList.add(new BirdSpecies("家燕", "Hirundo rustica", R.drawable.bird_swallow));
        birdList.add(new BirdSpecies("白头鹎", "Pycnonotus sinensis", R.drawable.bird_light_vented_bulbul));
        birdList.add(new BirdSpecies("鸳鸯", "Aix galericulata", R.drawable.bird_mandarin_duck));
        birdList.add(new BirdSpecies("红胁蓝尾鸲", "Tarsiger cyanurus", 0)); // 0表示无本地图片
        birdList.add(new BirdSpecies("北红尾鸲", "Phoenicurus auroreus", 0));
        birdList.add(new BirdSpecies("八哥", "Acridotheres cristatellus", 0));
        birdList.add(new BirdSpecies("普通翠鸟", "Alcedo atthis", R.drawable.bird_common_kingfisher));
//        birdList.add(new BirdSpecies("戴胜", "Upupa epops", R.drawable.bird_hoopoe));
//        birdList.add(new BirdSpecies("大山雀", "Parus major", R.drawable.bird_great_tit));
//        birdList.add(new BirdSpecies("红隼", "Falco tinnunculus", R.drawable.bird_eurasian_kestrel));
//        birdList.add(new BirdSpecies("白鹭", "Egretta garzetta", 0));
//        birdList.add(new BirdSpecies("珠颈斑鸠", "Spilopelia chinensis", 0));
//        birdList.add(new BirdSpecies("大斑啄木鸟", "Dendrocopos major", R.drawable.bird_great_spotted_woodpecker));

        return birdList;
    }
}