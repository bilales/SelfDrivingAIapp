# Depth Estimation and Object Detection App for SelfDriving purposes

[Example](markodown_selfdrivingAI.mp4)


This project is an Android application for depth estimation and object detection using TensorFlow Lite models. The app displays bounding boxes around detected objects and provides estimated depth for each object.
As this app is designed 

## Features
- Depth estimation using a pre-trained MiDaS model.
- Object detection with bounding boxes and class-based coloring.
- Real-time inference using TensorFlow Lite.

### Project info:

```
compileSdk 30
applicationId "com.bilalazdad.ml.SelfDrivingAI"
minSdk 23
targetSdk 30
androidGradlePluginVersion 7.0.0
gradlePluginVersion 7.0.2
```

## Installation
1. Clone the repository:
    ```bash
    git clone https://github.com/bilales/SelfDrivingAIapp
    ```
2. Open the project in Android Studio.
3. Sync the Gradle files and run the application on a device or emulator.

## Credits

This project is based on the original work by **[Subham Panchal]** under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0). The original project can be found [here](https://github.com/shubham0204/Realtime_MiDaS_Depth_Estimation_Android).

## License

```
Copyright 2024 Bilal Azdad
Licensed under the Apache License, Version 2.0 (the "License");
You may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
