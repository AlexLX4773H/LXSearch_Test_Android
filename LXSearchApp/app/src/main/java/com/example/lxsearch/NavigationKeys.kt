package com.example.lxsearch

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey
@Serializable data class CreateFileListRoute(val initialIsV2: Boolean? = null) : NavKey
@Serializable data object SearchRoute : NavKey
@Serializable data object NameCircleRoute : NavKey
@Serializable data object MoveToTempRoute : NavKey
@Serializable data object MoveFromSourceRoute : NavKey
@Serializable data class InputFilesRoute(val initialFileName: String? = null) : NavKey
