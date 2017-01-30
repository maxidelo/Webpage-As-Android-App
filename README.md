Mobile Apps wrapper
===================

Description:
-------------

Generic template to run any Web Page as Android application. 

How to modify:
-------------

- app/build.gradle
	- **applicationId:** identifies the package name on Android (demo apks add .demo to this property)
- app/src/main/res/values/strings.xml
	- **app_name:** it's the application name that will be shown on the icon
	- **app_url:** it's the URL that will be loaded by the WebView
- app/src/main/res/mipmap-{size} => Is the image that will be used as icon

Building and compiling apks:
-------------

Using Android Studio

1) Build -> Generate Signed APK...
2) Select or create a keystore
3) Make sure that build type is in release if you are generating release apks or debug otherwise
4) Using a terminal go to the directory that contains the apk and run the zipalign command:

- zipalign 4 {APK_NAME}.apk {APK_NAME}-release.apk

5) In the folder will be the compiled apk ready to be distributed.

Future improvements:
-------------

1) Add support to easily manage multiple webpages with only one app

Other notes:
-------------

1) How to add the zipalign command ?

a) Zipalign is aa executable that will be found in Android SDK (.../Android/sdk/build-tools/{version}/)
b) Add that folder to the PATH