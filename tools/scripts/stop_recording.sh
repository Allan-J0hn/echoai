#!/bin/bash
adb shell am startservice -a com.example.echoai.service.STOP com.example.echoai/.service.RecordingForegroundService
