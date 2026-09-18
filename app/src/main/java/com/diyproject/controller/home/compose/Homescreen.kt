package com.diyproject.controller.home.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.diyproject.controller.FeatureItem
import androidx.compose.ui.graphics.painter.Painter

/* ------------------------------------------------------------------ */
/*  UI state holders                                                    */
/*                                                                      */
/*  HomeScreen used to take ~20 loose primitives. Grouping the ones     */
/*  that always travel together does two things: the call site reads   */
/*  like a sentence instead of a parameter dump, and because each      */
/*  holder is @Immutable, Compose can trust it for skip checks and     */
/*  doesn't have to re-walk the whole tree just because, say, the      */
/*  connection status changed. That matters more now that the status   */
/*  updates live instead of only on app resume — see MainActivity.     */
/* ------------------------------------------------------------------ */

@Immutable
data class ConnectionState(
    val connected: Boolean,
    val label: String,
)

@Immutable
data class FeatureSection(
    val title: String,
    val items: List<FeatureItem>,
    val badgeFor: ((String) -> String?)? = null,
)

@Immutable
data class TemplateSection(
    val title: String,
    val items: List<FeatureItem>,
    val selectedId: String?,
)

@Immutable
data class HomeFooter(
    val shareLabel: String,
    val aboutLabel: String,
    val versionLabel: String,
    val madeBy: String,
)

/**
 * The home screen. Pure UI — every callback and list of [FeatureItem] is
 * supplied by the caller (MainActivity), so none of the existing
 * navigation, Bluetooth or share/about logic lives here.
 */
@Composable
fun HomeScreen(
    appName: String,
    tagline: String,
    brandIcon: Painter,
    connection: ConnectionState,
    controlSection: FeatureSection,
    developerSection: FeatureSection,
    templateSection: TemplateSection,
    footer: HomeFooter,
    onTemplateSelected: (String) -> Unit,
    onFeatureClick: (FeatureItem) -> Unit,
    onShareClick: () -> Unit,
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background),
    ) {
        HomeHeader(
            title = appName,
            tagline = tagline,
            brandIcon = brandIcon,
            connected = connection.connected,
            connectionLabel = connection.label,
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = AppDimens.Space16),
        ) {
            item {
                HomeSection(title = controlSection.title, count = controlSection.items.size) {
                    FeatureRow(
                        entries = controlSection.items,
                        onFeatureClick = onFeatureClick,
                        badgeFor = controlSection.badgeFor,
                    )
                }
            }
            item {
                HomeSection(title = developerSection.title, count = developerSection.items.size) {
                    FeatureRow(
                        entries = developerSection.items,
                        onFeatureClick = onFeatureClick,
                        badgeFor = developerSection.badgeFor,
                    )
                }
            }
            item {
                HomeSection(title = templateSection.title, count = templateSection.items.size) {
                    TemplateRow(
                        entries = templateSection.items,
                        selectedId = templateSection.selectedId,
                        onSelected = onTemplateSelected,
                    )
                }
            }
        }

        UtilityRow(
            shareLabel = footer.shareLabel,
            aboutLabel = footer.aboutLabel,
            onShareClick = onShareClick,
            onAboutClick = onAboutClick,
        )
        AppFooterText(
            versionLabel = footer.versionLabel,
            madeBy = footer.madeBy,
            modifier = Modifier.padding(bottom = AppDimens.Space16),
        )
    }
}

/** Developer-section items get a short, honest metadata badge derived from what they already do. */
fun developerBadgeFor(id: String): String? = when (id) {
    "code_control" -> "RAW"
    "terminal" -> "SERIAL"
    "macros" -> "MACRO"
    else -> null
}

@Composable
private fun HomeSection(
    title: String,
    count: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.padding(top = AppDimens.Space20)) {
        SectionHeader(title = title, count = count)
        Spacer(modifier = Modifier.height(AppDimens.Space12))
        content()
    }
}

@Composable
private fun FeatureRow(
    entries: List<FeatureItem>,
    onFeatureClick: (FeatureItem) -> Unit,
    badgeFor: ((String) -> String?)?,
) {
    if (entries.isEmpty()) {
        EmptyStateText(text = "No items configured")
        return
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = AppDimens.Space16),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Space12),
    ) {
        items(entries, key = { it.id }) { item ->
            FeatureCard(
                icon = painterResource(id = item.iconRes),
                title = item.title,
                description = item.subtitle,
                badge = badgeFor?.invoke(item.id),
                onClick = { onFeatureClick(item) },
            )
        }
    }
}

@Composable
private fun TemplateRow(
    entries: List<FeatureItem>,
    selectedId: String?,
    onSelected: (String) -> Unit,
) {
    if (entries.isEmpty()) {
        EmptyStateText(text = "No vehicle configured")
        return
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = AppDimens.Space16),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Space12),
    ) {
        items(entries, key = { it.id }) { item ->
            VehicleTemplateCard(
                icon = painterResource(id = item.iconRes),
                title = item.title,
                description = item.subtitle,
                selected = item.id == selectedId,
                onClick = { onSelected(item.id) },
            )
        }
    }
}

@Composable
private fun UtilityRow(
    shareLabel: String,
    aboutLabel: String,
    onShareClick: () -> Unit,
    onAboutClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.Space16, vertical = AppDimens.Space12),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Space12),
    ) {
        UtilityActionButton(
            icon = painterResource(id = android.R.drawable.ic_menu_share),
            label = shareLabel,
            onClick = onShareClick,
            modifier = Modifier.weight(1f),
        )
        UtilityActionButton(
            icon = painterResource(id = android.R.drawable.ic_menu_info_details),
            label = aboutLabel,
            onClick = onAboutClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun EmptyStateText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = AppColors.TextTertiary,
        modifier = modifier.padding(horizontal = AppDimens.Space16),
    )
}