#!/bin/bash
adb shell am startservice -a com.example.echoai.service.START com.example.echoai/.service.RecordingForegroundService
