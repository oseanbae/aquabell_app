package com.capstone.aquabell.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.viewmodel.compose.viewModel
import com.capstone.aquabell.R
import com.capstone.aquabell.data.model.HardwareComponent
import com.capstone.aquabell.ui.viewmodel.DocsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun DocsScreen(modifier: Modifier = Modifier) {
	val vm: DocsViewModel = viewModel()

	Column(
		modifier = modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.surface)
			.verticalScroll(rememberScrollState())
			.padding(horizontal = 16.dp, vertical = 12.dp),
		verticalArrangement = Arrangement.spacedBy(16.dp)
	) {
		Text(
			text = "Documentation",
			style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
		)

		AboutSection()

		// Components sections
		CategorySection(title = "Microcontroller", components = vm.microcontroller)
		CategorySection(title = "Sensors", components = vm.sensors)
		CategorySection(title = "Actuators", components = vm.actuators)
		CategorySection(title = "Power Components", components = vm.powerComponents)
		CategorySection(title = "Display & Interface", components = vm.display)
		CategorySection(title = "Others", components = vm.others)

		// Additional sections")
		SensorThresholdTable(rows = vm.thresholds)

		AutomationRules(rules = vm.rules)

		Spacer(Modifier.height(8.dp))
	}
}

@Composable
fun rememberPersistentExpandedState(context: Context, key: String): Pair<Boolean, (Boolean) -> Unit> {
	val scope = rememberCoroutineScope()
	val expandedFlow = remember { context.getExpandedStateFlow(key) }
	val expanded by expandedFlow.collectAsState(initial = false)

	val onExpandedChange: (Boolean) -> Unit = { newValue ->
		scope.launch {
			context.saveExpandedState(key, newValue)
		}
	}

	return expanded to onExpandedChange
}

val Context.docsPrefs by preferencesDataStore("docs_state")

suspend fun Context.saveExpandedState(key: String, expanded: Boolean) {
	docsPrefs.edit { prefs ->
		prefs[booleanPreferencesKey(key)] = expanded
	}
}

fun Context.getExpandedStateFlow(key: String): Flow<Boolean> =
	docsPrefs.data.map { prefs -> prefs[booleanPreferencesKey(key)] ?: false }

@Composable
private fun AboutSection() {
	val context = LocalContext.current
	val (expanded, setExpanded) = rememberPersistentExpandedState(context, "about_section")

	val accent = MaterialTheme.colorScheme.primary
	val text = buildAnnotatedString {
		withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
			append("AquaBell ")
		}
		append("is a smart aquaponics monitoring and automation system. It provides ")
		withStyle(SpanStyle(color = accent, fontWeight = FontWeight.SemiBold)) {
			append("real-time monitoring")
		}
		append(" of environmental conditions, ")
		withStyle(SpanStyle(color = accent, fontWeight = FontWeight.SemiBold)) {
			append("rule-based automation")
		}
		append(" for actuators, and user control in ")
		withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
			append("AUTO")
		}
		append(" or ")
		withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
			append("MANUAL")
		}
		append(" modes directly from the app.")
	}
	
	val rotationAngle by animateFloatAsState(
		targetValue = if (expanded) 180f else 0f,
		animationSpec = tween(durationMillis = 300),
		label = "about_chevron_rotation"
	)

	OutlinedCard(
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
		shape = RoundedCornerShape(16.dp),
		border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(16.dp),
			verticalArrangement = Arrangement.spacedBy(8.dp)
		) {
			// Accordion header
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.clickable { setExpanded(!expanded) }
					.padding(vertical = 4.dp),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically
			) {
				Text(
					text = "About AquaBell",
					style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
				)
				Icon(
					imageVector = Icons.Default.ExpandMore,
					contentDescription = if (expanded) "Collapse" else "Expand",
					modifier = Modifier.rotate(rotationAngle),
					tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
				)
			}
			
			// Animated content
			AnimatedVisibility(
				visible = expanded,
				enter = expandVertically(animationSpec = tween(300)),
				exit = shrinkVertically(animationSpec = tween(300))
			) {
				Text(
					text = text,
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurface
				)
			}
		}
	}
}

