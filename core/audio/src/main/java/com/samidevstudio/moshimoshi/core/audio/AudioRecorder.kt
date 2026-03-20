package com.samidevstudio.moshimoshi.core.audio

import java.io.File

interface AudioRecorder {
    fun start(outputFile: File)
    fun stop()
}