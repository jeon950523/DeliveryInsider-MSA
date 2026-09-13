package com.deliveryinsider.platform.domain.admin;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminPlatformMapper {
    long countConnections();
    long countActiveConnections();
    long countBlockedIncidents();
    long countRetryableIncidents();
    List<AdminConnectionRow> findConnectionPage(@Param("limit") int limit, @Param("offset") int offset);
    List<AdminIncidentRow> findIncidentPage(@Param("limit") int limit, @Param("offset") int offset);
}
