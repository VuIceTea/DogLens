<div align="center">
  <h1>🐾 DogLens - Dog Breed Recognition</h1>
  <p><em>A native Android app for identifying dog breeds from photos and learning more about them.</em></p>

  [![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
  [![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
  [![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)](https://firebase.google.com/)
</div>

## 📋 Project Overview

**DogLens** lets users take or import a photo, identify a candidate dog breed, and explore its traits and care information. The app brings breed profiles, related articles, favorites, and identification history together in one Android interface.

### 🎯 Project Goals

- Make photo-based breed identification easy from camera capture to result.
- Provide a searchable catalog with breed comparisons and care articles.
- Help users revisit breeds, recognition results, and articles they have saved or viewed.

## ✨ Key Features

### 📷 Photo Recognition

- Capture a photo in the app or select one from the Android photo picker.
- Crop and rotate the image before running recognition.
- See the predicted breed and the model’s confidence, or get feedback when no reliable result is available.
- Download or share a recognition result and revisit it in history.

### 🦮 Breed Discovery

- Browse breeds by size, featured topic, or working role.
- Search the catalog, compare similar breeds, and save favorites.
- Read breed traits, basic measurements, and care information when available.

### 📚 Articles & Activity

- Browse articles in list or cover view, filter by category, and search by title or summary.
- Bookmark articles and revisit reading history.
- Review recognition notifications and open the associated results.

### 👤 Account & Premium

- Create or sign in to an account, request a password reset, and edit a profile.
- Manage notification preferences and account settings.
- View monthly and yearly Premium plans and open the VNPAY sandbox checkout when configured.

## 🛠️ Technology & Architecture

### Android UI

- **Kotlin** and **Android XML** for the native app.
- **Fragments**, **RecyclerView**, and **Material Components** for screens and navigation.
- **CameraX**, the **Android photo picker**, and **Glide** for image capture, selection, and rendering.

### State Management & Data

- **ViewModel**, **StateFlow**, and **Kotlin Coroutines** for UI state and asynchronous work.
- **Repositories** for catalog, article, recognition, and account operations.
- **Firebase Authentication** and **Cloud Firestore** for identity and app data.

### Recognition & Payments

- **ONNX Runtime** for on-device recognition and an optional **FastAPI** recognition service.
- **Firebase Cloud Functions** and **VNPAY Sandbox** for Premium checkout.

### Development & Testing

- **Android Studio**, the **Gradle Wrapper**, **Git**, and **Firebase CLI**.
- Android instrumentation tests for local recognition and Python API tests for the recognition service.

<a name="app-screenshots"></a>

## 📱 Application Interface

Screenshots captured from DogLens running on an Android emulator. Click an image to open it at full size.

### 🔐 Authentication & Onboarding

<div align="center">
  <a href="screenshots/guest-profile.webp"><img src="screenshots/guest-profile.webp" width="200" alt="Guest profile"/></a>
  <a href="screenshots/login.webp"><img src="screenshots/login.webp" width="200" alt="Login"/></a>
  <a href="screenshots/register.webp"><img src="screenshots/register.webp" width="200" alt="Register"/></a>
  <a href="screenshots/forgot-password.webp"><img src="screenshots/forgot-password.webp" width="200" alt="Forgot password"/></a>
</div>

### 🏠 Home & Navigation

<div align="center">
  <a href="screenshots/home.webp"><img src="screenshots/home.webp" width="200" alt="Home"/></a>
  <a href="screenshots/explore.webp"><img src="screenshots/explore.webp" width="200" alt="Explore"/></a>
  <a href="screenshots/breed-search.webp"><img src="screenshots/breed-search.webp" width="200" alt="Breed search"/></a>
  <a href="screenshots/favorites.webp"><img src="screenshots/favorites.webp" width="200" alt="Favorites"/></a>
</div>

### 🦮 Breed Discovery

<div align="center">
  <a href="screenshots/explore-categories.webp"><img src="screenshots/explore-categories.webp" width="200" alt="Explore categories"/></a>
  <a href="screenshots/explore-topics.webp"><img src="screenshots/explore-topics.webp" width="200" alt="Explore topics"/></a>
  <a href="screenshots/explore-roles.webp"><img src="screenshots/explore-roles.webp" width="200" alt="Explore roles"/></a>
  <a href="screenshots/breed-detail.webp"><img src="screenshots/breed-detail.webp" width="200" alt="Breed detail"/></a>
</div>

<div align="center">
  <a href="screenshots/similar-breeds.webp"><img src="screenshots/similar-breeds.webp" width="200" alt="Similar breeds"/></a>
</div>

### 📷 Photo Recognition

<div align="center">
  <a href="screenshots/image-source.webp"><img src="screenshots/image-source.webp" width="200" alt="Image source"/></a>
  <a href="screenshots/camera.webp"><img src="screenshots/camera.webp" width="200" alt="Camera"/></a>
  <a href="screenshots/image-preview.webp"><img src="screenshots/image-preview.webp" width="200" alt="Crop and rotate"/></a>
  <a href="screenshots/recognition-progress.webp"><img src="screenshots/recognition-progress.webp" width="200" alt="Recognition progress"/></a>
</div>

<div align="center">
  <a href="screenshots/recognition-result.webp"><img src="screenshots/recognition-result.webp" width="200" alt="Recognition result"/></a>
  <a href="screenshots/recognition-no-dog.webp"><img src="screenshots/recognition-no-dog.webp" width="200" alt="Unrecognized photo"/></a>
  <a href="screenshots/recognition-history.webp"><img src="screenshots/recognition-history.webp" width="200" alt="Recognition history"/></a>
  <a href="screenshots/notifications.webp"><img src="screenshots/notifications.webp" width="200" alt="Notifications"/></a>
</div>

### 📚 Article Library

<div align="center">
  <a href="screenshots/articles-cards.webp"><img src="screenshots/articles-cards.webp" width="200" alt="Article list"/></a>
  <a href="screenshots/articles.webp"><img src="screenshots/articles.webp" width="200" alt="Article cover view"/></a>
  <a href="screenshots/article-detail.webp"><img src="screenshots/article-detail.webp" width="200" alt="Article detail"/></a>
  <a href="screenshots/article-contents.webp"><img src="screenshots/article-contents.webp" width="200" alt="Article contents"/></a>
</div>

<div align="center">
  <a href="screenshots/viewed-articles.webp"><img src="screenshots/viewed-articles.webp" width="200" alt="Viewed articles"/></a>
</div>

### 👤 Profile & Settings

<div align="center">
  <a href="screenshots/profile.webp"><img src="screenshots/profile.webp" width="200" alt="Signed-in profile"/></a>
  <a href="screenshots/edit-profile.webp"><img src="screenshots/edit-profile.webp" width="200" alt="Edit profile"/></a>
  <a href="screenshots/settings.webp"><img src="screenshots/settings.webp" width="200" alt="Settings"/></a>
  <a href="screenshots/change-password.webp"><img src="screenshots/change-password.webp" width="200" alt="Change password"/></a>
</div>

### 💎 Premium Checkout

<div align="center">
  <a href="screenshots/premium.webp"><img src="screenshots/premium.webp" width="200" alt="Premium plans"/></a>
  <a href="screenshots/premium-confirmation.webp"><img src="screenshots/premium-confirmation.webp" width="200" alt="Premium confirmation"/></a>
</div>

## 🚀 Getting Started

### 📋 System Requirements

- Android Studio with Android SDK 37 for compiling the app
- An Android emulator or device running Android 7.0 (API 24) or later
- A Firebase project for Authentication and Cloud Firestore
- Git

### 🛠️ Installation & Setup

#### 1. Clone the repository

```bash
git clone https://github.com/VuIceTea/DogLens.git
cd DogLens
```

#### 2. Configure Firebase

Register the Android app with package name `com.example.doglens` and place the Firebase configuration file at `app/google-services.json`. Enable the sign-in providers you use and configure Cloud Firestore using the repository’s `firestore.rules`.

#### 3. Build and run

Open the project in Android Studio, let Gradle Sync finish, select a device, and run the `app` configuration. Use the following PowerShell command to build a debug APK.

```powershell
.\gradlew.bat :app:assembleDebug
```

#### 4. Configure optional services

- **Recognition API** — Set the Gradle property `DOG_RECOGNITION_API_URL` when using the optional service. [Recognition backend setup](recognition_backend/README.md)
- **Premium** — Configure the VNPAY sandbox credentials on Firebase Cloud Functions for payment testing.

## 📁 Project Structure

```text
DogLens/
├── app/
│   └── src/
│       ├── main/
│       │   ├── java/com/example/doglens/
│       │   │   ├── ui/          # Screens and ViewModels
│       │   │   ├── data/        # Models, repositories, and data sources
│       │   │   └── utils/       # Shared Android utilities
│       │   ├── assets/          # Recognition models and bundled content
│       │   └── res/             # Layouts, drawables, fonts, and strings
│       ├── test/                # Local unit tests
│       └── androidTest/         # Android instrumentation tests
├── functions/                  # Firebase Functions and catalog tooling
├── recognition_backend/        # Optional recognition API
├── screenshots/                # Android screenshots used in this README
└── firestore.rules             # Firestore access rules
```

## 👨‍💻 Author

**Nguyen Phi Vu** - *Android Developer*

- 🐙 GitHub: [@VuIceTea](https://github.com/VuIceTea)
- 📧 Email: [phivu20030411@gmail.com](mailto:phivu20030411@gmail.com)
- 💼 LinkedIn: [Nguyen Phi Vu](https://www.linkedin.com/in/phi-vu-nguyen-8a18a5371/)
- 🌐 Portfolio: [**https://vuicetea.github.io/**](https://vuicetea.github.io/)
