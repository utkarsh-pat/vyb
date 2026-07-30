package social.vyb.app.features.market

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import social.vyb.app.ui.VybBorder
import social.vyb.app.ui.VybIndigo
import social.vyb.app.ui.VybMuted
import social.vyb.app.ui.VybPanel
import social.vyb.app.ui.VybPanelLifted
import social.vyb.app.ui.VybText

@Composable
internal fun PwaMarketControls(
    state: MarketUiState,
    dashboard: MarketDashboard?,
    onTab: (String) -> Unit,
    onQuery: (String) -> Unit,
    onCategory: (String?) -> Unit,
    onSort: (MarketSort) -> Unit
) {
    var filtersOpen by remember { mutableStateOf(false) }
    var sortOpen by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        color = Color(0xEE0A0B11),
        border = BorderStroke(1.dp, VybBorder),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(
                Modifier.fillMaxWidth().height(40.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                val tabs = listOf(
                    Triple("sale", "Items", dashboard?.listings?.size ?: 0),
                    Triple(
                        "buying",
                        "Requests",
                        dashboard?.requests?.count { it.tab == "buying" } ?: 0
                    ),
                    Triple(
                        "lend",
                        "Lend",
                        dashboard?.requests?.count { it.tab == "lend" } ?: 0
                    )
                )
                tabs.forEach { (id, label, count) ->
                    val selected = state.tab == id
                    Surface(
                        onClick = { onTab(id) },
                        modifier = Modifier.weight(1f),
                        color = Color.Transparent,
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .background(
                                    if (selected) {
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0xFF0B706F),
                                                Color(0xFF293D82)
                                            )
                                        )
                                    } else {
                                        Brush.horizontalGradient(
                                            listOf(Color.Transparent, Color.Transparent)
                                        )
                                    }
                                ),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                label,
                                color = if (selected) VybText else VybMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Surface(
                                modifier = Modifier.padding(start = 7.dp),
                                color = VybPanelLifted,
                                shape = CircleShape
                            ) {
                                Text(
                                    count.toString(),
                                    color = VybText,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQuery,
                    modifier = Modifier.weight(1f).height(54.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { onQuery("") }) {
                                Icon(Icons.Default.Clear, "Clear search")
                            }
                        }
                    },
                    placeholder = {
                        Text(
                            if (state.tab == "sale") "Search items, sellers"
                            else "Search requests, people",
                            maxLines = 1,
                            fontSize = 13.sp
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = VybPanel,
                        unfocusedContainerColor = VybPanel,
                        focusedBorderColor = VybIndigo,
                        unfocusedBorderColor = VybBorder,
                        focusedTextColor = VybText,
                        unfocusedTextColor = VybText,
                        focusedPlaceholderColor = VybMuted,
                        unfocusedPlaceholderColor = VybMuted
                    )
                )
                MarketSquareAction(
                    selected = filtersOpen || state.category != null,
                    icon = Icons.Default.FilterList,
                    label = "Filter",
                    onClick = { filtersOpen = !filtersOpen }
                )
                MarketSquareAction(
                    selected = state.sort != MarketSort.Recent,
                    icon = Icons.Default.SwapVert,
                    label = "Sort",
                    onClick = { sortOpen = true }
                )
            }
            if (filtersOpen) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    val categories = dashboard?.categoriesFor(state.tab).orEmpty()
                    MarketFilterPill("All", state.category == null) { onCategory(null) }
                    categories.forEach { category ->
                        MarketFilterPill(
                            category,
                            state.category.equals(category, ignoreCase = true)
                        ) { onCategory(category) }
                    }
                }
            }
        }
    }
    if (sortOpen) {
        AlertDialog(
            onDismissRequest = { sortOpen = false },
            title = { Text("Sort marketplace") },
            text = {
                Column {
                    MarketSort.entries.forEach { option ->
                        TextButton(
                            onClick = {
                                onSort(option)
                                sortOpen = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                option.label,
                                color = if (state.sort == option) VybIndigo else VybText
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { sortOpen = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun MarketSquareAction(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(44.dp),
        color = if (selected) VybIndigo.copy(alpha = .22f) else VybPanel,
        border = BorderStroke(1.dp, if (selected) VybIndigo else VybBorder),
        shape = RoundedCornerShape(18.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, label, tint = if (selected) VybText else VybMuted)
        }
    }
}

@Composable
private fun MarketFilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (selected) VybIndigo.copy(alpha = .26f) else VybPanel,
        border = BorderStroke(1.dp, if (selected) VybIndigo else VybBorder),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(
            label,
            color = if (selected) VybText else VybMuted,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
        )
    }
}
