package br.com.ticket.companion.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.ticket.companion.data.local.entities.MonitoredAppEntity

@Dao
interface MonitoredAppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(app: MonitoredAppEntity)

    @Query("SELECT * FROM monitored_apps ORDER BY label COLLATE NOCASE")
    suspend fun all(): List<MonitoredAppEntity>

    @Query("SELECT * FROM monitored_apps WHERE active = 1 ORDER BY label COLLATE NOCASE")
    suspend fun activeOnes(): List<MonitoredAppEntity>

    @Query("SELECT * FROM monitored_apps WHERE package_id = :packageId LIMIT 1")
    suspend fun find(packageId: String): MonitoredAppEntity?

    @Query("UPDATE monitored_apps SET active = :active WHERE package_id = :packageId")
    suspend fun setActive(packageId: String, active: Boolean)

    @Query("DELETE FROM monitored_apps WHERE package_id = :packageId")
    suspend fun forget(packageId: String)
}
