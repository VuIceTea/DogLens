<div align="center">

# 🐾 DogLens

### Discover dog breeds through images

An Android app for identifying dog breeds, exploring breed profiles and articles, and saving discoveries.

</div>

## Features

- **Photo recognition** uses CameraX or the Android photo picker. Users can crop and rotate an image before recognition, review confidence and breed information, and download or share the result.
- **Breed discovery** includes search, size categories, featured topics, working roles, similar breeds, and saved favorites.
- **Articles** include a searchable library, list and cover layouts, bookmarks, and reading history.
- **Account and activity** include Firebase sign-in, account settings, recognition history, notifications, and a Premium checkout entry point.

## App screenshots

Captured from a local DogLens build on an Android emulator. Click a screenshot to open it at full size.

### Home and breed discovery

| Home | Explore | Breed search |
| --- | --- | --- |
| <a href="screenshots/home.webp"><img src="screenshots/home.webp" alt="Home" width="216"></a> | <a href="screenshots/explore.webp"><img src="screenshots/explore.webp" alt="Explore" width="216"></a> | <a href="screenshots/breed-search.webp"><img src="screenshots/breed-search.webp" alt="Breed search" width="216"></a> |

| Categories | Featured topics | Working roles |
| --- | --- | --- |
| <a href="screenshots/explore-categories.webp"><img src="screenshots/explore-categories.webp" alt="Categories" width="216"></a> | <a href="screenshots/explore-topics.webp"><img src="screenshots/explore-topics.webp" alt="Featured topics" width="216"></a> | <a href="screenshots/explore-roles.webp"><img src="screenshots/explore-roles.webp" alt="Working roles" width="216"></a> |

| Breed profile | Similar breeds | Favorites |
| --- | --- | --- |
| <a href="screenshots/breed-detail.webp"><img src="screenshots/breed-detail.webp" alt="Breed profile" width="216"></a> | <a href="screenshots/similar-breeds.webp"><img src="screenshots/similar-breeds.webp" alt="Similar breeds" width="216"></a> | <a href="screenshots/favorites.webp"><img src="screenshots/favorites.webp" alt="Favorites" width="216"></a> |

### Photo recognition

| Choose an image source | In-app camera | Crop and rotate |
| --- | --- | --- |
| <a href="screenshots/image-source.webp"><img src="screenshots/image-source.webp" alt="Choose an image source" width="216"></a> | <a href="screenshots/camera.webp"><img src="screenshots/camera.webp" alt="In-app camera" width="216"></a> | <a href="screenshots/image-preview.webp"><img src="screenshots/image-preview.webp" alt="Crop and rotate" width="216"></a> |

| Recognition in progress | Breed result | Unrecognized camera photo |
| --- | --- | --- |
| <a href="screenshots/recognition-progress.webp"><img src="screenshots/recognition-progress.webp" alt="Recognition in progress" width="216"></a> | <a href="screenshots/recognition-result.webp"><img src="screenshots/recognition-result.webp" alt="Breed result" width="216"></a> | <a href="screenshots/recognition-no-dog.webp"><img src="screenshots/recognition-no-dog.webp" alt="Unrecognized camera photo" width="216"></a> |

| Recognition history | Notifications |
| --- | --- |
| <a href="screenshots/recognition-history.webp"><img src="screenshots/recognition-history.webp" alt="Recognition history" width="216"></a> | <a href="screenshots/notifications.webp"><img src="screenshots/notifications.webp" alt="Notifications" width="216"></a> |

### Article library

| Article list | Cover layout | Article detail |
| --- | --- | --- |
| <a href="screenshots/articles-cards.webp"><img src="screenshots/articles-cards.webp" alt="Article list" width="216"></a> | <a href="screenshots/articles.webp"><img src="screenshots/articles.webp" alt="Cover layout" width="216"></a> | <a href="screenshots/article-detail.webp"><img src="screenshots/article-detail.webp" alt="Article detail" width="216"></a> |

| Article sections | Reading history |
| --- | --- |
| <a href="screenshots/article-contents.webp"><img src="screenshots/article-contents.webp" alt="Article sections" width="216"></a> | <a href="screenshots/viewed-articles.webp"><img src="screenshots/viewed-articles.webp" alt="Reading history" width="216"></a> |

### Accounts, settings and Premium

| Guest profile | Sign in | Create account |
| --- | --- | --- |
| <a href="screenshots/guest-profile.webp"><img src="screenshots/guest-profile.webp" alt="Guest profile" width="216"></a> | <a href="screenshots/login.webp"><img src="screenshots/login.webp" alt="Sign in" width="216"></a> | <a href="screenshots/register.webp"><img src="screenshots/register.webp" alt="Create account" width="216"></a> |

| Reset password | Signed-in profile | Edit profile |
| --- | --- | --- |
| <a href="screenshots/forgot-password.webp"><img src="screenshots/forgot-password.webp" alt="Reset password" width="216"></a> | <a href="screenshots/profile.webp"><img src="screenshots/profile.webp" alt="Signed-in profile" width="216"></a> | <a href="screenshots/edit-profile.webp"><img src="screenshots/edit-profile.webp" alt="Edit profile" width="216"></a> |

| Settings | Change password | Premium plans |
| --- | --- | --- |
| <a href="screenshots/settings.webp"><img src="screenshots/settings.webp" alt="Settings" width="216"></a> | <a href="screenshots/change-password.webp"><img src="screenshots/change-password.webp" alt="Change password" width="216"></a> | <a href="screenshots/premium.webp"><img src="screenshots/premium.webp" alt="Premium plans" width="216"></a> |

| Checkout confirmation |
| --- |
| <a href="screenshots/premium-confirmation.webp"><img src="screenshots/premium-confirmation.webp" alt="Checkout confirmation" width="216"></a> |

## Tech stack

| Area | Technology |
| --- | --- |
| Android | Kotlin, XML layouts, Fragments, RecyclerView, CameraX |
| State and asynchronous work | ViewModel, StateFlow, Kotlin Coroutines |
| Catalog and account data | Firebase Authentication, Cloud Firestore |
| Recognition | ONNX Runtime on device, optional recognition backend |
| Payments | Firebase Cloud Functions and VNPAY |

## Run locally

1. Clone the repository and open it in Android Studio.

   ```bash
   git clone https://github.com/VuIceTea/DogLens.git
   ```

2. Add the Firebase Android configuration at `app/google-services.json` for the `com.example.doglens` application ID. Enable the authentication providers you intend to test and configure Firestore. The Android configuration is project-specific.
3. Sync Gradle and run the app on an emulator or an Android device. The minimum Android version is Android 7.0 (API 24).
4. To connect the optional recognition service, follow the [recognition backend setup](recognition_backend/README.md) and set `DOG_RECOGNITION_API_URL` as a Gradle property.

Premium checkout requires separate VNPAY sandbox and Firebase Functions configuration.
