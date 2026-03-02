package com.example.acatalepticmeditations.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.acatalepticmeditations.data.JournalEntry
import com.example.acatalepticmeditations.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CalendarScreen(modifier: Modifier = Modifier, viewModel: JournalViewModel) {
    val calendarPermissionState = rememberPermissionState(Manifest.permission.READ_CALENDAR)

    if (calendarPermissionState.status.isGranted) {
        CalendarView(modifier, viewModel)
    } else {
        Column(modifier = modifier.fillMaxSize().background(DarkBackground).padding(16.dp)) {
            CalendarPermissionRequest(calendarPermissionState)
        }
    }
}

@Composable
private fun CalendarView(modifier: Modifier, viewModel: JournalViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    var showLinksDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedMonth by remember { mutableStateOf(LocalDate.now().month) }
    var selectedYear by remember { mutableStateOf(LocalDate.now().year) }
    var searchQuery by remember { mutableStateOf("") }
    var entryToEdit by remember { mutableStateOf<JournalEntry?>(null) }
    val journalEntries by viewModel.getEntriesForDate(selectedDate ?: LocalDate.now()).collectAsState(initial = emptyList())
    val searchResults by viewModel.searchEntries(searchQuery).collectAsState(initial = emptyList())
    val context = LocalContext.current

    if (showDialog && selectedDate != null) {
        JournalEntryDialog(
            date = selectedDate!!,
            entryToEdit = entryToEdit,
            onDismiss = { showDialog = false; entryToEdit = null },
            onSave = { entry, imageUri, docUri, docName ->
                if (entryToEdit != null) {
                    viewModel.updateJournalEntry(entryToEdit!!.copy(text = entry, imageUri = imageUri, documentUri = docUri, documentName = docName))
                } else {
                    viewModel.addJournalEntry(JournalEntry(date = selectedDate!!, text = entry, imageUri = imageUri, documentUri = docUri, documentName = docName))
                }
                showDialog = false
                entryToEdit = null
            }
        )
    }

    if (showLinksDialog) {
        LinksDialog(onDismiss = { showLinksDialog = false })
    }

    Column(modifier = modifier.fillMaxSize().background(DarkBackground).padding(16.dp)) {
        Text(
            text = "Acataleptic Meditations",
            fontSize = 24.sp,
            color = TextColor,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).clickable { showLinksDialog = true },
            textAlign = TextAlign.Center
        )
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search Entries") },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(focusedTextColor = TextColor, unfocusedTextColor = TextColor, focusedContainerColor = DarkSurface, unfocusedContainerColor = DarkSurface),
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear Search", tint = TextColor)
                    }
                }
            }
        )

        if (searchQuery.isBlank()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            MonthDropdown(selectedMonth, modifier = Modifier.weight(1f), onMonthSelected = { selectedMonth = it })
                            YearDropdown(selectedYear, modifier = Modifier.weight(1f), onYearSelected = { selectedYear = it })
                        }
                        val daysOfWeek = remember { listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY) }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (day in daysOfWeek) {
                                Text(
                                    text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                    modifier = Modifier.weight(1f),
                                    color = TextColor,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        CalendarGrid(onDayClick = { date -> selectedDate = date }, daysOfWeek = daysOfWeek, month = selectedMonth, year = selectedYear)

                        Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                            Button(
                                onClick = { showDialog = true },
                                modifier = Modifier.fillMaxWidth(0.5f),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyber),
                                enabled = selectedDate != null
                            ) {
                                Text("New Journal Entry", color = Color.Black)
                            }
                        }
                    }
                }

                selectedDate?.let { date ->
                    if (journalEntries.isNotEmpty()) {
                        item {
                            Text(text = "Journal Entries for ${date.month.name} ${date.dayOfMonth}:", color = TextColor, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                        }
                        items(journalEntries) { entry ->
                            var isExpanded by remember { mutableStateOf(false) }
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = entry.text,
                                            modifier = Modifier.weight(1f).clickable { isExpanded = !isExpanded },
                                            color = TextColor,
                                            maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                                            overflow = if (isExpanded) TextOverflow.Clip else TextOverflow.Ellipsis
                                        )
                                        IconButton(onClick = {
                                            entryToEdit = entry
                                            showDialog = true
                                        }) {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Entry", tint = PrimaryCyber)
                                        }
                                        IconButton(onClick = { viewModel.deleteJournalEntry(entry) }) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Entry", tint = SecondaryCyber)
                                        }
                                    }
                                    if (isExpanded) {
                                        if (entry.imageUri != null) {
                                            AsyncImage(
                                                model = entry.imageUri,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxWidth().height(200.dp).padding(top = 8.dp).clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                        if (entry.documentUri != null) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable {
                                                    try {
                                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                                            data = Uri.parse(entry.documentUri)
                                                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                        }
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        // Handle error
                                                    }
                                                },
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = PrimaryCyber)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(entry.documentName ?: "View Document", color = TextColor, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
                items(searchResults) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                            selectedDate = entry.date
                            selectedMonth = entry.date.month
                            selectedYear = entry.date.year
                            searchQuery = ""
                        },
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("${entry.date.monthValue}/${entry.date.dayOfMonth}/${entry.date.year}", color = PrimaryCyber, fontSize = 12.sp)
                            Text(entry.text, color = TextColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CalendarPermissionRequest(calendarPermissionState: com.google.accompanist.permissions.PermissionState) {
    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("This app needs calendar permission to function.", color = TextColor, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { calendarPermissionState.launchPermissionRequest() }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyber)) {
            Text("Request permission", color = Color.Black)
        }
    }
}

@Composable
fun CalendarGrid(onDayClick: (LocalDate) -> Unit, daysOfWeek: List<DayOfWeek>, month: Month, year: Int) {
    val currentMonth = YearMonth.of(year, month)
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = currentMonth.atDay(1).dayOfWeek
    val emptyDays = daysOfWeek.indexOf(firstDayOfMonth)
    val today = LocalDate.now()

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.height(300.dp)
    ) {
        items(emptyDays) { Spacer(modifier = Modifier.padding(8.dp)) }
        items(daysInMonth) { day ->
            val dayOfMonth = day + 1
            val date = currentMonth.atDay(dayOfMonth)
            val isToday = date == today

            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(if (isToday) PrimaryCyber else Color.Transparent)
                    .clickable { onDayClick(date) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dayOfMonth.toString(),
                    color = if (isToday) Color.Black else TextColor,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun JournalEntryDialog(date: LocalDate, entryToEdit: JournalEntry?, onDismiss: () -> Unit, onSave: (String, String?, String?, String?) -> Unit) {
    var text by remember { mutableStateOf(entryToEdit?.text ?: "") }
    var imageUri by remember { mutableStateOf(entryToEdit?.imageUri) }
    var documentUri by remember { mutableStateOf(entryToEdit?.documentUri) }
    var documentName by remember { mutableStateOf(entryToEdit?.documentName) }
    
    val context = LocalContext.current
    
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> imageUri = uri?.toString() }

    val docLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        documentUri = uri?.toString()
        uri?.let {
            context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    documentName = cursor.getString(nameIndex)
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Journal Entry for ${date.month.name} ${date.dayOfMonth}, ${date.year}", color = TextColor) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Write your entry") },
                    colors = TextFieldDefaults.colors(focusedTextColor = TextColor, unfocusedTextColor = TextColor, focusedContainerColor = DarkBackground, unfocusedContainerColor = DarkBackground),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Image Section
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    TextButton(onClick = { imageUri = null }) {
                        Text("Remove Image", color = SecondaryCyber)
                    }
                }
                Button(
                    onClick = { imageLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurface)
                ) {
                    Text(if (imageUri == null) "Add Image" else "Change Image", color = TextColor)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Document Section
                if (documentUri != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = PrimaryCyber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(documentName ?: "Document attached", color = TextColor, modifier = Modifier.weight(1f))
                        IconButton(onClick = { 
                            documentUri = null
                            documentName = null
                        }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Remove Document", tint = SecondaryCyber)
                        }
                    }
                }
                Button(
                    onClick = { docLauncher.launch("*/*") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurface)
                ) {
                    Text(if (documentUri == null) "Add Document" else "Change Document", color = TextColor)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(text, imageUri, documentUri, documentName) }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyber)) {
                Text("Save", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextColor)
            }
        },
        containerColor = DarkSurface
    )
}

