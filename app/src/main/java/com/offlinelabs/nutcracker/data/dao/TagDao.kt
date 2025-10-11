package com.offlinelabs.nutcracker.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.offlinelabs.nutcracker.data.model.Tag
import com.offlinelabs.nutcracker.data.model.TagType
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: Tag): Long
    
    @Update
    suspend fun updateTag(tag: Tag)
    
    @Delete
    suspend fun deleteTag(tag: Tag)
    
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<Tag>>
    
    @Query("SELECT * FROM tags WHERE type = :type ORDER BY name ASC")
    fun getTagsByType(type: TagType): Flow<List<Tag>>
    
    @Query("SELECT * FROM tags WHERE id = :tagId")
    fun getTagById(tagId: Long): Flow<Tag?>
    
    @Query("SELECT * FROM tags WHERE name = :name AND type = :type")
    fun getTagByNameAndType(name: String, type: TagType): Flow<Tag?>
    
    @Query("DELETE FROM tags")
    suspend fun deleteAllTags()
}
