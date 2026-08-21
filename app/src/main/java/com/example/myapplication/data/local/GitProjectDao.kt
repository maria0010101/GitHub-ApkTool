package com.example.myapplication.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GitProjectDao {
    @Query("SELECT * FROM git_projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<GitProject>>

    @Query("SELECT * FROM git_projects WHERE id = :id LIMIT 1")
    fun getProjectById(id: Long): GitProject?

    @Query("SELECT * FROM git_projects WHERE LOWER(owner) = LOWER(:owner) AND LOWER(repo) = LOWER(:repo) LIMIT 1")
    fun findProject(owner: String, repo: String): GitProject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertProject(project: GitProject): Long

    @Update
    fun updateProject(project: GitProject): Int

    @Delete
    fun deleteProject(project: GitProject): Int

    @Query("DELETE FROM git_projects WHERE id = :id")
    fun deleteProjectById(id: Long): Int
}
