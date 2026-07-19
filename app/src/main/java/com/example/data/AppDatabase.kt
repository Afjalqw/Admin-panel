package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

// 1. Entity for User Logins via Gmail/Google Auth
@Entity(tableName = "user_logins")
data class UserLogin(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val displayName: String,
    val email: String,
    val timestamp: Long = System.currentTimeMillis(),
    val devInfoMock: String = "Device: Android Simulator v35"
)

// 2. Entity for Dynamic Settings controlled by Admin
@Entity(tableName = "app_config")
data class AppConfig(
    @PrimaryKey val id: Int = 1,
    val customAppName: String = "AI PDF Studio",
    val appHeaderLogoIndex: Int = 0, // 0: PDF Document Icon, 1: Diamond Premium, 2: Sparkle Magic, 3: Shield Secure
    val admobEnabled: Boolean = false,
    val admobBannerUnitId: String = "ca-app-pub-3940256099942544/6300978111", // Standard AdMob Test Banner ID
    val admobInterstitialUnitId: String = "ca-app-pub-3940256099942544/1033173712", // Standard AdMob Test Interstitial ID
    val admobRewardedUnitId: String = "ca-app-pub-3940256099942544/5224354917", // Standard AdMob Test Rewarded ID
    val admobNativeUnitId: String = "ca-app-pub-3940256099942544/2247696110", // Standard AdMob Test Native ID
    val admobAppOpenUnitId: String = "ca-app-pub-3940256099942544/3419835294", // Standard AdMob Test App Open ID
    val testAdMode: Boolean = true,
    val adminPin: String = "8070"
)

// 3. DAOs
@Dao
interface UserLoginDao {
    @Query("SELECT * FROM user_logins ORDER BY timestamp DESC")
    fun getAllLogins(): Flow<List<UserLogin>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogin(login: UserLogin)

    @Query("DELETE FROM user_logins")
    suspend fun clearLogins()
}

@Dao
interface AppConfigDao {
    @Query("SELECT * FROM app_config WHERE id = 1 LIMIT 1")
    fun getConfigFlow(): Flow<AppConfig?>

    @Query("SELECT * FROM app_config WHERE id = 1 LIMIT 1")
    suspend fun getConfig(): AppConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: AppConfig)
}

// 4. Room Database Singleton
@Database(entities = [UserLogin::class, AppConfig::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userLoginDao(): UserLoginDao
    abstract fun appConfigDao(): AppConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pdf_studio_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
