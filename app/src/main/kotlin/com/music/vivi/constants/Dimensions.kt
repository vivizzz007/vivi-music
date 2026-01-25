package com.music.vivi.constants

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

public const val CONTENT_TYPE_HEADER: Int = 0
public const val CONTENT_TYPE_LIST: Int = 1
public const val CONTENT_TYPE_SONG: Int = 2
public const val CONTENT_TYPE_ARTIST: Int = 3
public const val CONTENT_TYPE_ALBUM: Int = 4
public const val CONTENT_TYPE_PLAYLIST: Int = 5

public val NavigationBarHeight: Dp = 80.dp
public val SlimNavBarHeight: Dp = 64.dp
public val MiniPlayerHeight: Dp = 64.dp
public val MiniPlayerBottomSpacing: Dp = 8.dp // 8Space between MiniPlayer and NavigationBar
public val QueuePeekHeight: Dp = 64.dp
public val AppBarHeight: Dp = 64.dp

public val ListItemHeight: Dp = 64.dp
public val SuggestionItemHeight: Dp = 56.dp
public val SearchFilterHeight: Dp = 48.dp
public val ListThumbnailSize: Dp = 48.dp
public val SmallGridThumbnailHeight: Dp = 104.dp
public val GridThumbnailHeight: Dp = 128.dp
public val AlbumThumbnailSize: Dp = 144.dp

public val ThumbnailCornerRadius: Dp = 6.dp

public val PlayerHorizontalPadding: Dp = 32.dp

public val NavigationBarAnimationSpec: androidx.compose.animation.core.SpringSpec<Dp> =
    spring<Dp>(stiffness = Spring.StiffnessMediumLow)
public val BottomSheetAnimationSpec: androidx.compose.animation.core.SpringSpec<Dp> =
    spring<Dp>(stiffness = Spring.StiffnessMediumLow)
public val BottomSheetSoftAnimationSpec: androidx.compose.animation.core.SpringSpec<Dp> =
    spring<Dp>(stiffness = Spring.StiffnessLow)

public val MoodAndGenresButtonHeight: Dp = 48.dp
