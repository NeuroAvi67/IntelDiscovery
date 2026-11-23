package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.data.model.AccessType
import com.example.data.model.VerificationStatus
import com.example.data.model.WebsiteResource
import com.example.ui.theme.AmberLimited
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldFree
import com.example.ui.theme.RosePaid
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.VioletAi

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WebsiteIntelligenceCard(
    website: WebsiteResource,
    isCompared: Boolean,
    onVisit: () -> Unit,
    onToggleSave: () -> Unit,
    onAddToCollection: () -> Unit,
    onToggleCompare: () -> Unit,
    onAskAi: () -> Unit,
    onVerifyLive: () -> Unit,
    onEditNotes: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val accessColor = when (website.accessEnum) {
        AccessType.FREE -> EmeraldFree
        AccessType.LIMITED_ACCESS -> AmberLimited
        AccessType.PAID -> RosePaid
    }

    val verificationColor = when (website.verificationEnum) {
        VerificationStatus.VERIFIED -> EmeraldFree
        VerificationStatus.UNREACHABLE -> RosePaid
        VerificationStatus.UNVERIFIED -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = if (isCompared) 1.5.dp else 1.dp,
                color = if (isCompared) TacticalCyan else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("website_card_${website.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // LEFT SIDE ACCESS BADGE RAIL
            Box(
                modifier = Modifier
                    .width(34.dp)
                    .fillMaxHeight()
                    .background(accessColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(vertical = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(accessColor)
                    )
                    val shortCode = when (website.accessEnum) {
                        AccessType.FREE -> "F\nR\nE\nE"
                        AccessType.LIMITED_ACCESS -> "L\nI\nM\nI\nT\nE\nD"
                        AccessType.PAID -> "P\nA\nI\nD"
                    }
                    Text(
                        text = shortCode,
                        style = MaterialTheme.typography.labelSmall,
                        color = accessColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // MAIN INFORMATION COLUMN
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header: Favicon + Name + Domain + Score Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SiteFavicon(
                        faviconUrl = website.faviconUrl(),
                        siteName = website.name
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = website.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (website.aiPowered) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = "AI Powered",
                                    tint = VioletAi,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            if (website.openSource) {
                                Icon(
                                    imageVector = Icons.Filled.Code,
                                    contentDescription = "Open Source",
                                    tint = TacticalCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Text(
                            text = website.domain,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Recommendation Score Pill
                    Surface(
                        color = ElectricBlue.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.border(
                            1.dp,
                            ElectricBlue.copy(alpha = 0.4f),
                            RoundedCornerShape(10.dp)
                        )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${website.recommendationScore}",
                                style = MaterialTheme.typography.labelLarge,
                                color = ElectricBlue
                            )
                            Text(
                                text = "SCORE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Recommendation Label + Access Chip + Verification Status
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusBadgePill(
                        text = website.accessEnum.displayLabel,
                        color = accessColor
                    )
                    if (website.recommendationLabel.isNotBlank()) {
                        StatusBadgePill(
                            text = website.recommendationLabel,
                            color = TacticalCyan
                        )
                    }
                    StatusBadgePill(
                        text = buildString {
                            append(website.verificationEnum.displayLabel)
                            if (website.httpStatusCode > 0) append(" (${website.httpStatusCode})")
                        },
                        color = verificationColor,
                        onClick = onVerifyLive
                    )
                }

                // Description & Purpose
                Text(
                    text = website.description.ifBlank { website.purpose },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (website.purpose.isNotBlank()) {
                    Text(
                        text = "Purpose: ${website.purpose}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Categories & Key Features
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    website.categories.take(4).forEach { cat ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (website.keyFeatures.isNotEmpty()) {
                    Text(
                        text = "Key features: ${website.keyFeatures.take(4).joinToString(" • ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Pricing & Registration Metadata Line
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Pricing: ${website.pricingModel.ifBlank { "Check official site" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Reg: ${website.registrationEnum.displayLabel}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Last Checked & Source
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Checked: ${website.lastCheckedAt.ifBlank { "Pending" }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = website.sourceEnum.displayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                // Expandable Intelligence Evaluation Drawer
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expand_intel_${website.id}")
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (expanded) "Hide Intelligence Brief" else "Why Recommended, Pros/Cons & Verified Facts",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                AnimatedVisibility(visible = expanded) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                            .padding(12.dp)
                    ) {
                        if (website.whyRecommended.isNotBlank()) {
                            IntelDetailBlock("Why Recommended", website.whyRecommended)
                        }
                        if (website.freeTierLimitations.isNotBlank()) {
                            IntelDetailBlock("Pricing & Free-Tier Limits", website.freeTierLimitations)
                        }
                        if (website.advantages.isNotEmpty()) {
                            IntelDetailBlock("Advantages", website.advantages.joinToString("\n• ", prefix = "• "))
                        }
                        if (website.disadvantages.isNotEmpty()) {
                            IntelDetailBlock("Disadvantages", website.disadvantages.joinToString("\n• ", prefix = "• "))
                        }
                        if (website.whoShouldUse.isNotBlank()) {
                            IntelDetailBlock("Who Should Use It", website.whoShouldUse)
                        }
                        if (website.verifiedFacts.isNotBlank()) {
                            IntelDetailBlock("Verified Evidence vs AI Analysis", website.verifiedFacts)
                        }
                        if (website.notes.isNotBlank()) {
                            IntelDetailBlock("Personal Research Notes", website.notes)
                        }
                        if (website.tags.isNotEmpty()) {
                            IntelDetailBlock("Tags", website.tags.joinToString(", ") { "#$it" })
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // PRIMARY ACTIONS BAR (Visit, Save, Collection, Compare, Ask AI, Live Probe)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onVisit,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricBlue),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("visit_button_${website.id}")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Visit website",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Visit", style = MaterialTheme.typography.labelLarge)
                    }

                    IconButton(
                        onClick = onToggleSave,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("save_button_${website.id}")
                    ) {
                        Icon(
                            imageVector = if (website.savedByUser) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = if (website.savedByUser) "Unsave website" else "Save website",
                            tint = if (website.savedByUser) EmeraldFree else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onAddToCollection,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("collection_button_${website.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CreateNewFolder,
                            contentDescription = "Add to collection",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onToggleCompare,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("compare_button_${website.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CompareArrows,
                            contentDescription = "Compare website",
                            tint = if (isCompared) TacticalCyan else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onAskAi,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("ask_ai_button_${website.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QuestionAnswer,
                            contentDescription = "Ask AI about this website",
                            tint = VioletAi
                        )
                    }

                    IconButton(
                        onClick = onVerifyLive,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("verify_button_${website.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Http,
                            contentDescription = "Verify website reachability",
                            tint = verificationColor
                        )
                    }

                    if (onEditNotes != null) {
                        IconButton(
                            onClick = onEditNotes,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("edit_notes_button_${website.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.EditNote,
                                contentDescription = "Edit notes and tags",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("delete_button_${website.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Delete website",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SiteFavicon(
    faviconUrl: String,
    siteName: String,
    modifier: Modifier = Modifier
) {
    val initial = siteName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "W"
    SubcomposeAsyncImage(
        model = faviconUrl,
        contentDescription = "$siteName favicon",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)),
        loading = {
            Box(contentAlignment = Alignment.Center) {
                Text(initial, style = MaterialTheme.typography.titleMedium, color = ElectricBlue)
            }
        },
        error = {
            Box(contentAlignment = Alignment.Center) {
                Text(initial, style = MaterialTheme.typography.titleMedium, color = ElectricBlue)
            }
        }
    )
}

@Composable
private fun StatusBadgePill(
    text: String,
    color: Color,
    onClick: (() -> Unit)? = null
) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun IntelDetailBlock(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
