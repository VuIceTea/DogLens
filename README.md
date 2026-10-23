<div align="center">

# 🐾 DogLens

### Discover dog breeds through images

**An Android application concept for identifying dog breeds, exploring breed information, and saving favorites.**

![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)
![Language](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)
![UI](https://img.shields.io/badge/UI-XML%20Layouts-1688F0)
![Status](https://img.shields.io/badge/Status-In%20Development-F5A623)

</div>

## 📖 Overview

**DogLens** is a native Android app project designed to make discovering dog breeds simple and approachable. Users will be able to select or capture a photo, view breed information, browse popular breeds, and collect their favorites.

The project is **currently in development**. Its interface is being built with Kotlin and Android XML layouts; dog-breed recognition and live data integration are planned features, not yet claimed as working functionality.

## ✨ Features

### UI and navigation
- 🏠 **Home:** DogLens banner, image-upload area, feature shortcuts, and popular-breed previews.
- 🔎 **Explore:** Entry point for discovering breeds.
- ❤️ **Favorites:** Entry point for saved breeds.
- 👤 **Profile:** Entry point for personal settings and account-related screens.
- 📱 **Bottom navigation:** Switch between the main sections using Android Fragments.

### Planned functionality
- 📷 Capture a dog photo or select one from the gallery.
- 🤖 Identify a dog's breed from an image using a recognition service/model.
- 📚 Retrieve breed details and images from an API.
- 💾 Save and manage favorite breeds.
- 🕘 Review identification history.

> **Note:** These planned capabilities are a roadmap, not a statement that the backend or image-recognition functionality has already been implemented.

## 🛠️ Tech stack

| Area | Technology |
| --- | --- |
| Language | Kotlin |
| IDE | Android Studio |
| Interface | Android XML layouts |
| UI components | Fragments, RecyclerView, Material Components |
| Layouts | ConstraintLayout, LinearLayout, FrameLayout, ScrollView |
| Version control | Git & GitHub |
| Future integration | REST API for breed data and image recognition |

## 📱 Screens

| Screen | Purpose |
| --- | --- |
| Home | Main banner, upload UI, shortcuts, popular breeds |
| Explore | Browse and search breeds (under development) |
| Favorites | View saved breeds (under development) |
| Profile | User information and settings (under development) |

### Screenshots

Add screenshots captured from the Android emulator under `screenshots/` and then uncomment the examples below:

<!--
![DogLens Home](screenshots/home.png)
![DogLens Explore](screenshots/explore.png)
-->

## 🧱 Project organization

```text
app/src/main/
├── java/.../
│   ├── MainActivity.kt
│   ├── ui/
│   ├── adapter/
│   └── model/
└── res/
    ├── drawable/
    ├── font/
    ├── layout/
    ├── menu/
    └── values/
```

*The directory layout is illustrative; package locations and files may vary as development continues.*

## 🚀 Getting started

1. Clone the repository:

   ```bash
   git clone https://github.com/YOUR_USERNAME/YOUR_REPOSITORY.git
   ```

2. Open the cloned directory in **Android Studio**.
3. Let **Gradle Sync** finish and install any Android SDK packages Android Studio requests.
4. Select an emulator or connect an Android device.
5. Click **Run ▶** to build and launch the application.

> Replace `YOUR_USERNAME/YOUR_REPOSITORY` with the actual GitHub repository path.

## 🗺️ Roadmap

- [x] Design the Home screen with XML.
- [x] Create reusable feature and breed item layouts.
- [x] Set up the main navigation UI.
- [ ] Implement image picking and camera flow.
- [ ] Integrate breed-recognition API/model.
- [ ] Load breed lists and details from a REST API.
- [ ] Implement favorites persistence.
- [ ] Add identification history.
- [ ] Improve responsive layouts, accessibility, and testing.

## 🎯 Project goals

DogLens is a hands-on Android development project focused on Kotlin, XML UI design, reusable RecyclerView components, Fragment navigation, and future API integration.

---

<div align="center">

**Made with Kotlin for dog lovers 🐶**

</div>
