package social.vyb.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import social.vyb.app.data.UpsertProfileRequest
import social.vyb.app.data.CourseCatalogItem

@Composable
fun OnboardingScreen(
    displayName: String,
    email: String,
    collegeName: String,
    saving: Boolean,
    error: String?,
    catalog: List<CourseCatalogItem>,
    usernameAvailable: Boolean?,
    usernameChecking: Boolean,
    onLoadCatalog: () -> Unit,
    onUsernameChanged: (String) -> Unit,
    onSubmit: (UpsertProfileRequest) -> Unit
) {
    val nameParts = remember(displayName) { displayName.trim().split(" ", limit = 2) }
    var username by remember(email) {
        mutableStateOf(email.substringBefore("@").lowercase().replace(Regex("[^a-z0-9._]"), "_").take(24))
    }
    var firstName by remember(displayName) { mutableStateOf(nameParts.firstOrNull().orEmpty()) }
    var lastName by remember(displayName) { mutableStateOf(nameParts.getOrNull(1).orEmpty()) }
    var course by remember { mutableStateOf("B.Tech") }
    var stream by remember { mutableStateOf("Computer Science and Engineering") }
    var year by remember { mutableStateOf("1") }
    var section by remember { mutableStateOf("") }
    var hosteller by remember { mutableStateOf(false) }
    var hostelName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        onLoadCatalog()
        onUsernameChanged(username)
    }
    LaunchedEffect(catalog) {
        if (catalog.isNotEmpty() && catalog.none { it.title == course }) {
            course = catalog.first().title
            stream = catalog.first().branch.orEmpty()
        }
    }

    VybPageBackground(Modifier.fillMaxSize()) {
        VybResponsiveFrame(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                VybBrandLockup(compact = false)
                Spacer(Modifier.height(28.dp))
                Text(
                    "Complete your campus profile",
                    color = VybText,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "$collegeName · $email",
                    color = VybMuted,
                    modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                )
                ProfileField(
                    username,
                    {
                        username = it.lowercase().filter { char ->
                            char.isLetterOrDigit() || char == '.' || char == '_'
                        }.take(24)
                        onUsernameChanged(username)
                    },
                    "User ID"
                )
                Text(
                    when {
                        usernameChecking -> "Checking availability..."
                        usernameAvailable == true -> "User ID is available"
                        usernameAvailable == false -> "That user ID is already taken"
                        else -> "3–24 lowercase letters, numbers, dots, or underscores"
                    },
                    color = if (usernameAvailable == false) {
                        androidx.compose.material3.MaterialTheme.colorScheme.error
                    } else VybMuted,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProfileField(firstName, { firstName = it }, "First name", Modifier.weight(1f))
                    ProfileField(lastName, { lastName = it }, "Last name", Modifier.weight(1f))
                }
                if (catalog.isEmpty()) {
                    ProfileField(course, { course = it }, "Course")
                    ProfileField(stream, { stream = it }, "Stream")
                } else {
                    CatalogPicker(
                        label = "Course",
                        value = course,
                        items = catalog.map { it.title }.distinct(),
                        onSelect = { selected ->
                            course = selected
                            catalog.firstOrNull { it.title == selected }?.branch?.let { stream = it }
                        }
                    )
                    val streams = catalog
                        .filter { it.title == course }
                        .mapNotNull { it.branch?.takeIf(String::isNotBlank) }
                        .distinct()
                    if (streams.isEmpty()) {
                        ProfileField(stream, { stream = it }, "Stream")
                    } else {
                        CatalogPicker("Stream", stream, streams) { stream = it }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProfileField(
                        year,
                        { year = it.filter(Char::isDigit).take(1) },
                        "Year",
                        Modifier.weight(1f),
                        KeyboardType.Number
                    )
                    ProfileField(section, { section = it.uppercase().take(12) }, "Section", Modifier.weight(1f))
                }
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Hosteller", color = VybText, fontWeight = FontWeight.Bold)
                        Text("Enable if you live in campus housing", color = VybMuted)
                    }
                    Switch(checked = hosteller, onCheckedChange = { hosteller = it })
                }
                if (hosteller) {
                    ProfileField(hostelName, { hostelName = it }, "Hostel name")
                }
                ProfileField(phone, { phone = it.take(19) }, "Phone number (optional)", keyboardType = KeyboardType.Phone)
                (localError ?: error)?.let {
                    Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp))
                }
                Button(
                    onClick = {
                        val parsedYear = year.toIntOrNull()
                        localError = validateProfileDraft(
                            ProfileDraft(
                                username = username,
                                firstName = firstName,
                                course = course,
                                stream = stream,
                                year = parsedYear,
                                section = section,
                                isHosteller = hosteller,
                                hostelName = hostelName
                            )
                        )
                        if (localError == null) {
                            onSubmit(
                                UpsertProfileRequest(
                                    username = username.trim(),
                                    firstName = firstName.trim(),
                                    lastName = lastName.trim().ifBlank { null },
                                    course = course.trim(),
                                    stream = stream.trim(),
                                    year = requireNotNull(parsedYear),
                                    section = section.trim(),
                                    isHosteller = hosteller,
                                    hostelName = hostelName.trim().takeIf { hosteller && it.isNotBlank() },
                                    phoneNumber = phone.trim().ifBlank { null }
                                )
                            )
                        }
                    },
                    enabled = !saving && usernameAvailable != false && !usernameChecking,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VybIndigo),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp),
                            strokeWidth = 2.dp,
                            color = VybText
                        )
                    } else {
                        Text("Enter Vyb", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun CatalogPicker(
    label: String,
    value: String,
    items: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = VybPanelLifted,
                unfocusedContainerColor = VybPanelLifted,
                focusedTextColor = VybText,
                unfocusedTextColor = VybText,
                focusedBorderColor = VybIndigo,
                unfocusedBorderColor = VybBorder
            )
        )
        Box(
            Modifier.matchParentSize().clickable { expanded = true }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        expanded = false
                        onSelect(item)
                    }
                )
            }
        }
    }
}

@Composable
private fun ProfileField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth().padding(bottom = 10.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = VybPanelLifted,
            unfocusedContainerColor = VybPanelLifted,
            focusedTextColor = VybText,
            unfocusedTextColor = VybText,
            focusedBorderColor = VybIndigo,
            unfocusedBorderColor = VybBorder,
            focusedLabelColor = VybMuted,
            unfocusedLabelColor = VybMuted
        )
    )
}
