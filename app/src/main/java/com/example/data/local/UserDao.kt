package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:username) LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Query("SELECT * FROM users ORDER BY fullName ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("UPDATE users SET avatarId = :avatarId, avatarColorHex = :avatarColorHex WHERE id = :userId")
    suspend fun updateUserAvatar(userId: Long, avatarId: Int, avatarColorHex: String)

    @Query("UPDATE users SET password = :newPassword WHERE id = :userId")
    suspend fun updateUserPassword(userId: Long, newPassword: String)

    @Query("UPDATE users SET email = :newEmail WHERE id = :userId")
    suspend fun updateUserEmail(userId: Long, newEmail: String)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int
}
