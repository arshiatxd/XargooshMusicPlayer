package com.example.xargoosh.data.local.db

import androidx.room.*
import com.example.xargoosh.data.local.entities.FolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Query("SELECT * FROM folders WHERE uriString = :uri LIMIT 1")
    suspend fun getFolderByUri(uri: String): FolderEntity?

    @Query("SELECT * FROM folders WHERE id = :folderId LIMIT 1")
    suspend fun getFolder(folderId: Int): FolderEntity?

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :folderId")
    suspend fun deleteFolder(folderId: Int)
}
