# myprojects

Java AV1/Hevc Transcoder GUI Wrapper above ffmpeg

Install ffmpeg on your computer, your computer also needs JDK as this GUI is written in Java Swing

Prerequisites:
--------------------------------------------------------------------
1. Visual Studio Code
--------------------------------------------------------------------
Download and install visual studio code

Link: https://code.visualstudio.com/

--------------------------------------------------------------------
2. Extension Pack for Java
--------------------------------------------------------------------
Open Visual Studio Code and install Extension Pack for Java

You can also install extension pack for Java using Install button in the following Visual Studio Code Marketplace link (this extension pack is provided by Microsoft.com)

Link: https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack

--------------------------------------------------------------------
3. Download ffmpeg, unzip and add it to path
--------------------------------------------------------------------

********************************************************************
3.1 Download latest ffmpeg
********************************************************************

Global Link for any platform: https://ffmpeg.org/download.html

Recommended BtbN as this ffmpeg has latest Nvidia and AMD encoders built: https://github.com/BtbN/FFmpeg-Builds/releases

From this release, choose an lgpl release with version 6 (version 6 is the latest stable release version of ffmpeg, do not choose master which is a development branch version)

For example, ffmpeg 6 lgpl windows 64 bit link: https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-n6.0-latest-win64-lgpl-6.0.zip

********************************************************************
3.2 Unzip ffmpeg you downloaded
********************************************************************

Unzip the downloaded ffmpeg, for example if your download was inside Downloads and you had unzipped, the path might be like as follows, in the following substitute your username in place of &lt;Your Username&gt;

C:\Users\\&lt;Your Username&gt;\Downloads\ffmpeg-n6.0-latest-win64-lgpl-6.0


********************************************************************
3.3 Add to PATH
********************************************************************

3.3.1 

Linux & Mac: Add the bin path to your PATH environment variable in .bash_profile

Relaunch terminal and verify path using echo $PATH

Your PATH environment variable should have the ffmpeg path, if you installed ffmpeg from apt (sudo apt install ffmpeg) or using brew (brew install ffmpeg) your PATH would be already having ffmpeg!

Verify ffmpeg by using following command

ffmpeg -version

3.3.2

Windows: Add the bin path to your PATH environment variable as follows

3.3.2.1 Launch explorer using Windows + E 
3.3.2.2 Right click This PC and select Properties
3.3.2.3 Choose Advanced system settings
3.3.2.4 Select Advanced tab in the launched window
3.3.2.5 Press Environmental Variables... button
3.3.2.6 Double click Path and paste the path of bin folder you saw in unzipping section, press Ok
3.3.2.7 Verify the environment variable by launching a CMD or Terminal and running following command

ffmpeg -version

Your ffmpeg should print the features enabled along with version

for example, my ffmpeg version is as follows

ffmpeg version n6.0-26-g3f345ebf21-20230713 Copyright (c) 2000-2023 the FFmpeg developers

--------------------------------------------------------------------
4. Clone this repository
--------------------------------------------------------------------

You can either download this repository as zip or clone using git

--------------------------------------------------------------------
5. Launch Visual Studio Code
--------------------------------------------------------------------

Right click your cloned folder myprojects-main and choose Code (Windows 11 users should do Show more options to reveal Open with Code

You can also open Visual Studio Code, File -> Open Folder... --> Chose myprojects-main/av1utils (you might have cloned myprojects-main anywhere, so you must be knowing its path)

--------------------------------------------------------------------
6. Run the project
--------------------------------------------------------------------

From the left side toolbar, choose Run and Debug, this should launch the transcoder

--------------------------------------------------------------------
7. Transcode
--------------------------------------------------------------------

Drag a normal mp4 file onto the main screen launched, this should transcode the file to AV1
