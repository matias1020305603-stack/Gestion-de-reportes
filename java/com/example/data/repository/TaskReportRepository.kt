package com.example.data.repository

import com.example.data.local.ReportDao
import com.example.data.local.TaskDao
import com.example.data.local.UserDao
import com.example.data.model.ReportEntity
import com.example.data.model.TaskEntity
import com.example.data.model.UserEntity
import kotlinx.coroutines.flow.Flow

class TaskReportRepository(
    private val taskDao: TaskDao,
    private val reportDao: ReportDao,
    private val userDao: UserDao
) {
    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()
    val allReports: Flow<List<ReportEntity>> = reportDao.getAllReports()
    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()

    suspend fun addTask(task: TaskEntity): Long = taskDao.insertTask(task)
    suspend fun updateTask(task: TaskEntity) = taskDao.updateTask(task)
    suspend fun deleteTask(task: TaskEntity) = taskDao.deleteTask(task)
    suspend fun toggleTask(id: Long, completed: Boolean) = taskDao.setTaskCompleted(id, completed)

    suspend fun addReport(report: ReportEntity): Long = reportDao.insertReport(report)
    suspend fun updateReport(report: ReportEntity) = reportDao.updateReport(report)
    suspend fun deleteReport(report: ReportEntity) = reportDao.deleteReport(report)
    suspend fun updateReportStatus(id: Long, status: String, notes: String) {
        reportDao.updateReportStatus(id, status, notes, System.currentTimeMillis())
    }
    suspend fun clearAllReports() = reportDao.clearAllReports()

    // Auth methods
    suspend fun findUserByUsername(username: String): UserEntity? = userDao.getUserByUsername(username)
    suspend fun registerUser(user: UserEntity): Long = userDao.insertUser(user)
    suspend fun getUserCount(): Int = userDao.getUserCount()
    suspend fun updateUserAvatar(userId: Long, avatarId: Int, avatarColorHex: String) =
        userDao.updateUserAvatar(userId, avatarId, avatarColorHex)
    suspend fun updateUserPassword(userId: Long, newPassword: String) =
        userDao.updateUserPassword(userId, newPassword)
    suspend fun updateUserEmail(userId: Long, newEmail: String) =
        userDao.updateUserEmail(userId, newEmail)
}
