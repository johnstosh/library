/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

/**
 * Describes one alphabetically-bounded partition of the photo ZIP export.
 * The frontend uses partNumber/totalParts/rangeLabel to render download buttons;
 * startKey/endKey are included for transparency but are not needed by the UI.
 */
public class PhotoZipPartDto {

    private int partNumber;
    private int totalParts;
    /** Human-readable range, e.g. "0-9, A-H", "I-R", "S-Z" */
    private String rangeLabel;
    private int photoCount;
    private long estimatedMb;
    /** The lowest sort key in this part: "0" for digits, "#" for misc, or a single letter */
    private String startKey;
    /** The highest sort key in this part */
    private String endKey;

    public int getPartNumber() { return partNumber; }
    public void setPartNumber(int partNumber) { this.partNumber = partNumber; }

    public int getTotalParts() { return totalParts; }
    public void setTotalParts(int totalParts) { this.totalParts = totalParts; }

    public String getRangeLabel() { return rangeLabel; }
    public void setRangeLabel(String rangeLabel) { this.rangeLabel = rangeLabel; }

    public int getPhotoCount() { return photoCount; }
    public void setPhotoCount(int photoCount) { this.photoCount = photoCount; }

    public long getEstimatedMb() { return estimatedMb; }
    public void setEstimatedMb(long estimatedMb) { this.estimatedMb = estimatedMb; }

    public String getStartKey() { return startKey; }
    public void setStartKey(String startKey) { this.startKey = startKey; }

    public String getEndKey() { return endKey; }
    public void setEndKey(String endKey) { this.endKey = endKey; }
}
