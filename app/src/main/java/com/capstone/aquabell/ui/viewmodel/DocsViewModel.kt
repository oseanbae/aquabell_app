package com.capstone.aquabell.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.capstone.aquabell.data.model.ManualContent
import com.capstone.aquabell.data.model.SensorRanges

class DocsViewModel : ViewModel() {
	// Expose categories from ManualContent
	val microcontroller = ManualContent.microcontroller
	val sensors = ManualContent.sensors
	val actuators = ManualContent.actuators
	val powerComponents = ManualContent.powerComponents
	val display = ManualContent.display
	val others = ManualContent.others

	// Expose sensor thresholds for table rendering
	data class ThresholdRow(
		val name: String,
		val min: String,
		val max: String
	)

	val thresholds: List<ThresholdRow> = listOf(
		ThresholdRow(
			name = "pH",
			min = SensorRanges.PH_CAUTION_MIN.toString(),
			max = SensorRanges.PH_CAUTION_MAX.toString()
		),
		ThresholdRow(
			name = "Dissolved Oxygen (mg/L)",
			min = SensorRanges.DO_CAUTION_MIN.toString(),
			max = "∞"
		),
		ThresholdRow(
			name = "Water Temp (°C)",
			min = SensorRanges.WATER_TEMP_CAUTION_MIN.toString(),
			max = SensorRanges.WATER_TEMP_CAUTION_MAX.toString()
		),
		ThresholdRow(
			name = "Air Temp (°C)",
			min = SensorRanges.AIR_TEMP_CAUTION_MIN.toString(),
			max = SensorRanges.AIR_TEMP_CAUTION_MAX.toString()
		),
		ThresholdRow(
			name = "Humidity (%)",
			min = SensorRanges.HUMIDITY_CAUTION_MIN.toString(),
			max = SensorRanges.HUMIDITY_CAUTION_MAX.toString()
		),
		ThresholdRow(
			name = "Turbidity (NTU)",
			min = "0",
			max = SensorRanges.TURBIDITY_CAUTION_MAX.toString()
		),
	)

	data class Rule(val condition: String, val action: String)

	val rules: List<Rule> = listOf(
		Rule("Water temperature > ${SensorRanges.WATER_TEMP_CAUTION_MAX}°C", "Fish Tank Fan ON"),
		Rule("Water temperature < ${SensorRanges.WATER_TEMP_ACCEPTABLE_MIN}°C", "Water Heater ON"),
		Rule("Air temperature > ${SensorRanges.AIR_TEMP_CAUTION_MAX}°C", "Grow Bed Fans ON"),
		Rule("DO < ${SensorRanges.DO_ACCEPTABLE_MIN} mg/L", "Air Pump ON"),
		Rule("Float switch LOW", "Solenoid Valve OPEN, Water Pump OFF"),
		Rule("Pump schedule", "15 min ON / 45 min OFF each hour"),
		Rule("Grow lights", "ON 5:30–11:00 and 15:00–21:30"),
	)
}


