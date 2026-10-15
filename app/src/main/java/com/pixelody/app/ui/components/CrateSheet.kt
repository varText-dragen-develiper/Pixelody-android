package com.pixelody.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelody.app.data.model.CRATE_SLOT_COUNT
import com.pixelody.app.data.model.Crate
import com.pixelody.app.data.model.CrateSlot
import com.pixelody.app.ui.navigation.PixelodyStateTags

/**
 * The opened crate: nine addresses in a fixed grid.
 *
 * It is an overlay, never a destination. Opening a crate is a zoom, not a trip —
 * the person is still wherever they were, so Back has nothing to restore and there
 * is no return journey to remember. Anything in here that needs to move them closes
 * the crate first and lets the layer that owns the move perform it.
 *
 * Holes are drawn as holes rather than closed up. Unavailable things keep their
 * address and say so. Both are the same rule: an address a person has learned is
 * not ours to reassign.
 */
@Composable
fun CrateSheet(
    crate: Crate,
    resolve: (CrateSlot) -> CrateSlotFace?,
    onOpenSlot: (Int, CrateSlot) -> Unit,
    onPlayAll: () -> Unit,
    onRearrange: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    rearranging: Boolean = false,
    selectedForMove: Int? = null,
    onPickForMove: (Int) -> Unit = {},
    onSlotActions: (Int) -> Unit = {}
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(PixelodyStateTags.CRATE_SHEET),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = crate.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (rearranging) {
                        "Tap two slots to swap them. Nothing else moves."
                    } else {
                        "${crate.occupiedCount} of $CRATE_SLOT_COUNT slots"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (row in 0 until CRATE_SLOT_COUNT / 3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (column in 0 until 3) {
                            val index = row * 3 + column
                            CrateSlotCell(
                                index = index,
                                slot = crate.slotAt(index),
                                resolve = resolve,
                                rearranging = rearranging,
                                selected = selectedForMove == index,
                                onOpenSlot = onOpenSlot,
                                onPickForMove = onPickForMove,
                                onSlotActions = onSlotActions,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onPlayAll,
                    enabled = !rearranging && crate.occupiedCount > 0,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Play everything")
                }
                OutlinedButton(
                    onClick = onRearrange,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(PixelodyStateTags.CRATE_REARRANGE)
                ) {
                    Text(if (rearranging) "Done" else "Rearrange")
                }
            }

            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Close")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CrateSlotCell(
    index: Int,
    slot: CrateSlot?,
    resolve: (CrateSlot) -> CrateSlotFace?,
    rearranging: Boolean,
    selected: Boolean,
    onOpenSlot: (Int, CrateSlot) -> Unit,
    onPickForMove: (Int) -> Unit,
    onSlotActions: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val face = slot?.let(resolve)
    val position = index + 1
    val description = when {
        slot == null -> "Slot $position, empty"
        face == null || !face.available -> "Slot $position, ${face?.label ?: "unavailable"}, unavailable"
        else -> "Slot $position, ${face.label}, ${face.kindMark}"
    }

    val actionable = rearranging || (slot != null && face?.available == true)

    Surface(
        modifier = modifier
            .aspectRatio(1f)
            .combinedClickable(
                enabled = actionable || slot != null,
                onClickLabel = if (rearranging) "Choose slot ${index + 1}" else null,
                onLongClickLabel = if (slot == null) null else "Actions for slot ${index + 1}",
                role = Role.Button,
                onLongClick = if (slot == null || rearranging) null else {
                    { onSlotActions(index) }
                },
                onClick = {
                    if (rearranging) {
                        onPickForMove(index)
                    } else if (slot != null && actionable) {
                        onOpenSlot(index, slot)
                    }
                }
            )
            .semantics { contentDescription = description },
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            1.dp,
            if (slot == null) {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.42f)
            }
        ),
        shape = RoundedCornerShape(3.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = face?.badge ?: "",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (face?.available == false) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1
                )
                Text(
                    text = face?.label ?: if (rearranging) "empty" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
