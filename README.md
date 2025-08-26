# relay-lap-counter
Android app for tracking laps in a one-hour relay run. Multiple teams can record laps by tapping buttons with their start numbers.

## Features

- Dynamically created buttons fill the screen so each team has a large rectangular target.
- Lap times are recorded per start number and additional distance not completing a full lap can be entered afterwards.
- Total distance is calculated as `laps x 400m + extra meters` and results can be sorted to show the best team on top.
- Tapping a result reveals a list of lap times for that start number.
- Recorded data (start number, lap times and remaining meters) can be exported via the share menu.
