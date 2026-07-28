package social.vyb.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import social.vyb.app.data.Listing

@Composable
fun MarketScreen(listings: List<Listing>) {
    LazyColumn {
        item {
            Row(
                Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Campus Market", style = MaterialTheme.typography.headlineMedium)
                    Text("Buy nearby. Sell safely.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = {}) {
                    Icon(Icons.Default.Add, null)
                    Text("Sell")
                }
            }
        }
        items(listings) { listing ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    Modifier.fillMaxWidth(.32f).aspectRatio(1f)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(18.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Inventory2, null) }
                Column(Modifier.weight(1f).align(Alignment.CenterVertically)) {
                    Text(listing.tag.uppercase(), color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge)
                    Text(listing.title, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium)
                    Text(listing.price, style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(vertical = 4.dp))
                    Text(listing.seller, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
