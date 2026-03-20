package com.samidevstudio.moshimoshi.audio

import java.io.File

interface AudioRecorder {
    fun start(outputFile: File)
    fun stop()
}