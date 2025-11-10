package com.capstone.aquabell.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.capstone.aquabell.R
import com.capstone.aquabell.data.model.HardwareComponent
import com.capstone.aquabell.ui.viewmodel.DocsViewModel

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

		SensorThresholdTable(rows = vm.thresholds)

		AutomationRules(rules = vm.rules)

		Spacer(Modifier.height(8.dp))
	}
}

@Composable
private fun AboutSection() {
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

	OutlinedCard(
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
		shape = RoundedCornerShape(16.dp),
		border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(16.dp),
			verticalArrangement = Arrangement.spacedBy(8.dp)
		) {
			Text(
				text = "About Aquabell",
				style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
			)
			Text(
				text = text,
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurface
			)
		}
	}
}

@Composable
private fun CategorySection(title: String, components: List<HardwareComponent>) {
	Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
		Text(
			text = title,
			style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
		)
		ComponentsGrid(components)
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
	val outline = MaterialTheme.colorScheme.outline
	OutlinedCard(
		modifier = Modifier.fillMaxWidth(),
		shape = RoundedCornerShape(16.dp),
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
		border = androidx.compose.foundation.BorderStroke(1.dp, outline)
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(12.dp),
			horizontalArrangement = Arrangement.spacedBy(12.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			// Image container: fixed ratio, fit, neutral tint for transparency
			Box(
				modifier = Modifier
					.size(84.dp)
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
							.padding(12.dp)
							.fillMaxSize()
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
			Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
				Text(
					text = component.name,
					style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
				)
				Text(
					text = component.summary,
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
				)
				Divider(modifier = Modifier.padding(vertical = 4.dp), color = outline.copy(alpha = 0.4f))
				Text(
					text = component.purpose,
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
				)
			}
		}
	}
}

@Composable
private fun SensorThresholdTable(rows: List<com.capstone.aquabell.ui.viewmodel.DocsViewModel.ThresholdRow>) {
	val outline = MaterialTheme.colorScheme.outline
	OutlinedCard(
		shape = RoundedCornerShape(16.dp),
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
		border = androidx.compose.foundation.BorderStroke(1.dp, outline)
	) {
		Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
			Text(
				text = "Sensor Thresholds",
				style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
			)
			// Header
			Row(
				modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
				horizontalArrangement = Arrangement.SpaceBetween
			) {
				Text("Sensor", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1.2f))
				Text("Min", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End, modifier = Modifier.weight(0.8f))
				Text("Max", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End, modifier = Modifier.weight(0.8f))
			}
			Divider(color = outline.copy(alpha = 0.6f))
			rows.forEach { r ->
				Row(
					modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp),
					horizontalArrangement = Arrangement.SpaceBetween
				) {
					Text(r.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1.2f))
					Text(r.min, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End, modifier = Modifier.weight(0.8f))
					Text(r.max, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End, modifier = Modifier.weight(0.8f))
				}
				Divider(color = outline.copy(alpha = 0.15f))
			}
		}
	}
}

@Composable
private fun AutomationRules(rules: List<com.capstone.aquabell.ui.viewmodel.DocsViewModel.Rule>) {
	val outline = MaterialTheme.colorScheme.outline
	OutlinedCard(
		shape = RoundedCornerShape(16.dp),
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
		border = androidx.compose.foundation.BorderStroke(1.dp, outline)
	) {
		Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
			Text(
				text = "Automation Rules",
				style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
			)
			rules.forEach { rule ->
				RuleRow(condition = rule.condition, action = rule.action)
				Divider(color = outline.copy(alpha = 0.15f))
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
		"esp32.png" -> R.drawable.ic_analytics // placeholder fallback
		"sensor_dht11.png" -> R.drawable.ic_humidity
		"sensor_water_temp.png" -> R.drawable.ic_water_temp
		"sensor_do.png" -> R.drawable.ic_oxygen
		"sensor_ph.png" -> R.drawable.ic_ph
		"sensor_float_switch.png" -> R.drawable.ic_float_switch
		"actuator_fans.png" -> R.drawable.ic_fans
		"actuator_water_pump.png" -> R.drawable.ic_pump
		"actuator_air_pump.png" -> R.drawable.ic_oxygen
		"actuator_valve.png" -> R.drawable.ic_valve
		"actuator_grow_light.png" -> R.drawable.ic_light
		"actuator_water_heater.png" -> R.drawable.ic_water_heater
		"lcd_display.png" -> R.drawable.ic_home
		"power_ups.png" -> R.drawable.ic_bell
		"power_solar.png" -> R.drawable.ic_home
		"power_sps.png" -> R.drawable.ic_home
		"power_buck.png" -> R.drawable.ic_home
		"relay_module.png" -> R.drawable.ic_home
		"pocket_wifi.png" -> R.drawable.ic_home
		else -> null
	}
}


