package com.redshirt.warpcore.data

import androidx.room.*

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY createdAt DESC")
    suspend fun getAll(): List<Profile>

    @Insert
    suspend fun insert(profile: Profile): Long

    @Update
    suspend fun update(profile: Profile)

    @Delete
    suspend fun delete(profile: Profile)

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getById(id: Int): Profile?
}