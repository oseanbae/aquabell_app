package com.capstone.aquabell.data.model

object ManualContent {

    // ===== Microcontroller =====
    val microcontroller = listOf(
        HardwareComponent(
            name = "ESP32 (30-pin)",
            summary = "Acts as the system’s main controller — reads sensors, executes automation logic, and communicates with the mobile app through Firebase.",
            purpose = """
                The ESP32 is the central brain of the AquaBell system. It reads all sensor data, executes the automation rules, and controls actuators based on defined thresholds. 
                It also communicates with the mobile app via Firebase for real-time monitoring and manual control. 
                Without the ESP32, the system wouldn’t be able to automate responses or synchronize data to the cloud.
            """.trimIndent(),
            imagePath = "drawable/esp32.png"
        )
    )

    // ===== Sensors =====
    val sensors = listOf(
        HardwareComponent(
            name = "DHT11",
            summary = "Monitors air temperature and humidity around the grow bed. Automatically triggers fans when air gets too hot or humid.",
            purpose = """
                Measures air temperature and humidity in the grow bed and surrounding environment. 
                The system uses this data to control DC fans for evaporative cooling and maintain a healthy climate for plants and fish. 
                Proper humidity prevents algae growth, while temperature stability ensures optimal photosynthesis.
                RULE: Fan ON if air temperature > ${SensorRanges.AIR_TEMP_CAUTION_MAX}°C.
            """.trimIndent(),
            imagePath = "drawable/sensor_dht11.png"
        ),
        HardwareComponent(
            name = "DS18B20",
            summary = "Tracks fish tank water temperature to control the heater and cooling fans, maintaining ideal thermal balance.",
            purpose = """
                Measures water temperature in the fish tank. 
                Water temperature stability is critical for fish like tilapia, which thrive between ${SensorRanges.WATER_TEMP_EXCELLENT_MIN}°C and ${SensorRanges.WATER_TEMP_EXCELLENT_MAX}°C. 
                When water gets too cold, the heater activates; when too hot, cooling fans engage.
            """.trimIndent(),
            imagePath = "drawable/sensor_water_temp.png"
        ),
        HardwareComponent(
            name = "DO Sensor",
            summary = "Monitors dissolved oxygen in the water to keep fish healthy. Triggers aeration when oxygen drops too low.",
            purpose = """
                Measures Dissolved Oxygen (DO) levels in the water. 
                When DO drops below ${SensorRanges.DO_ACCEPTABLE_MIN} mg/L, the air pump is activated to improve oxygen circulation and prevent fish stress. 
                High oxygen levels also promote healthy bacteria and water quality.
            """.trimIndent(),
            imagePath = "drawable/sensor_do.png"
        ),
        HardwareComponent(
            name = "pH Sensor",
            summary = "Checks acidity and alkalinity of the fish tank water. Alerts users if readings fall outside the safe range.",
            purpose = """
                Monitors the pH level of the water to ensure it stays within ${SensorRanges.PH_ACCEPTABLE_MIN}–${SensorRanges.PH_ACCEPTABLE_MAX}. 
                Stable pH supports nutrient absorption and prevents fish stress. 
                Alerts are triggered when readings reach caution or critical zones.
            """.trimIndent(),
            imagePath = "drawable/sensor_ph.png"
        ),
        HardwareComponent(
            name = "Float Switch",
            summary = "Detects low water level in the fish tank and triggers automatic refill via solenoid valve.",
            purpose = """
                Detects the water level in the fish tank. When it drops below the threshold, the solenoid valve opens to refill. 
                The system also pauses the pump during refill to prevent dry running. 
                Once normal level is restored, the valve closes automatically.
            """.trimIndent(),
            imagePath = "drawable/sensor_float_switch.png"
        )
    )

    // ===== Actuators =====
    val actuators = listOf(
        HardwareComponent(
            name = "DC Fans (Grow Bed & Fish Tank)",
            summary = "Provide air circulation and cooling. Triggered when air or water temperature exceeds caution levels.",
            purpose = """
                DC fans handle air movement and evaporative cooling. 
                Grow bed fans lower humidity to prevent algae and mold, while fish tank fans cool water when it exceeds ${SensorRanges.WATER_TEMP_CAUTION_MAX}°C. 
                RULE: Fans ON if air or water temperature > caution threshold.
            """.trimIndent(),
            imagePath = "drawable/actuator_fans.png"
        ),
        HardwareComponent(
            name = "Water Pump",
            summary = "Circulates water between fish tank and grow bed on an automated 15/45 schedule to sustain nutrient flow.",
            purpose = """
                The water pump drives nutrient exchange between the fish tank and grow bed. 
                Operates on a 15-minute ON / 45-minute OFF cycle for efficiency, ensuring plants receive nutrients without overwatering. 
                RULE: Timed operation every hour.
            """.trimIndent(),
            imagePath = "drawable/actuator_water_pump.png"
        ),
        HardwareComponent(
            name = "Air Pump",
            summary = "Provides constant aeration to maintain oxygen-rich water for fish and bacteria.",
            purpose = """
                Continuously runs to maintain dissolved oxygen levels above ${SensorRanges.DO_ACCEPTABLE_MIN} mg/L. 
                It supports fish respiration and beneficial bacteria for biofiltration. 
                RULE: Always ON for constant aeration.
            """.trimIndent(),
            imagePath = "drawable/actuator_air_pump.png"
        ),
        HardwareComponent(
            name = "Solenoid Valve",
            summary = "Automatically refills the fish tank when water drops below float switch level.",
            purpose = """
                Opens when float switch detects low water. Closes automatically once level returns to normal. 
                The water pump is disabled during refill to prevent dry run. 
                RULE: Valve ON if float switch = LOW.
            """.trimIndent(),
            imagePath = "drawable/actuator_valve.png"
        ),
        HardwareComponent(
            name = "Grow Lights",
            summary = "Provide controlled lighting for plant growth using a daily schedule.",
            purpose = """
                Simulates natural sunlight for the plants. 
                Operates from 5:30–11:00 AM and 3:00–9:30 PM to support photosynthesis. 
                RULE: Scheduled ON/OFF periods for consistent light exposure.
            """.trimIndent(),
            imagePath = "drawable/actuator_grow_light.png"
        ),
        HardwareComponent(
            name = "Water Heater",
            summary = "Heats the fish tank water when temperature drops below ${SensorRanges.WATER_TEMP_ACCEPTABLE_MIN}°C.",
            purpose = """
                Activates when water temperature is below optimal range to maintain fish comfort and growth. 
                RULE: Heater ON if water temp < ${SensorRanges.WATER_TEMP_ACCEPTABLE_MIN}°C; OFF when above ${SensorRanges.WATER_TEMP_EXCELLENT_MIN}°C.
            """.trimIndent(),
            imagePath = "drawable/actuator_water_heater.png"
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
