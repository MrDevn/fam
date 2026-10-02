package com.fam.aware.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.fam.aware.R
import com.fam.aware.ui.theme.Dimens

/**
 * Явный перечень того, чего приложение не делает.
 *
 * Экран намеренно сделан публичным: пользователю и родителю должно быть видно,
 * что приложение не использует привилегированные механизмы и не скрывается.
 */
@Composable
fun TransparencyCard(modifier: Modifier = Modifier) {
    SectionCard(
        modifier = modifier,
        title = stringResource(R.string.home_section_transparency),
        icon = Icons.Filled.CheckCircle,
        accent = MaterialTheme.colorScheme.secondary,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            listOf(
                R.string.transparency_item_1,
                R.string.transparency_item_2,
                R.string.transparency_item_3,
                R.string.transparency_item_4,
                R.string.transparency_item_5,
                R.string.transparency_item_6,
                R.string.transparency_item_7,
            ).forEach { itemRes ->
                BulletItem(
                    text = stringResource(itemRes),
                    marker = Icons.Filled.CheckCircle,
                    markerTint = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        Spacer(Modifier.height(Dimens.ItemGap))
    }
}