@Composable
fun LinksDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "About Us", color = PrimaryCyber) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "Welcome to Acataleptic Meditations - A journal where you can keep your thoughts. The Oregon Motivation Center is dedicated to helping people like yourself feel centered. The founder of The Oregon Motivation Center is also an author, Blaine Lambert has written a number of books on Hypnosis and various change work methodologies as well as a series of fictional books based in the world of Rimvale! Thank you for including us in your life journey!",
                    color = TextColor,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Button(onClick = { val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.oregonmotivationcenter.com")); context.startActivity(intent) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = DarkBackground)) {
                    Text("Oregon Motivation Center", color = TextColor)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.rimvale.com")); context.startActivity(intent) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = DarkBackground)) {
                    Text("Rimvale", color = TextColor)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com/artist/4ULtfceAPZd1jB5YBeL4cG")); context.startActivity(intent) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = DarkBackground)) {
                    Text("Spotify", color = TextColor)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://music.youtube.com/search?q=blaine+lambert")); context.startActivity(intent) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = DarkBackground)) {
                    Text("Youtube", color = TextColor)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.qobuz.com/us-en/interpreter/blaine-lambert/26425789")); context.startActivity(intent) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = DarkBackground)) {
                    Text("Qobuz", color = TextColor)
                }
            }
        },
        confirmButton = { },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close", color = TextColor) } },
        containerColor = DarkSurface
    )
}

@Composable
fun MonthDropdown(selectedMonth: Month, modifier: Modifier = Modifier, onMonthSelected: (Month) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier.padding(4.dp)) {
        Button(onClick = { expanded = true }, colors = ButtonDefaults.buttonColors(containerColor = DarkSurface), modifier = Modifier.fillMaxWidth()) {
            Text(selectedMonth.getDisplayName(TextStyle.FULL, Locale.getDefault()), color = TextColor)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(DarkSurface)
        ) {
            Month.entries.forEach { month ->
                DropdownMenuItem(text = { Text(month.getDisplayName(TextStyle.FULL, Locale.getDefault()), color = TextColor) }, onClick = {
                    onMonthSelected(month)
                    expanded = false
                })
            }
        }
    }
}

@Composable
fun YearDropdown(selectedYear: Int, modifier: Modifier = Modifier, onYearSelected: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val years = (2000..2050).toList()
    Box(modifier = modifier.padding(4.dp)) {
        Button(onClick = { expanded = true }, colors = ButtonDefaults.buttonColors(containerColor = DarkSurface), modifier = Modifier.fillMaxWidth()) {
            Text(selectedYear.toString(), color = TextColor)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(DarkSurface)
        ) {
            years.forEach { year ->
                DropdownMenuItem(text = { Text(year.toString(), color = TextColor) }, onClick = {
                    onYearSelected(year)
                    expanded = false
                })
            }
        }
    }
}
