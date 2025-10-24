package com.example.echoai.ui.summary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.echoai.data.local.Summary
import com.example.echoai.data.local.SummaryStatus
import com.example.echoai.ui.theme.Dimens
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import javax.inject.Inject

// Need Moshi to parse the JSON arrays
private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
private val listOfStringsType = Types.newParameterizedType(List::class.java, String::class.java)
private val jsonAdapter = moshi.adapter<List<String>>(listOfStringsType)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    viewModel: SummaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Session Summary") })
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentPadding = PaddingValues(Dimens.spacing_l)) {
            item {
                when (val state = uiState) {
                    is SummaryUiState.Loading -> Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    is SummaryUiState.Error -> ErrorState(message = state.message, onRetry = { viewModel.generateSummary() })
                    is SummaryUiState.Success -> {
                        val summary = state.summary
                        if (summary == null || summary.status == SummaryStatus.IDLE) {
                            InitialState(onGenerate = { viewModel.generateSummary() })
                        } else {
                            SummaryContent(summary = summary)
                        }
                    }
                }
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
