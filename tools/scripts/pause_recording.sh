#!/bin/bash
adb shell am startservice -a com.example.echoai.service.PAUSE com.example.echoai/.service.RecordingForegroundService
