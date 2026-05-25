package com.zapbot.android.database

import androidx.room.TypeConverter
import com.zapbot.android.domain.DownloadStatus
import com.zapbot.android.domain.DownloadType
import com.zapbot.android.domain.LogLevel

class Converters {
    @TypeConverter fun toDownloadType(value: String) = DownloadType.valueOf(value)
    @TypeConverter fun fromDownloadType(value: DownloadType) = value.name
    @TypeConverter fun toDownloadStatus(value: String) = DownloadStatus.valueOf(value)
    @TypeConverter fun fromDownloadStatus(value: DownloadStatus) = value.name
    @TypeConverter fun toLogLevel(value: String) = LogLevel.valueOf(value)
    @TypeConverter fun fromLogLevel(value: LogLevel) = value.name
}
