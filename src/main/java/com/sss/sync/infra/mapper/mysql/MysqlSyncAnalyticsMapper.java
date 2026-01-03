package com.sss.sync.infra.mapper.mysql;

import com.sss.sync.web.dto.SyncStatsDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface MysqlSyncAnalyticsMapper {

  /**
   * Get daily sync statistics for the last N days from MySQL tables
   */
  List<SyncStatsDTO> getDailySyncStats(
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate
  );

  /**
   * Get summary statistics for the date range from MySQL tables
   */
  Long getTotalSyncedChanges(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

  Long getTotalConflictsCreated(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

  Long getTotalConflictsResolved(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}