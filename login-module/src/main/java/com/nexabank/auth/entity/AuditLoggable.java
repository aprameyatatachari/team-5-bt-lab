package com.nexabank.auth.entity;

import com.nexabank.auth.entity.enums.CrudValue;
import jakarta.persistence.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

// Use @MappedSuperclass to avoid creating a table for this abstract class
@MappedSuperclass
public abstract class AuditLoggable {

    @CreationTimestamp
    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;
    
    @Column(name = "CRUD_VALUE")
    @Enumerated(EnumType.STRING)
    private CrudValue crudValue;
    
    @Column(name = "AUDIT_USER_ID")
    private String auditUserId;
    
    @Column(name = "WS_ID")
    private String wsId;
    
    @Column(name = "PRGM_ID")
    private String prgmId;
    
    @Column(name = "HOST_TS")
    @UpdateTimestamp
    private Timestamp hostTs;
    
    @UpdateTimestamp
    @Column(name = "LOCAL_TS")
    private Timestamp localTs;
    
    @UpdateTimestamp
    @Column(name = "ACPT_TS")
    private Timestamp acptTs;
    
    @UpdateTimestamp
    @Column(name = "ACPT_TS_UTC_OFST")
    private Timestamp acptTsUtcOfst;
    
    @Column(name = "UUID_REFERENCE")
    private UUID uuidReference;

    // Constructors
    public AuditLoggable() {}

    // Getters and Setters
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public CrudValue getCrudValue() {
        return crudValue;
    }

    public void setCrudValue(CrudValue crudValue) {
        this.crudValue = crudValue;
    }

    public String getAuditUserId() {
        return auditUserId;
    }

    public void setAuditUserId(String auditUserId) {
        this.auditUserId = auditUserId;
    }

    public String getWsId() {
        return wsId;
    }

    public void setWsId(String wsId) {
        this.wsId = wsId;
    }

    public String getPrgmId() {
        return prgmId;
    }

    public void setPrgmId(String prgmId) {
        this.prgmId = prgmId;
    }

    public Timestamp getHostTs() {
        return hostTs;
    }

    public void setHostTs(Timestamp hostTs) {
        this.hostTs = hostTs;
    }

    public Timestamp getLocalTs() {
        return localTs;
    }

    public void setLocalTs(Timestamp localTs) {
        this.localTs = localTs;
    }

    public Timestamp getAcptTs() {
        return acptTs;
    }

    public void setAcptTs(Timestamp acptTs) {
        this.acptTs = acptTs;
    }

    public Timestamp getAcptTsUtcOfst() {
        return acptTsUtcOfst;
    }

    public void setAcptTsUtcOfst(Timestamp acptTsUtcOfst) {
        this.acptTsUtcOfst = acptTsUtcOfst;
    }

    public UUID getUuidReference() {
        return uuidReference;
    }

    public void setUuidReference(UUID uuidReference) {
        this.uuidReference = uuidReference;
    }
}