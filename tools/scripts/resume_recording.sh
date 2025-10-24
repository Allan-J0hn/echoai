#!/bin/bash
adb shell am startservice -a com.example.echoai.service.RESUME com.example.echoai/.service.RecordingForegroundService
