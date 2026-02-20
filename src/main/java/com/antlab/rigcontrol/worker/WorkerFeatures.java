package com.antlab.rigcontrol.worker;

public class WorkerFeatures {
    private double isDocumentLikelihood;
    private double hasTextLikelihood;
    private int facesCount;
    private double screenshotLikelihood;
    private String notes;

    public double getIsDocumentLikelihood() {
        return isDocumentLikelihood;
    }

    public void setIsDocumentLikelihood(double isDocumentLikelihood) {
        this.isDocumentLikelihood = isDocumentLikelihood;
    }

    public double getHasTextLikelihood() {
        return hasTextLikelihood;
    }

    public void setHasTextLikelihood(double hasTextLikelihood) {
        this.hasTextLikelihood = hasTextLikelihood;
    }

    public int getFacesCount() {
        return facesCount;
    }

    public void setFacesCount(int facesCount) {
        this.facesCount = facesCount;
    }

    public double getScreenshotLikelihood() {
        return screenshotLikelihood;
    }

    public void setScreenshotLikelihood(double screenshotLikelihood) {
        this.screenshotLikelihood = screenshotLikelihood;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
