package com.devansh.alarm.ui

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val ROW_HEIGHT: Dp = 40.dp
private const val VISIBLE_ROWS = 3

/**
 * Vertical snapping wheel. Shows [VISIBLE_ROWS] rows; the centered row is selected.
 * Calls [onSelected] with the item index whenever the wheel settles.
 */
@Composable
fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 72.dp,
) {
    val state = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedIndex.coerceIn(items.indices),
    )
    LaunchedEffect(state, items.size) {
        snapshotFlow { state.isScrollInProgress to state.firstVisibleItemIndex }
            .collect { (scrolling, first) ->
                if (!scrolling) onSelected(first.coerceIn(items.indices))
            }
    }
    Box(modifier = modifier.width(width).height(ROW_HEIGHT * VISIBLE_ROWS)) {
        LazyColumn(
            state = state,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = state),
            contentPadding = PaddingValues(vertical = ROW_HEIGHT), // 1 row above + below center
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(items.size) { index ->
                val isSelected = index == state.firstVisibleItemIndex
                Box(
                    modifier = Modifier.fillMaxWidth().height(ROW_HEIGHT),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        items[index],
                        style = if (isSelected) MaterialTheme.typography.headlineSmall
                        else MaterialTheme.typography.titleMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
