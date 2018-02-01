#Connect SDK Core Test project (Android)
The Test project contains unit tests for The Connect SDK Core. 

##General Information
For more information about Connect SDK, visit the [main repository](https://github.com/ConnectSDK/Connect-SDK-Android).

##Setup and run from Eclipse
1. Go to Eclipse Menu -> File -> Import -> (General) Existing projects into workspace
2. Open project properties and make sure that java compiler has version 1.6
3. Open project properties -> Java Build Path -> Projects and add a reference to Connect-SDK-Android-Core project
4. Open project properties -> Java Build Path -> Libraries and add all jars from libs folder and add android.jar from sdk/platforms/android-19
5. Open project properties -> Java Build Path -> Order and Export and put android.jar at the bottom of the list.
6. Run as Eclipse JUnit Test

##Run from command line
1. Add a system variable ANDROID_HOME which contains a path to Android SDK
2. Go to Connect-SDK-Core project folder and build it: ant clean debug
3. Go to test folder and execute: ant


##License
Copyright (c) 2013-2014 LG Electronics.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

> http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


