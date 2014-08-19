TranscodingApp
==============

This is the client-side of the MSc thesis "Computation Offloading from an Android Device". It's main function is transcoding a video, either on the device, either offloading the computation to a server, which will be located at the specified IP.

How to configure it
-------------------

It is possible to change the IP and port where the server is expected to be by changing the constants `IP` and `PORT`at the beginning of the Util.java class.

By changing the "OPTION" constant, at the beginning of the MainActivity.java class, selecting 1 or 2, the program will assume the video file is already stored on the Server, or it will send the video file to the Server respectively. This option needs to be consistent with the option picked in the client-side.
