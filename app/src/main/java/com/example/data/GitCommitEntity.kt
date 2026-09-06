package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.GitCommitItem

@Entity(tableName = "git_commits")
data class GitCommitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sha: String,
    val message: String,
    val author: String,
    val timestamp: Long,
    val branch: String,
    val filesCount: Int
) {
    fun toCommitItem(): GitCommitItem {
        return GitCommitItem(
            sha = sha,
            message = message,
            author = author,
            timestamp = timestamp,
            branch = branch,
            filesCount = filesCount
        )
    }

    companion object {
        fun fromCommitItem(item: GitCommitItem): GitCommitEntity {
            return GitCommitEntity(
                sha = item.sha,
                message = item.message,
                author = item.author,
                timestamp = item.timestamp,
                branch = item.branch,
                filesCount = item.filesCount
            )
        }
    }
}
