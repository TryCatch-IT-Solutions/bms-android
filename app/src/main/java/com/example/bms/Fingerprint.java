package com.example.bms;

public class Fingerprint {

    private long id;
    private long biometricId;
    private String key;
    private String createdAt;
    private String updatedAt;
    private String deletedAt;
    private long deletedBy;

    public Fingerprint(long id, long biometricId, String key, String createdAt, String updatedAt, String deletedAt, long deletedBy) {
        this.id = id;
        this.biometricId = biometricId;
        this.key = key;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
        this.deletedBy = deletedBy;
    }


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getBiometricId() {
        return biometricId;
    }

    public void setBiometricId(long biometricId) {
        this.biometricId = biometricId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(String deletedAt) {
        this.deletedAt = deletedAt;
    }

    public long getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(long deletedBy) {
        this.deletedBy = deletedBy;
    }
}
