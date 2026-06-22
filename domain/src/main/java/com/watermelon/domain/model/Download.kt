package com.watermelon.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class Download(
    val songId: String,
    val filePath: String,
    val fileSize: Long,
    val downloadStatus: DownloadStatus,
    val progress: Int = 0,
    val downloadedAt: Long = System.currentTimeMillis()
) : Parcelable

enum class DownloadStatus {
    PENDING, IN_PROGRESS, COMPLETED, FAILED, CANCELLED
}
