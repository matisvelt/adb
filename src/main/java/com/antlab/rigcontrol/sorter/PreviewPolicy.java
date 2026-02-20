package com.antlab.rigcontrol.sorter;

public class PreviewPolicy {
    private int maxLongEdgePx = 1536;
    private String format = "WEBP";
    private int quality = 85;
    private boolean keepPreviews = false;

    public PreviewPolicy() {
    }

    public PreviewPolicy(int maxLongEdgePx, String format, int quality, boolean keepPreviews) {
        this.maxLongEdgePx = maxLongEdgePx;
        this.format = format;
        this.quality = quality;
        this.keepPreviews = keepPreviews;
    }

    public int getMaxLongEdgePx() {
        return maxLongEdgePx;
    }

    public void setMaxLongEdgePx(int maxLongEdgePx) {
        this.maxLongEdgePx = Math.max(256, maxLongEdgePx);
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format == null ? "WEBP" : format.trim().toUpperCase();
    }

    public int getQuality() {
        return quality;
    }

    public void setQuality(int quality) {
        this.quality = Math.min(95, Math.max(50, quality));
    }

    public boolean isKeepPreviews() {
        return keepPreviews;
    }

    public void setKeepPreviews(boolean keepPreviews) {
        this.keepPreviews = keepPreviews;
    }
}
