package com.capstone.aquabell.data.model

object ManualContent {

    // ===== Microcontroller =====
    val microcontroller = listOf(
        HardwareComponent(
            name = "ESP32 (30-pin)",
            summary = "Main controller — reads sensors, runs automation rules, and communicates with the mobile app.",
            purpose = """
                Central brain of the AquaBell system. Reads all sensor data, executes automation rules, and controls actuators. 
                Syncs with the mobile app via Firebase for real-time monitoring and manual override.
            """.trimIndent(),
            imagePath = "drawable/esp32.png"
        )
    )

    // ===== Sensors =====
    val sensors = listOf(
        HardwareComponent(
            name = "DHT11",
            summary = "Monitors air temperature and humidity in the grow bed.",
            purpose = """
                Measures air temperature and humidity to maintain a healthy environment for plants and fish. 
                Grow bed fans are triggered if air temperature exceeds ${SensorRanges.AIR_TEMP_CAUTION_MAX}°C. 
                Proper humidity and temperature prevent mold, algae, and plant stress.
            """.trimIndent(),
            imagePath = "drawable/sensor_dht11.png"
        ),
        HardwareComponent(
            name = "DS18B20",
            summary = "Tracks fish tank water temperature.",
            purpose = """
                Monitors water temperature to maintain optimal conditions for fish. 
                Water heater activates below ${SensorRanges.WATER_TEMP_ACCEPTABLE_MIN}°C, and fans engage above ${SensorRanges.WATER_TEMP_CAUTION_MAX}°C. 
                Stable temperatures prevent stress and promote healthy growth.
            """.trimIndent(),
            imagePath = "drawable/sensor_water_temp.png"
        ),
        HardwareComponent(
            name = "Dissolved Oxygen Sensor",
            summary = "Monitors oxygen levels in the tank.",
            purpose = """
                Measures dissolved oxygen to ensure fish and beneficial bacteria are healthy. 
                Air pump activates if DO falls below ${SensorRanges.DO_ACCEPTABLE_MIN} mg/L. 
                Maintaining oxygen levels prevents fish stress and supports biofiltration.
            """.trimIndent(),
            imagePath = "drawable/sensor_do.png"
        ),
        HardwareComponent(
            name = "pH Sensor",
            summary = "Checks acidity and alkalinity of tank water.",
            purpose = """
                Monitors pH to keep water within ${SensorRanges.PH_ACCEPTABLE_MIN}–${SensorRanges.PH_ACCEPTABLE_MAX}. 
                When pH goes outside safe limits, the system triggers the pH dosing pump to correct it. 
                Stable pH ensures nutrient absorption and fish comfort.
            """.trimIndent(),
            imagePath = "drawable/sensor_ph.png"
        ),
        HardwareComponent(
            name = "Turbidity Sensor",
            summary = "Monitors water clarity after filtration.",
            purpose = """
                Measures cloudiness to detect filtration issues or dirty water. 
                Alerts the user when turbidity exceeds safe levels. 
                This ensures water remains clear and healthy for fish and plants.
            """.trimIndent(),
            imagePath = "drawable/sensor_turbidity.png"
        ),
        HardwareComponent(
            name = "Float Switch",
            summary = "Detects low water levels in the fish tank.",
            purpose = """
                Triggers the solenoid valve to refill the tank when water drops too low. 
                Pauses the water pump during refill to prevent dry running. 
                Closes the valve automatically once normal water level is restored.
            """.trimIndent(),
            imagePath = "drawable/sensor_float_switch.png"
        )
    )

