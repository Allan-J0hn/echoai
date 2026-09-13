#!/bin/bash
adb shell am startservice -a com.echoai.app.service.STOP com.echoai.app/.service.RecordingForegroundService
