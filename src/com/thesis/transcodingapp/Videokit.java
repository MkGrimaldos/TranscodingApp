package com.thesis.transcodingapp;

public final class Videokit {

  static {
    System.loadLibrary("videokit");
  }

  public native void run(String[] args);

}