@Composable
private fun CategorySection(title: String, components: List<HardwareComponent>) {
	val context = LocalContext.current
	val key = "category_$title"
	val (expanded, setExpanded) = rememberPersistentExpandedState(context, key)
	
	val rotationAngle by animateFloatAsState(
		targetValue = if (expanded) 180f else 0f,
		animationSpec = tween(durationMillis = 300),
		label = "chevron_rotation"
	)
	
	Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
		// Accordion header
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.clickable { setExpanded(!expanded) }
				.padding(vertical = 4.dp),
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically
		) {
			Text(
				text = title,
				style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
			)
			Icon(
				imageVector = Icons.Default.ExpandMore,
				contentDescription = if (expanded) "Collapse" else "Expand",
				modifier = Modifier.rotate(rotationAngle),
				tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
			)
		}
		
		// Animated content
		AnimatedVisibility(
			visible = expanded,
			enter = expandVertically(animationSpec = tween(300)),
			exit = shrinkVertically(animationSpec = tween(300))
		) {
			ComponentsGrid(components)
		}
		
		// Section divider
		HorizontalDivider(
			modifier = Modifier.padding(vertical = 4.dp),
			color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
		)
	}
}

@Composable
private fun ComponentsGrid(components: List<HardwareComponent>) {
	// Use a simple vertical list of cards that align well across sizes
	Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
		components.forEach { comp ->
			ComponentCard(comp)
		}
	}
}

@Composable
private fun ComponentCard(component: HardwareComponent) {
	val context = LocalContext.current
	val key = "component_${component.name}"
	val (expanded, setExpanded) = rememberPersistentExpandedState(context, key)
	val outline = MaterialTheme.colorScheme.outline
	
	val rotationAngle by animateFloatAsState(
		targetValue = if (expanded) 180f else 0f,
		animationSpec = tween(durationMillis = 300),
		label = "card_chevron_rotation"
	)

	OutlinedCard(
		modifier = Modifier
			.fillMaxWidth()
			.padding(vertical = 6.dp),
		shape = RoundedCornerShape(16.dp),
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
		border = BorderStroke(1.dp, outline)
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.clickable { setExpanded(!expanded) }
				.padding(16.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp)
		) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(16.dp),
				verticalAlignment = Alignment.CenterVertically
			) {
				// Image container
				Box(
					modifier = Modifier
						.size(120.dp)
						.aspectRatio(1f)
						.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
					contentAlignment = Alignment.Center
				) {
					val resId = resolveDrawable(component.imagePath)
					if (resId != null) {
						Image(
							painter = painterResource(id = resId),
							contentDescription = component.name,
							contentScale = ContentScale.Fit,
							modifier = Modifier
								.fillMaxSize()
								.padding(10.dp)
						)
					} else {
						Text(
							text = "No Image",
							style = MaterialTheme.typography.labelSmall,
							color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
							textAlign = TextAlign.Center
						)
					}
				}
				
				// Name and summary column
				Column(
					modifier = Modifier.weight(1f),
					verticalArrangement = Arrangement.spacedBy(6.dp)
				) {
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.SpaceBetween,
						verticalAlignment = Alignment.CenterVertically
					) {
						Text(
							text = component.name,
							style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
							color = MaterialTheme.colorScheme.onSurface,
							modifier = Modifier.weight(1f)
						)
						
						// Chevron icon
						Icon(
							imageVector = Icons.Default.ExpandMore,
							contentDescription = if (expanded) "Collapse" else "Expand",
							modifier = Modifier
								.size(20.dp)
								.rotate(rotationAngle),
							tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
						)
					}
					
					Text(
						text = component.summary,
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
					)
				}
			}
			
			// Expandable purpose section
			AnimatedVisibility(
				visible = expanded,
				enter = expandVertically(animationSpec = tween(300)),
				exit = shrinkVertically(animationSpec = tween(300))
			) {
				Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
					HorizontalDivider(
						modifier = Modifier.padding(vertical = 4.dp),
						color = outline.copy(alpha = 0.3f)
					)
					
					Text(
						text = component.purpose,
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
					)
				}
			}
		}
	}
}


@Composable
private fun SensorThresholdTable(rows: List<DocsViewModel.ThresholdRow>) {
	val context = LocalContext.current
	val key = "sensor_threshold_table"
	val (expanded, setExpanded) = rememberPersistentExpandedState(context, key)

	val outline = MaterialTheme.colorScheme.outline
	
	val rotationAngle by animateFloatAsState(
		targetValue = if (expanded) 180f else 0f,
		animationSpec = tween(durationMillis = 300),
		label = "threshold_chevron_rotation"
	)
	
	OutlinedCard(
		shape = RoundedCornerShape(16.dp),
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
		border = BorderStroke(1.dp, outline)
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(12.dp),
			verticalArrangement = Arrangement.spacedBy(8.dp)
		) {
			// Accordion header
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.clickable { setExpanded(!expanded) }
					.padding(vertical = 4.dp),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically
			) {
				Text(
					text = "Sensor Thresholds",
					style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
				)
				Icon(
					imageVector = Icons.Default.ExpandMore,
					contentDescription = if (expanded) "Collapse" else "Expand",
					modifier = Modifier.rotate(rotationAngle),
					tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
				)
			}
			
			// Animated content
			AnimatedVisibility(
				visible = expanded,
				enter = expandVertically(animationSpec = tween(300)),
				exit = shrinkVertically(animationSpec = tween(300))
			) {
				Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
					// Header
					Row(
						modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
						horizontalArrangement = Arrangement.SpaceBetween
					) {
						Text("Sensor", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1.2f))
						Text("Min", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End, modifier = Modifier.weight(0.8f))
						Text("Max", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End, modifier = Modifier.weight(0.8f))
					}
					HorizontalDivider(color = outline.copy(alpha = 0.6f))
					rows.forEach { r ->
						Row(
							modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp),
							horizontalArrangement = Arrangement.SpaceBetween
						) {
							Text(r.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1.2f))
							Text(r.min, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End, modifier = Modifier.weight(0.8f))
							Text(r.max, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End, modifier = Modifier.weight(0.8f))
						}
						HorizontalDivider(color = outline.copy(alpha = 0.15f))
					}
				}
			}
		}
	}
}

