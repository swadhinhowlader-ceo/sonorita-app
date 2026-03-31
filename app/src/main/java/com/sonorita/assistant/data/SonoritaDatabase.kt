package com.sonorita.assistant.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ═══════════════════════════════════════════════════════
// ENTITIES
// ═══════════════════════════════════════════════════════

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String, // "user" or "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val provider: String? = null
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val triggerTime: Long,
    val isRecurring: Boolean = false,
    val recurringInterval: Long = 0,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val category: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val streak: Int = 0,
    val longestStreak: Int = 0,
    val lastCompletedDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

@Entity(tableName = "habit_completions")
data class HabitCompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val completedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val dueTime: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val topic: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "preferences")
data class PreferenceEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "app_usage")
data class AppUsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val usageDurationMs: Long,
    val date: String, // YYYY-MM-DD
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "speed_tests")
data class SpeedTestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val downloadMbps: Double,
    val uploadMbps: Double,
    val ping: Long,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "contacts_memory")
data class ContactMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactName: String,
    val phoneNumber: String,
    val lastCallTime: Long? = null,
    val callCount: Int = 0,
    val notes: String? = null
)

@Entity(tableName = "hotspot_devices")
data class HotspotDeviceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceName: String,
    val ipAddress: String,
    val macAddress: String,
    val firstSeen: Long = System.currentTimeMillis(),
    val lastSeen: Long = System.currentTimeMillis()
)

// ═══════════════════════════════════════════════════════
// DAOs
// ═══════════════════════════════════════════════════════

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int = 50): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations ORDER BY timestamp ASC")
    suspend fun getAll(): List<ConversationEntity>

    @Insert
    suspend fun insert(conversation: ConversationEntity)

    @Query("DELETE FROM conversations WHERE id NOT IN (SELECT id FROM conversations ORDER BY timestamp DESC LIMIT :keep)")
    suspend fun trim(keep: Int = 1000)

    @Query("DELETE FROM conversations")
    suspend fun clearAll()
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE isActive = 1 ORDER BY triggerTime ASC")
    fun getActive(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isActive = 1 AND triggerTime <= :time")
    suspend fun getDue(time: Long = System.currentTimeMillis()): List<ReminderEntity>

    @Insert
    suspend fun insert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Query("UPDATE reminders SET isActive = 0 WHERE id = :id")
    suspend fun deactivate(id: Long)

    @Delete
    suspend fun delete(reminder: ReminderEntity)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE timestamp >= :from ORDER BY timestamp DESC")
    fun getSince(from: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT SUM(amount) FROM expenses WHERE timestamp >= :from")
    suspend fun getTotalSince(from: Long): Double?

    @Query("SELECT category, SUM(amount) as total FROM expenses WHERE timestamp >= :from GROUP BY category")
    suspend fun getCategoryTotals(from: Long): List<CategoryTotal>

    @Insert
    suspend fun insert(expense: ExpenseEntity)
}

data class CategoryTotal(
    val category: String,
    val total: Double
)

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE isActive = 1")
    fun getActive(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getById(id: Long): HabitEntity?

    @Insert
    suspend fun insert(habit: HabitEntity): Long

    @Update
    suspend fun update(habit: HabitEntity)

    @Insert
    suspend fun insertCompletion(completion: HabitCompletionEntity)

    @Query("SELECT COUNT(*) FROM habit_completions WHERE habitId = :habitId AND completedAt >= :since")
    suspend fun getCompletionCount(habitId: Long, since: Long): Int
}

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos WHERE isCompleted = 0 ORDER BY dueTime ASC, createdAt ASC")
    fun getPending(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos ORDER BY createdAt DESC")
    fun getAll(): Flow<List<TodoEntity>>

    @Insert
    suspend fun insert(todo: TodoEntity): Long

    @Update
    suspend fun update(todo: TodoEntity)

    @Query("UPDATE todos SET isCompleted = 1, completedAt = :time WHERE id = :id")
    suspend fun complete(id: Long, time: Long = System.currentTimeMillis())

    @Delete
    suspend fun delete(todo: TodoEntity)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<NoteEntity>

    @Insert
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)
}

@Dao
interface PreferenceDao {
    @Query("SELECT value FROM preferences WHERE `key` = :key")
    suspend fun get(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(preference: PreferenceEntity)

    @Query("DELETE FROM preferences WHERE `key` = :key")
    suspend fun delete(key: String)
}

@Dao
interface AppUsageDao {
    @Query("SELECT * FROM app_usage WHERE date = :date ORDER BY usageDurationMs DESC")
    suspend fun getForDate(date: String): List<AppUsageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(usage: AppUsageEntity)
}

@Dao
interface SpeedTestDao {
    @Query("SELECT * FROM speed_tests ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 10): List<SpeedTestEntity>

    @Insert
    suspend fun insert(test: SpeedTestEntity)
}

@Dao
interface ContactMemoryDao {
    @Query("SELECT * FROM contacts_memory WHERE phoneNumber = :number LIMIT 1")
    suspend fun getByPhone(number: String): ContactMemoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: ContactMemoryEntity)

    @Query("UPDATE contacts_memory SET callCount = callCount + 1, lastCallTime = :time WHERE phoneNumber = :number")
    suspend fun incrementCallCount(number: String, time: Long = System.currentTimeMillis())
}

@Dao
interface HotspotDeviceDao {
    @Query("SELECT * FROM hotspot_devices WHERE macAddress = :mac LIMIT 1")
    suspend fun getByMac(mac: String): HotspotDeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: HotspotDeviceEntity)

    @Query("SELECT * FROM hotspot_devices ORDER BY lastSeen DESC")
    suspend fun getAll(): List<HotspotDeviceEntity>
}

// ═══════════════════════════════════════════════════════
// DATABASE
// ═══════════════════════════════════════════════════════

@Database(
    entities = [
        ConversationEntity::class,
        ReminderEntity::class,
        ExpenseEntity::class,
        HabitEntity::class,
        HabitCompletionEntity::class,
        TodoEntity::class,
        NoteEntity::class,
        PreferenceEntity::class,
        AppUsageEntity::class,
        SpeedTestEntity::class,
        ContactMemoryEntity::class,
        HotspotDeviceEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SonoritaDatabase : RoomDatabase() {

    abstract fun conversationDao(): ConversationDao
    abstract fun reminderDao(): ReminderDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun habitDao(): HabitDao
    abstract fun todoDao(): TodoDao
    abstract fun noteDao(): NoteDao
    abstract fun preferenceDao(): PreferenceDao
    abstract fun appUsageDao(): AppUsageDao
    abstract fun speedTestDao(): SpeedTestDao
    abstract fun contactMemoryDao(): ContactMemoryDao
    abstract fun hotspotDeviceDao(): HotspotDeviceDao

    companion object {
        @Volatile
        private var INSTANCE: SonoritaDatabase? = null

        fun getInstance(context: android.content.Context): SonoritaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SonoritaDatabase::class.java,
                    "sonorita.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
