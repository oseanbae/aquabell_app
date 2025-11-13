package com.capstone.aquabell.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.capstone.aquabell.data.model.ManualContent
import com.capstone.aquabell.data.model.SensorRanges
import com.capstone.aquabell.data.model.SensorInterval

class DocsViewModel : ViewModel() {
	// Expose categories from ManualContent
	val microcontroller = ManualContent.microcontroller
	val sensors = ManualContent.sensors
	val actuators = ManualContent.actuators
	val powerComponents = ManualContent.powerComponents
	val display = ManualContent.display
	val others = ManualContent.others

	// === Data Classes ===
	data class ThresholdRow(
		val name: String,
		val readingInterval: String,
		val statusRanges: List<StatusRange>
	)

	data class StatusRange(
		val status: String, // "Excellent", "Good", "Caution", "Critical"
		val min: String,
		val max: String
	)

	// === Refactored Thresholds ===
	val thresholds: List<ThresholdRow> = listOf(
		// 1. pH
		ThresholdRow(
			name = "pH",
			readingInterval = "${SensorInterval.pH}s",
			statusRanges = listOf(
				StatusRange("Excellent", SensorRanges.PH_EXCELLENT_MIN.toString(), SensorRanges.PH_EXCELLENT_MAX.toString()),
				StatusRange("Good", SensorRanges.PH_ACCEPTABLE_MIN.toString(), SensorRanges.PH_ACCEPTABLE_MAX.toString()),
				StatusRange("Caution", SensorRanges.PH_CAUTION_MIN.toString(), SensorRanges.PH_CAUTION_MAX.toString()),
				StatusRange("Critical", "<${SensorRanges.PH_CAUTION_MIN}", ">${SensorRanges.PH_CAUTION_MAX}")
			)
		),

		// 2. Dissolved Oxygen (mg/L)
		ThresholdRow(
			name = "Dissolved Oxygen (mg/L)",
			readingInterval = "${SensorInterval.dissolvedOxygen}s",
			statusRanges = listOf(
				StatusRange("Excellent", SensorRanges.DO_EXCELLENT_MIN.toString(), "∞"),
				StatusRange("Good", SensorRanges.DO_ACCEPTABLE_MIN.toString(), "${SensorRanges.DO_EXCELLENT_MIN}"),
				StatusRange("Caution", SensorRanges.DO_CAUTION_MIN.toString(), "${SensorRanges.DO_ACCEPTABLE_MIN}"),
				StatusRange("Critical", "<${SensorRanges.DO_CAUTION_MIN}", "-")
			)
		),

		// 3. Water Temp (°C)
		ThresholdRow(
			name = "Water Temp (°C)",
			readingInterval = "${SensorInterval.waterTemp}s",
			statusRanges = listOf(
				StatusRange("Excellent", SensorRanges.WATER_TEMP_EXCELLENT_MIN.toString(), SensorRanges.WATER_TEMP_EXCELLENT_MAX.toString()),
				StatusRange("Good", SensorRanges.WATER_TEMP_ACCEPTABLE_MIN.toString(), SensorRanges.WATER_TEMP_ACCEPTABLE_MAX.toString()),
				StatusRange("Caution", SensorRanges.WATER_TEMP_CAUTION_MIN.toString(), SensorRanges.WATER_TEMP_CAUTION_MAX.toString()),
				StatusRange("Critical", "<${SensorRanges.WATER_TEMP_CAUTION_MIN}", ">${SensorRanges.WATER_TEMP_CAUTION_MAX}")
			)
		),

		// 4. Air Temp (°C)
		ThresholdRow(
			name = "Air Temp (°C)",
			readingInterval = "${SensorInterval.airTemp}s",
			statusRanges = listOf(
				StatusRange("Excellent", SensorRanges.AIR_TEMP_EXCELLENT_MIN.toString(), SensorRanges.AIR_TEMP_EXCELLENT_MAX.toString()),
				StatusRange("Good", SensorRanges.AIR_TEMP_ACCEPTABLE_MIN.toString(), SensorRanges.AIR_TEMP_ACCEPTABLE_MAX.toString()),
				StatusRange("Caution", SensorRanges.AIR_TEMP_CAUTION_MIN.toString(), SensorRanges.AIR_TEMP_CAUTION_MAX.toString()),
				StatusRange("Critical", "<${SensorRanges.AIR_TEMP_CAUTION_MIN}", ">${SensorRanges.AIR_TEMP_CAUTION_MAX}")
			)
		),

		// 5. Humidity (%)
		ThresholdRow(
			name = "Humidity (%)",
			readingInterval = "${SensorInterval.airHumidity}s",
			statusRanges = listOf(
				StatusRange("Excellent", SensorRanges.HUMIDITY_EXCELLENT_MIN.toString(), SensorRanges.HUMIDITY_EXCELLENT_MAX.toString()),
				StatusRange("Good", SensorRanges.HUMIDITY_ACCEPTABLE_MIN.toString(), SensorRanges.HUMIDITY_ACCEPTABLE_MAX.toString()),
				StatusRange("Caution", SensorRanges.HUMIDITY_CAUTION_MIN.toString(), SensorRanges.HUMIDITY_CAUTION_MAX.toString()),
				StatusRange("Critical", "<${SensorRanges.HUMIDITY_CAUTION_MIN}", ">${SensorRanges.HUMIDITY_CAUTION_MAX}")
			)
		),

		// 6. Turbidity (NTU)
		ThresholdRow(
			name = "Turbidity (NTU)",
			readingInterval = "${SensorInterval.turbidityNTU}s",
			statusRanges = listOf(
				StatusRange("Excellent", "0", SensorRanges.TURBIDITY_EXCELLENT_MAX.toString()),
				StatusRange("Good", "${SensorRanges.TURBIDITY_EXCELLENT_MAX + 0.1}", SensorRanges.TURBIDITY_ACCEPTABLE_MAX.toString()),
				StatusRange("Caution", "${SensorRanges.TURBIDITY_ACCEPTABLE_MAX + 0.1}", SensorRanges.TURBIDITY_CAUTION_MAX.toString()),
				StatusRange("Critical", ">${SensorRanges.TURBIDITY_CAUTION_MAX}", "-")
			)
		)
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


