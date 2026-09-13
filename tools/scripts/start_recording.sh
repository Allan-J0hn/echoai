#!/bin/bash
adb shell am startservice -a com.echoai.app.service.START com.echoai.app/.service.RecordingForegroundService
