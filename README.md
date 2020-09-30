# Text renderer

### OpenGL text renderer

This text renderer is a fork of a shader based JOGL text renderer extracted from https://github.com/adbrown85/jogl.

Compared to the original project the project has two goals:

- provide a standalone text renderer package running of GL3 core profile profiler
- be able to run both on JOGL and LWJGL

The GL2 fixed function pipeline implementation is also present, as it was relatively easy to keep it. It will not be
removed unless the maintenance will prove to be harder than expected.   

The primary consumer of this renderer is GLG2D - https://github.com/OpenGrabeso/glg2d 
