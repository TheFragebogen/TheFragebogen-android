TheFragebogen-android
===

The companion app for Android to make running TheFragebogen on Android fun again.
On startup, the app tries to load a html-file from `Downloads/index.html`.
Collected data is written to `Downloads/TheFragebogen-${timestamp}.csv`.

Features:
* fixed screen brightness (max),
* fixed orientation,
* disabled screen timeout, and
* supports local download (blob URL).

Configuration: none.

__Requirement__: TheFragebogen's `ScreenWaitDataDownload` can only be used, if the `ScreenController` is referenced as global variable called `screenController` .

Technical details
---

The app consists only of a [WebView](https://developer.android.com/reference/android/webkit/WebView).
On startup it checks required permissions and requests these if not yet granted.

Android's [DownloadManager](https://developer.android.com/reference/android/app/DownloadManager) does not support blob's (anymore).
Therefore, blob downloads are intercepted and data are exported via `WebView.evaluateJavascript()` executing `javascript:encodeURI(screenController.requestDataCSV())`.
