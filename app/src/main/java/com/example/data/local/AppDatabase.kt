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
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun reportDao(): ReportDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reports ADD COLUMN imagePath TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gestor_database.db"
                )
                    .addCallback(AppDatabaseCallback(scope))
                    .addMigrations(MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val PRECONFIGURED_SERVICIOS_ACCOUNTS = listOf(
            UserEntity(
                username = "servicios1",
                fullName = "Servicios Generales 01",
                password = "Sg#7492k",
                role = UserRoles.SERVICIOS_GENERALES,
                email = "servicios1@colredentorista.edu.co",
                avatarId = 2,
                avatarColorHex = "#00FFFF"
            ),
            UserEntity(
                username = "servicios2",
                fullName = "Servicios Generales 02",
                password = "Sg#3851m",
                role = UserRoles.SERVICIOS_GENERALES,
                email = "servicios2@colredentorista.edu.co",
                avatarId = 3,
                avatarColorHex = "#FFCC00"
            ),
            UserEntity(
                username = "servicios3",
                fullName = "Servicios Generales 03",
                password = "Sg#9164p",
                role = UserRoles.SERVICIOS_GENERALES,
                email = "servicios3@colredentorista.edu.co",
                avatarId = 5,
                avatarColorHex = "#39FF14"
            ),
            UserEntity(
                username = "servicios4",
                fullName = "Servicios Generales 04",
                password = "Sg#5283r",
                role = UserRoles.SERVICIOS_GENERALES,
                email = "servicios4@colredentorista.edu.co",
                avatarId = 6,
                avatarColorHex = "#FF00FF"
            ),
            UserEntity(
                username = "servicios5",
                fullName = "Servicios Generales 05",
                password = "Sg#6419t",
                role = UserRoles.SERVICIOS_GENERALES,
                email = "servicios5@colredentorista.edu.co",
                avatarId = 7,
                avatarColorHex = "#0056D2"
            ),
            UserEntity(
                username = "servicios6",
                fullName = "Servicios Generales 06",
                password = "Sg#2738v",
                role = UserRoles.SERVICIOS_GENERALES,
                email = "servicios6@colredentorista.edu.co",
                avatarId = 8,
                avatarColorHex = "#FF6B00"
            ),
            UserEntity(
                username = "servicios7",
                fullName = "Servicios Generales 07",
                password = "Sg#8546w",
                role = UserRoles.SERVICIOS_GENERALES,
                email = "servicios7@colredentorista.edu.co",
                avatarId = 11,
                avatarColorHex = "#9D4EDD"
            ),
            UserEntity(
                username = "servicios8",
                fullName = "Servicios Generales 08",
                password = "Sg#1927x",
                role = UserRoles.SERVICIOS_GENERALES,
                email = "servicios8@colredentorista.edu.co",
                avatarId = 12,
                avatarColorHex = "#00E5FF"
            )
        )

        suspend fun ensureServiciosAccounts(userDao: UserDao) {
            PRECONFIGURED_SERVICIOS_ACCOUNTS.forEach { account ->
                if (userDao.getUserByUsername(account.username) == null) {
                    userDao.insertUser(account)
                }
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

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    ensureServiciosAccounts(database.userDao())
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

            ensureServiciosAccounts(userDao)

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
