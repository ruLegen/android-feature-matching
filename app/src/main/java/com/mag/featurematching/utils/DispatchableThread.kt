package com.mag.featurematching.utils

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class DispatchableThread :AutoCloseable {
    var isFinished:Boolean = false
        private set

    var messageQueue:BlockingQueue<Runnable>? = null
    var thread:Thread? = null;

    init {
        messageQueue = LinkedBlockingQueue()
        thread = Thread(this::mainLoop)
    }

    fun start() {
        if (isFinished) throw Exception ("Cannot start finished Thread. Create new one");
        thread?.start();
    }

    fun stop(){
        isFinished = true;
    }

    fun dispatch(r :Runnable){
        messageQueue?.clear()
        messageQueue?.add(r)
    }

    private fun mainLoop(){
        while (!isFinished) {
            val runnable = messageQueue?.take();
            if (!isFinished) {
                runnable?.run()
            }
        }
    }

    override fun close() {
        stop();
        thread = null;
    }
}