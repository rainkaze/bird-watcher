package com.rainkaze.birdwatcher.model.zoology;

// 用于表示一条完整的描述信息（标题+内容）
public class DescriptionItem {
    private String title;
    private String content;

    public DescriptionItem(String title) {
        this.title = title;
        this.content = "加载中..."; // 默认内容
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}