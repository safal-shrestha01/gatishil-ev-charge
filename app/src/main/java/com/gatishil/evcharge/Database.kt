package com.gatishil.evcharge

import android.content.Context
import androidx.room.*

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val txNumber: String,
    val timestamp: Long,
    val vehicleModel: String,
    val vehicleNo: String,
    val startBattery: Int,
    val endBattery: Int,
    val chargedBattery: Int,
    val rateUsed: Int,
    val billAmount: Int,
    val paymentMode: String
)

@Entity(tableName = "settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val ratePerPercent: Int = 25 // Default NPR 25
)

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAll(): List<Transaction>

    @Query("SELECT * FROM transactions WHERE timestamp >= :start AND timestamp <= :end ORDER BY timestamp ASC")
    suspend fun getByDateRange(start: Long, end: Long): List<Transaction>
    
    @Query("SELECT * FROM transactions WHERE vehicleNo LIKE '%' || :query || '%' OR txNumber LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<Transaction>
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun getSettings(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: AppSettings)
}

@Database(entities = [Transaction::class, AppSettings::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gatishil_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
