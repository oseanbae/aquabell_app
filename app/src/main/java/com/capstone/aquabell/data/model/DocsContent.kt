package com.capstone.aquabell.data.model

// Alias wrapper to satisfy requested filename while reusing ManualContent
object DocsContent {
	val microcontroller get() = ManualContent.microcontroller
	val sensors get() = ManualContent.sensors
	val actuators get() = ManualContent.actuators
	val powerComponents get() = ManualContent.powerComponents
	val display get() = ManualContent.display
	val others get() = ManualContent.others
}


