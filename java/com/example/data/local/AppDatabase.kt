package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.ReportEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.data.model.TaskCategory
import com.example.data.model.UserEntity
import com.example.data.model.UserRoles
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [TaskEntity::class, ReportEntity::class, UserEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun reportDao(): ReportDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gestor_database.db"
                )
                    .addCallback(AppDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database.taskDao(), database.reportDao(), database.userDao())
                }
            }
        }

        private suspend fun populateInitialData(
            taskDao: TaskDao,
            reportDao: ReportDao,
            userDao: UserDao
        ) {
            // Personal de Servicios Generales precargado en la base de datos
            userDao.insertUser(
                UserEntity(
                    username = "servicios",
                    fullName = "Mario Gómez",
                    password = "123",
                    role = UserRoles.SERVICIOS_GENERALES,
                    email = "servicios@colegio.edu.co",
                    avatarId = 9,
                    avatarColorHex = "#00FFFF"
                )
            )
            userDao.insertUser(
                UserEntity(
                    username = "mantenimiento",
                    fullName = "Roberto Silva",
                    password = "123",
                    role = UserRoles.SERVICIOS_GENERALES,
                    email = "mantenimiento@colegio.edu.co",
                    avatarId = 4,
                    avatarColorHex = "#FFCC00"
                )
            )

            // Cuentas de demostración adicionales para Profesor y Estudiante
            userDao.insertUser(
                UserEntity(
                    username = "profesor",
                    fullName = "Prof. Andrés Castro",
                    password = "123",
                    role = UserRoles.PROFESOR,
                    email = "andres.castro@colegio.edu.co",
                    avatarId = 1,
                    avatarColorHex = "#0056D2"
                )
            )
            userDao.insertUser(
                UserEntity(
                    username = "estudiante",
                    fullName = "Camila Torres",
                    password = "123",
                    role = UserRoles.ESTUDIANTE,
                    email = "camila.torres@colegio.edu.co",
                    avatarId = 10,
                    avatarColorHex = "#FF00FF"
                )
            )

            // La base de datos de reportes inicia limpia y lista para registrar incidencias
        }
    }
}
