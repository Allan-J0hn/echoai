#!/bin/bash
adb shell am startservice -a com.echoai.app.service.RESUME com.echoai.app/.service.RecordingForegroundService