@Composable
private fun AutomationRules(rules: List<DocsViewModel.Rule>) {
	val context = LocalContext.current
	val key = "automation_rules"
	val (expanded, setExpanded) = rememberPersistentExpandedState(context, key)
	val outline = MaterialTheme.colorScheme.outline
	
	val rotationAngle by animateFloatAsState(
		targetValue = if (expanded) 180f else 0f,
		animationSpec = tween(durationMillis = 300),
		label = "rules_chevron_rotation"
	)
	
	OutlinedCard(
		shape = RoundedCornerShape(16.dp),
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
		border = BorderStroke(1.dp, outline)
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(12.dp),
			verticalArrangement = Arrangement.spacedBy(8.dp)
		) {
			// Accordion header
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.clickable { setExpanded(!expanded) }
					.padding(vertical = 4.dp),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically
			) {
				Text(
					text = "Automation Rules",
					style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
				)
				Icon(
					imageVector = Icons.Default.ExpandMore,
					contentDescription = if (expanded) "Collapse" else "Expand",
					modifier = Modifier.rotate(rotationAngle),
					tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
				)
			}
			
			// Animated content
			AnimatedVisibility(
				visible = expanded,
				enter = expandVertically(animationSpec = tween(300)),
				exit = shrinkVertically(animationSpec = tween(300))
			) {
				Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
					rules.forEachIndexed { index, rule ->
						RuleRow(condition = rule.condition, action = rule.action)
						// Only add divider if not the last item
						if (index < rules.size - 1) {
							HorizontalDivider(color = outline.copy(alpha = 0.15f))
						}
					}
				}
			}
		}
	}
}

@Composable
private fun RuleRow(condition: String, action: String) {
	val accent by animateColorAsState(targetValue = MaterialTheme.colorScheme.primary, label = "accent_anim")
	Row(
		modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
		horizontalArrangement = Arrangement.spacedBy(8.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Box(
			modifier = Modifier
				.size(8.dp)
				.background(accent, RoundedCornerShape(999.dp))
		)
		Text(
			text = "If $condition → $action",
			style = MaterialTheme.typography.bodySmall,
			color = MaterialTheme.colorScheme.onSurface
		)
	}
}

private fun resolveDrawable(path: String?): Int? {
	if (path.isNullOrBlank()) return null
	return when (path.substringAfterLast('/')) {
		"esp32.png" -> R.drawable.esp32 // placeholder fallback
		"sensor_dht11.png" -> R.drawable.sensor_dht11
		"sensor_water_temp.png" -> R.drawable.sensor_water_temp
		"sensor_do.png" -> R.drawable.sensor_do
		"sensor_ph.png" -> R.drawable.sensor_ph
		"sensor_float_switch.png" -> R.drawable.sensor_float_switch
		"sensor_turbidity.png" -> R.drawable.sensor_turbidity
		"actuator_fans.png" -> R.drawable.actuator_fans
		"actuator_water_pump.png" -> R.drawable.actuator_water_pump
		"actuator_air_pump.png" -> R.drawable.actuator_air_pump
		"actuator_valve.png" -> R.drawable.actuator_valve
		"actuator_grow_light.png" -> R.drawable.actuator_grow_light
		"actuator_water_heater.png" -> R.drawable.actuator_water_heater
		"lcd_display.png" -> R.drawable.lcd_display
		"power_ups.png" -> R.drawable.power_ups
		"power_solar.png" -> R.drawable.power_solar
		"power_sps.png" -> R.drawable.power_sps
		"power_buck.png" -> R.drawable.power_buck
		"relay_module.png" -> R.drawable.relay_module
		"pocket_wifi.png" -> R.drawable.pocket_wifi
		else -> null
	}
}


