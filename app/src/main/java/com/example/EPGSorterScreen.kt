package com.example

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.ui.theme.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EPGSorterScreen(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isExpanded = configuration.screenWidthDp >= 600

    val coroutineScope = rememberCoroutineScope()

    // Sorter UI Local States
    var activeSubTab by remember { mutableStateOf(0) } // 0 = Categories, 1 = Channels
    var searchQuery by remember { mutableStateOf("") }

    // Selected items for the details pane (Right side)
    val rawCategories by viewModel.allRawIptvCategories.collectAsState()
    val rawChannels by viewModel.iptvChannels.collectAsState()

    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var selectedChannelId by remember { mutableStateOf<String?>(null) }

    // Resolve selection fallbacks
    LaunchedEffect(rawCategories) {
        if (selectedCategoryId == null && rawCategories.isNotEmpty()) {
            selectedCategoryId = rawCategories.first().id
        }
    }
    LaunchedEffect(rawChannels) {
        if (selectedChannelId == null && rawChannels.isNotEmpty()) {
            selectedChannelId = rawChannels.first().id
        }
    }

    Scaffold(
        containerColor = ObsidianSurface,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = null,
                            tint = IndigoPrimary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column {
                            Text(
                                text = "IPTV & EPG Planner",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Customize groupings, order, and channel scheduling visibility",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("sorter_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Close Planner",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianSurface),
                modifier = Modifier.drawBehind {
                    drawLine(
                        color = Color.White.copy(alpha = 0.05f),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            )
        }
    ) { paddingValues ->
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT PANEL: List Reorder Editor (60% width on Expanded width, full width on compact phone width)
            Box(
                modifier = Modifier
                    .weight(if (isExpanded) 0.58f else 1f)
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Tab Selectors (Categories vs Channels)
                    TabRow(
                        selectedTabIndex = activeSubTab,
                        containerColor = ObsidianSurface,
                        contentColor = Color.White,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[activeSubTab]),
                                color = IndigoPrimary
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Tab(
                            selected = activeSubTab == 0,
                            onClick = { activeSubTab = 0; searchQuery = "" },
                            text = { Text("Group Categories", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                            icon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("sorter_tab_category")
                        )
                        Tab(
                            selected = activeSubTab == 1,
                            onClick = { activeSubTab = 1; searchQuery = "" },
                            text = { Text("Stream Channels", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                            icon = { Icon(Icons.Default.Tv, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("sorter_tab_channel")
                        )
                    }

                    // Search and Filters
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search list...", color = TextMuted, fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp)) },
                            trailingIcon = if (searchQuery.isNotEmpty()) {
                                {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(16.dp))
                                    }
                                }
                            } else null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IndigoPrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.10f),
                                focusedContainerColor = Color.White.copy(alpha = 0.02f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.02f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("sorter_search_input"),
                            singleLine = true
                        )

                        // Sort Mode Selector based on active sub tab
                        if (activeSubTab == 0) {
                            CategorySortDropdown(viewModel = viewModel)
                        } else {
                            ChannelSortDropdown(viewModel = viewModel)
                        }
                    }

                    // Content Editor list
                    Box(modifier = Modifier.weight(1f)) {
                        if (activeSubTab == 0) {
                            CategoryReorderEditor(
                                viewModel = viewModel,
                                searchQuery = searchQuery,
                                selectedId = selectedCategoryId,
                                onSelect = { selectedCategoryId = it },
                                isExpanded = isExpanded
                            )
                        } else {
                            ChannelReorderEditor(
                                viewModel = viewModel,
                                searchQuery = searchQuery,
                                selectedId = selectedChannelId,
                                onSelect = { selectedChannelId = it },
                                isExpanded = isExpanded
                            )
                        }
                    }
                }
            }

            // RIGHT PANEL: Spacious Preview/Details (Only shown on expanded/foldable width)
            if (isExpanded) {
                Box(
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight()
                        .padding(start = 8.dp, end = 20.dp, top = 20.dp, bottom = 20.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        AnimatedContent(
                            targetState = Pair(activeSubTab, if (activeSubTab == 0) selectedCategoryId else selectedChannelId),
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            },
                            label = "DetailAnimator"
                        ) { (tab, targetId) ->
                            if (tab == 0) {
                                val catObj = rawCategories.find { it.id == targetId }
                                CategoryDetailsView(
                                    category = catObj,
                                    channels = rawChannels,
                                    viewModel = viewModel
                                )
                            } else {
                                val chObj = rawChannels.find { it.id == targetId }
                                val categoriesList by viewModel.allRawIptvCategories.collectAsState()
                                val epgList by viewModel.epgProgrammes.collectAsState()
                                ChannelDetailsView(
                                    channel = chObj,
                                    categories = categoriesList,
                                    epgList = epgList
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// DROPDOWN FOR CATEGORIES SORT MODE SELECTOR
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySortDropdown(viewModel: MainViewModel) {
    val sortMode by viewModel.categorySortMode.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    Box {
        Button(
            onClick = { expanded = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.height(50.dp).testTag("category_sort_mode_select"),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            contentPadding = PaddingValues(horizontal = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.FilterList, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(16.dp))
                Text(sortMode.displayName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(ObsidianSurface).border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
        ) {
            CategorySortMode.values().forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.displayName, color = Color.White, fontSize = 13.sp) },
                    onClick = {
                        viewModel.setCategorySortMode(mode)
                        expanded = false
                    },
                    modifier = Modifier.testTag("sort_cat_${mode.name}")
                )
            }
        }
    }
}

// DROPDOWN FOR CHANNELS SORT MODE SELECTOR
@Composable
fun ChannelSortDropdown(viewModel: MainViewModel) {
    val sortMode by viewModel.channelSortMode.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    Box {
        Button(
            onClick = { expanded = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.height(50.dp).testTag("channel_sort_mode_select"),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            contentPadding = PaddingValues(horizontal = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.FilterList, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(16.dp))
                Text(sortMode.displayName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(ObsidianSurface).border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
        ) {
            ChannelSortMode.values().forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.displayName, color = Color.White, fontSize = 13.sp) },
                    onClick = {
                        viewModel.setChannelSortMode(mode)
                        expanded = false
                    },
                    modifier = Modifier.testTag("sort_chan_${mode.name}")
                )
            }
        }
    }
}

// CATEGORY REORDER LIST CONTAINER
@Composable
fun CategoryReorderEditor(
    viewModel: MainViewModel,
    searchQuery: String,
    selectedId: String?,
    onSelect: (String) -> Unit,
    isExpanded: Boolean
) {
    val rawCategories by viewModel.allRawIptvCategories.collectAsState()
    val customOrder by viewModel.customCategoryOrder.collectAsState()
    val hiddenIds by viewModel.hiddenCategoryIds.collectAsState()
    val sortMode by viewModel.categorySortMode.collectAsState()

    // Single-pane phone fallback dialog display for details review
    var phoneDetailCategoryToShow by remember { mutableStateOf<IptvCategory?>(null) }
    val channels by viewModel.iptvChannels.collectAsState()

    val sortedCategories = remember(rawCategories, customOrder, sortMode) {
        val list = rawCategories.toList()
        when (sortMode) {
            CategorySortMode.PROVIDER -> list
            CategorySortMode.CUSTOM -> {
                val orderMap = customOrder.withIndex().associate { it.value to it.index }
                list.sortedWith(
                    compareBy<IptvCategory> { orderMap[it.id] ?: Int.MAX_VALUE }
                        .thenBy { rawCategories.indexOf(it) }
                )
            }
            CategorySortMode.NAME_AZ -> list.sortedBy { it.name.lowercase() }
            CategorySortMode.FAVORITES_FIRST -> list // custom grouping, fallback
        }
    }

    val filteredList = remember(sortedCategories, searchQuery) {
        if (searchQuery.isBlank()) {
            sortedCategories
        } else {
            sortedCategories.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    // Modal view for phone width
    if (phoneDetailCategoryToShow != null) {
        AlertDialog(
            onDismissRequest = { phoneDetailCategoryToShow = null },
            confirmButton = {
                TextButton(onClick = { phoneDetailCategoryToShow = null }) { Text("Close", color = IndigoPrimary) }
            },
            title = { Text(phoneDetailCategoryToShow!!.name, color = Color.White, fontSize = 16.sp) },
            text = {
                val catChs = channels.filter { it.categoryId == phoneDetailCategoryToShow!!.id }
                val isHidden = hiddenIds.contains(phoneDetailCategoryToShow!!.id)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Total Channels: ${catChs.size}", color = TextSilver, fontSize = 13.sp)
                    Text("Status: ${if (isHidden) "Hidden 👁️‍" else "Visible ✅"}", color = TextSilver, fontSize = 13.sp)
                    Text("Category ID: ${phoneDetailCategoryToShow!!.id}", color = TextMuted, fontSize = 11.sp)
                    Divider(color = Color.White.copy(alpha = 0.05f))
                    Text("Channels in group:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.heightIn(max = 200.dp)) {
                        LazyColumn {
                            itemsIndexed(catChs) { index, ch ->
                                Text("${index + 1}. ${ch.name}", color = TextSilver, fontSize = 12.sp, maxLines = 1, modifier = Modifier.padding(vertical = 2.dp))
                            }
                        }
                    }
                }
            },
            containerColor = ObsidianSurface
        )
    }

    if (filteredList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No categories match your query.", color = TextMuted, fontSize = 13.sp)
        }
    } else {
        val listState = rememberLazyListState()

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(filteredList, key = { _, cat -> cat.id }) { index, cat ->
                val isSelected = cat.id == selectedId
                val isHidden = hiddenIds.contains(cat.id)

                // Drag gesture handling state wrapper
                var draggingOffsetY by remember { mutableStateOf(0f) }
                val dragThreshold = 180f // Threshold in px to trigger swap index

                CategoryEditorItemRow(
                    category = cat,
                    index = index,
                    totalCount = filteredList.size,
                    isSelected = isSelected,
                    isHidden = isHidden,
                    isCustomSortMode = (sortMode == CategorySortMode.CUSTOM),
                    offsetY = draggingOffsetY,
                    onStartDrag = {
                        draggingOffsetY = 0f
                    },
                    onDrag = { dy ->
                        draggingOffsetY += dy
                        val currentY = draggingOffsetY
                        if (sortMode == CategorySortMode.CUSTOM) {
                            if (currentY > dragThreshold && index < filteredList.size - 1) {
                                val newList = sortedCategories.map { it.id }.toMutableList()
                                val targetIdx = newList.indexOf(cat.id)
                                if (targetIdx != -1 && targetIdx < newList.size - 1) {
                                    val temp = newList[targetIdx]
                                    newList[targetIdx] = newList[targetIdx + 1]
                                    newList[targetIdx + 1] = temp
                                    viewModel.updateIptvCategoryOrder(newList)
                                    draggingOffsetY = 0f
                                }
                            } else if (currentY < -dragThreshold && index > 0) {
                                val newList = sortedCategories.map { it.id }.toMutableList()
                                val targetIdx = newList.indexOf(cat.id)
                                if (targetIdx != -1 && targetIdx > 0) {
                                    val temp = newList[targetIdx]
                                    newList[targetIdx] = newList[targetIdx - 1]
                                    newList[targetIdx - 1] = temp
                                    viewModel.updateIptvCategoryOrder(newList)
                                    draggingOffsetY = 0f
                                }
                            }
                        }
                    },
                    onEndDrag = {
                        draggingOffsetY = 0f
                    },
                    onMoveUp = {
                        val newList = sortedCategories.map { it.id }.toMutableList()
                        val targetIdx = newList.indexOf(cat.id)
                        if (targetIdx > 0) {
                            val temp = newList[targetIdx]
                            newList[targetIdx] = newList[targetIdx - 1]
                            newList[targetIdx - 1] = temp
                            viewModel.updateIptvCategoryOrder(newList)
                        }
                    },
                    onMoveDown = {
                        val newList = sortedCategories.map { it.id }.toMutableList()
                        val targetIdx = newList.indexOf(cat.id)
                        if (targetIdx < newList.size - 1) {
                            val temp = newList[targetIdx]
                            newList[targetIdx] = newList[targetIdx + 1]
                            newList[targetIdx + 1] = temp
                            viewModel.updateIptvCategoryOrder(newList)
                        }
                    },
                    onToggleVisibility = {
                        viewModel.toggleIptvCategoryVisibility(cat.id)
                    },
                    onClick = {
                        onSelect(cat.id)
                        if (!isExpanded) {
                            phoneDetailCategoryToShow = cat
                        }
                    }
                )
            }
        }
    }
}

// CHANNEL REORDER LIST CONTAINER
@Composable
fun ChannelReorderEditor(
    viewModel: MainViewModel,
    searchQuery: String,
    selectedId: String?,
    onSelect: (String) -> Unit,
    isExpanded: Boolean
) {
    val rawChannels by viewModel.iptvChannels.collectAsState()
    val customOrder by viewModel.customChannelOrder.collectAsState()
    val sortMode by viewModel.channelSortMode.collectAsState()

    // Fallbacks state variables for compact mobile displays
    var phoneDetailChannelToShow by remember { mutableStateOf<IptvChannel?>(null) }
    val categoriesList by viewModel.allRawIptvCategories.collectAsState()
    val epgList by viewModel.epgProgrammes.collectAsState()

    val sortedChannels = remember(rawChannels, customOrder, sortMode) {
        val list = rawChannels.toList()
        when (sortMode) {
            ChannelSortMode.PROVIDER -> list
            ChannelSortMode.CUSTOM -> {
                val orderMap = customOrder.withIndex().associate { it.value to it.index }
                list.sortedWith(
                    compareBy<IptvChannel> { orderMap[it.id] ?: Int.MAX_VALUE }
                        .thenBy { rawChannels.indexOf(it) }
                )
            }
            ChannelSortMode.NAME_AZ -> list.sortedBy { it.name.lowercase() }
            ChannelSortMode.FAVORITES_FIRST -> list.sortedWith(
                compareByDescending<IptvChannel> { it.isFavorite }
                    .thenBy { rawChannels.indexOf(it) }
            )
            ChannelSortMode.CHANNEL_NUMBER -> list.sortedBy { ch ->
                val numberPart = ch.name.takeWhile { it.isDigit() }
                if (numberPart.isNotEmpty()) {
                    numberPart.toIntOrNull() ?: rawChannels.indexOf(ch)
                } else {
                    rawChannels.indexOf(ch)
                }
            }
        }
    }

    val filteredList = remember(sortedChannels, searchQuery) {
        if (searchQuery.isBlank()) {
            sortedChannels
        } else {
            sortedChannels.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    // Phone Overlay Dialog for details preview
    if (phoneDetailChannelToShow != null) {
        val parentCategoryName = categoriesList.find { it.id == phoneDetailChannelToShow!!.categoryId }?.name ?: "Default / Uncategorized"
        AlertDialog(
            onDismissRequest = { phoneDetailChannelToShow = null },
            confirmButton = {
                TextButton(onClick = { phoneDetailChannelToShow = null }) { Text("Close", color = IndigoPrimary) }
            },
            title = { Text(phoneDetailChannelToShow!!.name, color = Color.White, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Category: $parentCategoryName", color = TextSilver, fontSize = 13.sp)
                    Text("Is Favorite: ${if (phoneDetailChannelToShow!!.isFavorite) "⭐ Yes" else "No"}", color = TextSilver, fontSize = 13.sp)
                    Text("ID: ${phoneDetailChannelToShow!!.id}", color = TextMuted, fontSize = 11.sp, maxLines = 1)
                    Text("Stream URI: ${phoneDetailChannelToShow!!.streamUrl}", color = TextMuted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Divider(color = Color.White.copy(alpha = 0.05f))
                    Text("Current EPG Scheduling Map:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    val channelShows = epgList.filter { it.channelId == phoneDetailChannelToShow!!.id || it.channelId == phoneDetailChannelToShow!!.epgId }
                    if (channelShows.isEmpty()) {
                        Text("No scheduling telemetry active on this group.", color = TextMuted, fontSize = 11.sp)
                    } else {
                        Box(modifier = Modifier.heightIn(max = 180.dp)) {
                            LazyColumn {
                                itemsIndexed(channelShows.sortedBy { it.startMs }) { _, prog ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(prog.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                            val startLabel = Instant.ofEpochMilli(prog.startMs).toString().substring(11,16)
                                            val endLabel = Instant.ofEpochMilli(prog.endMs).toString().substring(11,16)
                                            Text("$startLabel - $endLabel UTC", color = IndigoPrimary, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold)
                                            prog.description?.let { Text(it, color = TextMuted, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            containerColor = ObsidianSurface
        )
    }

    if (filteredList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No channels match your query.", color = TextMuted, fontSize = 13.sp)
        }
    } else {
        val listState = rememberLazyListState()

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(filteredList, key = { _, ch -> ch.id }) { index, ch ->
                val isSelected = ch.id == selectedId

                var draggingOffsetY by remember { mutableStateOf(0f) }
                val dragThreshold = 180f

                ChannelEditorItemRow(
                    channel = ch,
                    index = index,
                    totalCount = filteredList.size,
                    isSelected = isSelected,
                    isCustomSortMode = (sortMode == ChannelSortMode.CUSTOM),
                    offsetY = draggingOffsetY,
                    onStartDrag = {
                        draggingOffsetY = 0f
                    },
                    onDrag = { dy ->
                        draggingOffsetY += dy
                        val currentY = draggingOffsetY
                        if (sortMode == ChannelSortMode.CUSTOM) {
                            if (currentY > dragThreshold && index < filteredList.size - 1) {
                                val newList = sortedChannels.map { it.id }.toMutableList()
                                val targetIdx = newList.indexOf(ch.id)
                                if (targetIdx != -1 && targetIdx < newList.size - 1) {
                                    val temp = newList[targetIdx]
                                    newList[targetIdx] = newList[targetIdx + 1]
                                    newList[targetIdx + 1] = temp
                                    viewModel.updateIptvChannelOrder(newList)
                                    draggingOffsetY = 0f
                                }
                            } else if (currentY < -dragThreshold && index > 0) {
                                val newList = sortedChannels.map { it.id }.toMutableList()
                                val targetIdx = newList.indexOf(ch.id)
                                if (targetIdx != -1 && targetIdx > 0) {
                                    val temp = newList[targetIdx]
                                    newList[targetIdx] = newList[targetIdx - 1]
                                    newList[targetIdx - 1] = temp
                                    viewModel.updateIptvChannelOrder(newList)
                                    draggingOffsetY = 0f
                                }
                            }
                        }
                    },
                    onEndDrag = {
                        draggingOffsetY = 0f
                    },
                    onMoveUp = {
                        val newList = sortedChannels.map { it.id }.toMutableList()
                        val targetIdx = newList.indexOf(ch.id)
                        if (targetIdx > 0) {
                            val temp = newList[targetIdx]
                            newList[targetIdx] = newList[targetIdx - 1]
                            newList[targetIdx - 1] = temp
                            viewModel.updateIptvChannelOrder(newList)
                        }
                    },
                    onMoveDown = {
                        val newList = sortedChannels.map { it.id }.toMutableList()
                        val targetIdx = newList.indexOf(ch.id)
                        if (targetIdx < newList.size - 1) {
                            val temp = newList[targetIdx]
                            newList[targetIdx] = newList[targetIdx + 1]
                            newList[targetIdx + 1] = temp
                            viewModel.updateIptvChannelOrder(newList)
                        }
                    },
                    onToggleFav = {
                        viewModel.toggleFavoriteChannel(ch.id)
                    },
                    onClick = {
                        onSelect(ch.id)
                        if (!isExpanded) {
                            phoneDetailChannelToShow = ch
                        }
                    }
                )
            }
        }
    }
}

// SINGLE ROW CARD FOR CATEGORIES LIST
@Composable
fun CategoryEditorItemRow(
    category: IptvCategory,
    index: Int,
    totalCount: Int,
    isSelected: Boolean,
    isHidden: Boolean,
    isCustomSortMode: Boolean,
    offsetY: Float,
    onStartDrag: () -> Unit,
    onDrag: (Float) -> Unit,
    onEndDrag: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onToggleVisibility: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color.White.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.01f)
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) IndigoPrimary else Color.White.copy(alpha = 0.05f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(0, offsetY.toInt()) }
            .clickable { onClick() }
            .testTag("reorder_item_${category.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag Handle for gesture drag & drop reorder
            if (isCustomSortMode) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.02f))
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { onStartDrag() },
                                onDragEnd = { onEndDrag() },
                                onDragCancel = { onEndDrag() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    onDrag(dragAmount.y)
                                }
                            )
                        }
                        .testTag("drag_handle_${category.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.DragHandle, contentDescription = "Hold to Drag", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
            }

            // Visual Status Icon Indicator
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isHidden) Color.Black.copy(alpha = 0.3f) else IndigoPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Folder,
                    contentDescription = null,
                    tint = if (isHidden) Color.DarkGray else IndigoPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text Titles
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    color = if (isHidden) TextMuted else Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "ID: ${category.id}",
                    color = TextMuted,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }

            // Direct Click Actions for Sorting & Visibility Toggle
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Eye button to toggle visible/hidden
                IconButton(onClick = onToggleVisibility, modifier = Modifier.size(32.dp).testTag("visibility_toggle_${category.id}")) {
                    Icon(
                        imageVector = if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Hide Filter",
                        tint = if (isHidden) Color.Red.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (isCustomSortMode) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = index > 0,
                        modifier = Modifier.size(32.dp).testTag("move_up_${category.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Move Up",
                            tint = if (index > 0) Color.White else Color.DarkGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onMoveDown,
                        enabled = index < totalCount - 1,
                        modifier = Modifier.size(32.dp).testTag("move_down_${category.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Move Down",
                            tint = if (index < totalCount - 1) Color.White else Color.DarkGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// SINGLE ROW CARD FOR CHANNELS LIST
@Composable
fun ChannelEditorItemRow(
    channel: IptvChannel,
    index: Int,
    totalCount: Int,
    isSelected: Boolean,
    isCustomSortMode: Boolean,
    offsetY: Float,
    onStartDrag: () -> Unit,
    onDrag: (Float) -> Unit,
    onEndDrag: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onToggleFav: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color.White.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.01f)
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) IndigoPrimary else Color.White.copy(alpha = 0.05f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(0, offsetY.toInt()) }
            .clickable { onClick() }
            .testTag("reorder_item_${channel.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag handle modifier
            if (isCustomSortMode) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.02f))
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { onStartDrag() },
                                onDragEnd = { onEndDrag() },
                                onDragCancel = { onEndDrag() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    onDrag(dragAmount.y)
                                }
                            )
                        }
                        .testTag("drag_handle_${channel.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.DragHandle, contentDescription = "Hold to Drag", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
            }

            // Visual placeholder for logo preview
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Tv, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(16.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Channel titles
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "ID: ${channel.id}",
                    color = TextMuted,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }

            // Quick interaction arrow reorder panels
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onToggleFav, modifier = Modifier.size(32.dp).testTag("favorite_toggle_${channel.id}")) {
                    Icon(
                        imageVector = if (channel.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Toggle Favorite",
                        tint = if (channel.isFavorite) Color.Yellow else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (isCustomSortMode) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = index > 0,
                        modifier = Modifier.size(32.dp).testTag("move_up_${channel.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Move Up",
                            tint = if (index > 0) Color.White else Color.DarkGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onMoveDown,
                        enabled = index < totalCount - 1,
                        modifier = Modifier.size(32.dp).testTag("move_down_${channel.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Move Down",
                            tint = if (index < totalCount - 1) Color.White else Color.DarkGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// RIGHT DETAILS CONTENT FOR CATEGORIES
@Composable
fun CategoryDetailsView(
    category: IptvCategory?,
    channels: List<IptvChannel>,
    viewModel: MainViewModel
) {
    if (category == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select a category grouping to view details", color = TextMuted, fontSize = 13.sp)
        }
        return
    }

    val catChannels = remember(channels, category.id) {
        channels.filter { it.categoryId == category.id }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        // Aesthetic header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(IndigoPrimary.copy(alpha = 0.15f), Color.Transparent)
                    )
                )
                .border(1.dp, IndigoPrimary.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(IndigoPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(category.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Type ID: ${category.type}", color = TextMuted, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Basic Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Channels", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    Text("${catChannels.size}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
            ) {
                val hiddenIds by viewModel.hiddenCategoryIds.collectAsState()
                val isHidden = hiddenIds.contains(category.id)
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Guide Visibility", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (isHidden) "Hidden" else "Visible",
                        color = if (isHidden) Color.Red.copy(alpha = 0.8f) else Color.Green.copy(alpha = 0.8f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Divider(color = Color.White.copy(alpha = 0.05f))
        Spacer(modifier = Modifier.height(14.dp))

        Text("Sub-Channel Directory Placement:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        if (catChannels.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Zero stream lines matching group category.", color = TextMuted, fontSize = 12.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(catChannels) { index, ch ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${index + 1}", color = IndigoPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                            Text(ch.name, color = TextSilver, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

// RIGHT DETAILS CONTENT FOR CHANNELS
@Composable
fun ChannelDetailsView(
    channel: IptvChannel?,
    categories: List<IptvCategory>,
    epgList: List<EpgProgramme>
) {
    if (channel == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select a channel to review scheduling grid details", color = TextMuted, fontSize = 13.sp)
        }
        return
    }

    val belongsToCategory = categories.find { it.id == channel.categoryId }?.name ?: "Default / Uncategorized"
    val channelEpgs = epgList.filter { it.channelId == channel.id || it.channelId == channel.epgId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        // Aesthetic Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(IndigoPrimary.copy(alpha = 0.15f), Color.Transparent)
                    )
                )
                .border(1.dp, IndigoPrimary.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Tv, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(channel.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Category: $belongsToCategory", color = TextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // General Technical Telemetry Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("TECHNICAL SPECS", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text("Channel ID: ${channel.id}", color = TextSilver, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("EPG Ref: ${channel.epgId ?: "Unspecified"}", color = TextSilver, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Format Type: HTTP Live Stream (M3U8)", color = TextSilver, fontSize = 11.sp)
                Text("Favorite Class: ${if (channel.isFavorite) "Starred Favorite ⭐" else "Standard Stream Line"}", color = TextSilver, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = Color.White.copy(alpha = 0.05f))
        Spacer(modifier = Modifier.height(12.dp))

        Text("ACTIVE SCHEDULING GUIDES", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        if (channelEpgs.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No scheduling metadata loaded on this carrier line.", color = TextMuted, fontSize = 12.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(channelEpgs.sortedBy { it.startMs }) { _, prog ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(prog.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            val startLabel = Instant.ofEpochMilli(prog.startMs).toString().substring(11, 16)
                            val endLabel = Instant.ofEpochMilli(prog.endMs).toString().substring(11, 16)
                            Text("$startLabel - $endLabel UTC", color = IndigoPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            prog.description?.let {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(it, color = TextMuted, fontSize = 10.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}
