package com.antlab.rigcontrol.sorter;

public class FileRecord {
    private String fileId;
    private String sourcePath;
    private long sizeBytes;
    private long modifiedTime;
    private String extension;
    private FileType fileType = FileType.UNKNOWN;
    private FileStatus status = FileStatus.NEW;
    private PreviewStatus previewStatus = PreviewStatus.NOT_READY;
    private String previewPath;
    private String label;
    private double confidence;
    private int facesCount;
    private double hasTextLikelihood;
    private double isDocumentLikelihood;
    private double screenshotLikelihood;
    private String exifDateTime;
    private String exifModel;
    private String notes;
    private String ruleId;
    private String ruleName;
    private String destinationPath;
    private String moveStatus;
    private String error;
    private long lastUpdated;

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public long getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(long modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public FileType getFileType() {
        return fileType == null ? FileType.UNKNOWN : fileType;
    }

    public void setFileType(FileType fileType) {
        this.fileType = fileType == null ? FileType.UNKNOWN : fileType;
    }

    public FileStatus getStatus() {
        return status == null ? FileStatus.NEW : status;
    }

    public void setStatus(FileStatus status) {
        this.status = status == null ? FileStatus.NEW : status;
    }

    public PreviewStatus getPreviewStatus() {
        return previewStatus == null ? PreviewStatus.NOT_READY : previewStatus;
    }

    public void setPreviewStatus(PreviewStatus previewStatus) {
        this.previewStatus = previewStatus == null ? PreviewStatus.NOT_READY : previewStatus;
    }

    public String getPreviewPath() {
        return previewPath;
    }

    public void setPreviewPath(String previewPath) {
        this.previewPath = previewPath;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public int getFacesCount() {
        return facesCount;
    }

    public void setFacesCount(int facesCount) {
        this.facesCount = facesCount;
    }

    public double getHasTextLikelihood() {
        return hasTextLikelihood;
    }

    public void setHasTextLikelihood(double hasTextLikelihood) {
        this.hasTextLikelihood = hasTextLikelihood;
    }

    public double getIsDocumentLikelihood() {
        return isDocumentLikelihood;
    }

    public void setIsDocumentLikelihood(double isDocumentLikelihood) {
        this.isDocumentLikelihood = isDocumentLikelihood;
    }

    public double getScreenshotLikelihood() {
        return screenshotLikelihood;
    }

    public void setScreenshotLikelihood(double screenshotLikelihood) {
        this.screenshotLikelihood = screenshotLikelihood;
    }

    public String getExifDateTime() {
        return exifDateTime;
    }

    public void setExifDateTime(String exifDateTime) {
        this.exifDateTime = exifDateTime;
    }

    public String getExifModel() {
        return exifModel;
    }

    public void setExifModel(String exifModel) {
        this.exifModel = exifModel;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public void setDestinationPath(String destinationPath) {
        this.destinationPath = destinationPath;
    }

    public String getMoveStatus() {
        return moveStatus;
    }

    public void setMoveStatus(String moveStatus) {
        this.moveStatus = moveStatus;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String shortId() {
        if (fileId == null) {
            return "";
        }
        return fileId.length() <= 8 ? fileId : fileId.substring(0, 8);
    }
}
