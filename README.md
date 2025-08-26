# AquaBell Smart Aquaponic System

A modern Android application for monitoring and controlling aquaponic systems.

## Features

### Dashboard
- **Air Temperature**: Real-time air temperature monitoring with thermometer icon
- **Air Humidity**: Relative humidity monitoring with dual droplet icon
- **Water Temperature**: Water temperature monitoring with thermometer and water drop icon
- **pH Level**: pH monitoring with droplet-shaped pH indicator
- **Dissolved Oxygen**: Oxygen levels with cloud and O2 symbol
- **Turbidity Level**: Water clarity monitoring with particle indicators

### Control Panel
- **Light Control**: Stylized sun icon for lighting control
- **Fan Control**: Airflow indicators for ventilation control
- **Pump Control**: Water drop outline for pump control
- **Valve Control**: Stylized valve design for flow control
- **Auto Mode**: Toggle for automatic system control

### Navigation
- **Analytics**: Bar chart icon for data analysis
- **Home**: House icon for main dashboard
- **Alerts**: Bell icon for notifications

## Design Improvements

### Icons
- Custom vector icons for all dashboard metrics
- Modern, clean design matching the app's aesthetic
- Consistent black outline style throughout
- Proper sizing and spacing for optimal visibility

### Layout
- **Scrollable Interface**: Added vertical scrolling for compatibility with all screen sizes
- **Improved Card Heights**: Increased metric card height to accommodate longer text (like "Dissolved Oxygen")
- **Better Text Handling**: Added text overflow handling with ellipsis for long labels
- **Responsive Design**: Cards adapt to different screen sizes while maintaining readability

### User Experience
- **Visual Feedback**: Active control states are clearly indicated
- **Consistent Spacing**: Improved spacing between sections
- **Modern Aesthetics**: Clean, minimalist design with rounded corners and subtle shadows

## Technical Details

- Built with Jetpack Compose
- Material Design 3 components
- Custom vector drawables for all icons
- Responsive grid layout
- State management for control toggles

## File Structure

```
app/src/main/
├── java/com/capstone/aquabell/
│   └── ui/
│       └── HomeScreen.kt          # Main UI implementation
└── res/drawable/
    ├── ic_thermometer.xml         # Air temperature icon
    ├── ic_water_temp.xml          # Water temperature icon
    ├── ic_humidity.xml            # Humidity icon
    ├── ic_ph.xml                  # pH level icon
    ├── ic_oxygen.xml              # Dissolved oxygen icon
    ├── ic_turbidity.xml           # Turbidity icon
    ├── ic_light.xml               # Light control icon
    ├── ic_fans.xml                # Fan control icon
    ├── ic_pump.xml                # Pump control icon
    ├── ic_valve.xml               # Valve control icon
    ├── ic_analytics.xml           # Analytics navigation icon
    ├── ic_home.xml                # Home navigation icon
    └── ic_bell.xml                # Alerts navigation icon
```

