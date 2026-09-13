package com.echoai.app.ui.summary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.echoai.app.data.local.Summary
import com.echoai.app.data.local.SummaryStatus
import com.echoai.app.ui.theme.Dimens
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import javax.inject.Inject

// Need Moshi to parse the JSON arrays
private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
private val listOfStringsType = Types.newParameterizedType(List::class.java, String::class.java)
private val jsonAdapter = moshi.adapter<List<String>>(listOfStringsType)

private val tabTitles = listOf("Summary", "Transcript")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    summaryViewModel: SummaryViewModel = hiltViewModel(),
    transcriptViewModel: TranscriptViewModel = hiltViewModel()
) {
    val uiState by summaryViewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Session Summary") })
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            when (selectedTab) {
                0 -> SummaryTab(uiState = uiState, onGenerate = { summaryViewModel.generateSummary() })
                1 -> TranscriptTab(viewModel = transcriptViewModel)
            }
        }
    }
}

@Composable
private fun SummaryTab(uiState: SummaryUiState, onGenerate: () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(Dimens.spacing_l)) {
        item {
            when (uiState) {
                is SummaryUiState.Loading -> Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                is SummaryUiState.Error -> ErrorState(message = uiState.message, onRetry = onGenerate)
                is SummaryUiState.Success -> {
                    val summary = uiState.summary
                    if (summary == null || summary.status == SummaryStatus.IDLE) {
                        InitialState(onGenerate = onGenerate)
                    } else {
                        SummaryContent(summary = summary)
                    }
                }
            }
        }
    }
}

@Composable
private fun TranscriptTab(viewModel: TranscriptViewModel) {
    val transcript by viewModel.transcript.collectAsState()

    if (transcript.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No transcript yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Dimens.spacing_l),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacing_s)
        ) {
            items(transcript) { line ->
                Text(line.text, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun InitialState(onGenerate: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("Ready to generate summary", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(Dimens.spacing_l))
        Button(onClick = onGenerate) {
            Text("Generate Summary")
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Column(modifier = Modifier.padding(Dimens.spacing_l), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Error Generating Summary", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(Dimens.spacing_s))
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(Dimens.spacing_l))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun SummaryContent(summary: Summary) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacing_l)) {
        if (summary.status == SummaryStatus.GENERATING || summary.status == SummaryStatus.STREAMING) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(Dimens.spacing_m))
                Text("Generating summary, please wait...")
            }
        }

        AnimatedVisibility(visible = summary.title.isNotEmpty()) {
            SummarySection(title = "Title") {
                Text(summary.title, style = MaterialTheme.typography.headlineSmall)
            }
        }
        AnimatedVisibility(visible = summary.summaryText.isNotEmpty()) {
            SummarySection(title = "Summary") {
                Text(summary.summaryText, style = MaterialTheme.typography.bodyLarge)
            }
        }

        val keyPoints = try { jsonAdapter.fromJson(summary.keyPointsJson) ?: emptyList() } catch (e: Exception) { emptyList() }
        AnimatedVisibility(visible = keyPoints.isNotEmpty()) {
            SummarySection(title = "Key Points") {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacing_xs)) {
                    keyPoints.forEach { point ->
                        Text("• $point", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        val actionItems = try { jsonAdapter.fromJson(summary.actionItemsJson) ?: emptyList() } catch (e: Exception) { emptyList() }
        AnimatedVisibility(visible = actionItems.isNotEmpty()) {
            SummarySection(title = "Action Items") {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacing_xs)) {
                    actionItems.forEach { item ->
                        Text("• $item", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun SummarySection(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(Dimens.elevation_card)) {
        Column(modifier = Modifier.padding(Dimens.spacing_l)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(Dimens.spacing_m))
            content()
        }
    }
}
