#!/bin/bash
adb shell am startservice -a com.echoai.app.service.PAUSE com.echoai.app/.service.RecordingForegroundService
