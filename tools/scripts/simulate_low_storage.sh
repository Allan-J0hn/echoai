#!/bin/bash
adb shell dd if=/dev/zero of=/sdcard/largefile bs=1M count=1024
