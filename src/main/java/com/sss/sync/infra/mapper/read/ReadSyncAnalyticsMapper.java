package com.sss.sync.infra.mapper.read;

import com.sss.sync.web.dto.SyncStatsDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ReadSyncAnalyticsMapper {

  /**
   * Get daily sync statistics for the last N days
   */
  List<SyncStatsDTO> getDailySyncStats(
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate
  );

  /**
   * Get summary statistics for the date range
   */
  Long getTotalSyncedChanges(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

  Long getTotalConflictsCreated(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

  Long getTotalConflictsResolved(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}