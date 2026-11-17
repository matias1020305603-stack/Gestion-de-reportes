package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    @Query("SELECT * FROM reports ORDER BY updatedAt DESC")
    fun getAllReports(): Flow<List<ReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReports(reports: List<ReportEntity>)

    @Update
    suspend fun updateReport(report: ReportEntity)

    @Delete
    suspend fun deleteReport(report: ReportEntity)

    @Query("UPDATE reports SET status = :status, resolutionNotes = :notes, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateReportStatus(id: Long, status: String, notes: String, updatedAt: Long)

    @Query("DELETE FROM reports")
    suspend fun clearAllReports()

    @Query("SELECT COUNT(*) FROM reports")
    suspend fun getReportCount(): Int
}