    // ===== Actuators =====
    val actuators = listOf(
        HardwareComponent(
            name = "DC Fans (Grow Bed & Fish Tank)",
            summary = "Provide air circulation and cooling.",
            purpose = """
                Grow bed fans lower humidity while fish tank fans cool water above ${SensorRanges.WATER_TEMP_CAUTION_MAX}°C. 
                They activate automatically based on temperature thresholds. 
                Fans help maintain a stable environment for both fish and plants.
            """.trimIndent(),
            imagePath = "drawable/actuator_fans.png"
        ),
        HardwareComponent(
            name = "Water Pump",
            summary = "Circulates water between fish tank and grow bed on a schedule.",
            purpose = """
                Operates on a 15-minute ON / 45-minute OFF cycle to supply nutrients without overwatering. 
                Ensures efficient nutrient flow between fish tank and plants. 
                Automatic scheduling reduces the need for manual intervention.
            """.trimIndent(),
            imagePath = "drawable/actuator_water_pump.png"
        ),
        HardwareComponent(
            name = "Air Pump",
            summary = "Maintains oxygen levels in water.",
            purpose = """
                Activates when DO drops below ${SensorRanges.DO_ACCEPTABLE_MIN} mg/L. 
                Provides consistent aeration to support fish respiration and biofiltration. 
                Keeps the water oxygen-rich for healthy tank conditions.
            """.trimIndent(),
            imagePath = "drawable/actuator_air_pump.png"
        ),
        HardwareComponent(
            name = "Solenoid Valve",
            summary = "Refills fish tank automatically when water is low.",
            purpose = """
                Opens when the float switch detects low water and closes once normal level is restored. 
                Pauses the water pump during refill to prevent dry running. 
                Ensures water supply is maintained without manual monitoring.
            """.trimIndent(),
            imagePath = "drawable/actuator_valve.png"
        ),
        HardwareComponent(
            name = "Grow Lights",
            summary = "Provide scheduled lighting for plant growth.",
            purpose = """
                Simulates sunlight on a 5:30–11:00 AM and 3:00–9:30 PM schedule. 
                Supports photosynthesis and consistent plant growth. 
                Automatically manages lighting without manual control.
            """.trimIndent(),
            imagePath = "drawable/actuator_grow_light.png"
        ),
        HardwareComponent(
            name = "Water Heater",
            summary = "Heats fish tank water when temperature is low.",
            purpose = """
                Activates if water temperature falls below ${SensorRanges.WATER_TEMP_ACCEPTABLE_MIN}°C and deactivates above ${SensorRanges.WATER_TEMP_EXCELLENT_MIN}°C. 
                Maintains optimal thermal conditions for fish health. 
                Helps prevent stress caused by cold water.
            """.trimIndent(),
            imagePath = "drawable/actuator_water_heater.png"
        ),
        HardwareComponent(
            name = "pH Dosing Pump",
            summary = "Adjusts water pH automatically.",
            purpose = """
                Dispenses acid or base in pulses when pH exceeds safe range. 
                Maintains stable pH to prevent stress on fish and plants. 
                Operates gradually to avoid sudden chemical changes.
            """.trimIndent(),
            imagePath = "drawable/actuator_ph_pump.png"
        )
    )

    // ===== Display =====
    val display = listOf(
        HardwareComponent(
            name = "20x4 LCD Display",
            summary = "Shows live sensor readings and actuator status without opening the app.",
            purpose = """
                Displays real-time system data like temperature, pH, DO, humidity, and actuator states locally. 
                Serves as an immediate diagnostic interface and a backup when internet connectivity is unavailable.
            """.trimIndent(),
            imagePath = "drawable/lcd_display.png"
        )
    )

    // ===== Power Components =====
    val powerComponents = listOf(
        HardwareComponent(
            name = "UPS (Uninterruptible Power Supply)",
            summary = "Keeps system running during power outages by supplying temporary backup power.",
            purpose = """
                Stores energy from the solar panel or mains and delivers it during outages. 
                Ensures continuous operation of essential components like pumps, fans, and sensors.
            """.trimIndent(),
            imagePath = "drawable/power_ups.png"
        ),
        HardwareComponent(
            name = "Solar Panel",
            summary = "Provides renewable power to charge the system battery and reduce grid dependence.",
            purpose = """
                Converts sunlight into DC electricity to power the AquaBell system. 
                Helps sustain off-grid operation and keeps the battery charged for nighttime use.
            """.trimIndent(),
            imagePath = "drawable/power_solar.png"
        ),
        HardwareComponent(
            name = "Switching Power Supply",
            summary = "Converts AC mains power to regulated DC voltage for ESP32 and actuators.",
            purpose = """
                Provides stable voltage and prevents power fluctuations that could damage sensitive components.
            """.trimIndent(),
            imagePath = "drawable/power_sps.png"
        ),
        HardwareComponent(
            name = " 5V Buck Converter",
            summary = "Steps down voltage for low-power components like sensors and LCD display.",
            purpose = """
                Regulates voltage levels to prevent overvoltage damage and ensures stable operation of 3.3V and 5V peripherals.
            """.trimIndent(),
            imagePath = "drawable/power_buck.png"
        )
    )

    // ===== Relays & Connectivity =====
    val others = listOf(
        HardwareComponent(
            name = "8-Channel Relay Module",
            summary = "Switches high-current actuators safely under control of the ESP32.",
            purpose = """
                Electrically isolates the ESP32 from high-voltage loads. 
                Each actuator is connected through a relay channel that toggles ON/OFF based on rule-based triggers.
            """.trimIndent(),
            imagePath = "drawable/relay_module.png"
        ),
        HardwareComponent(
            name = "Pocket Wi-Fi",
            summary = "Provides internet connectivity for Firebase and mobile app communication.",
            purpose = """
                Ensures real-time data sync, cloud logging, and remote monitoring. 
                Without it, app communication and alert notifications are unavailable.
            """.trimIndent(),
            imagePath = "drawable/pocket_wifi.png"
        )
    )
}

// ===== Data Model =====
data class HardwareComponent(
    val name: String,
    val summary: String,
    val purpose: String,
    val imagePath: String? = null
)
