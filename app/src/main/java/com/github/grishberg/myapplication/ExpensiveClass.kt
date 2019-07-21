package com.github.grishberg.myapplication

class ExpensiveClass {
    private val map: Map<String, String>

    init {
        map = HashMap()
        map["1"] = "2"
        doSomeExpensiveThing()
    }

    private fun doSomeExpensiveThing() {
        Thread.sleep(1000)
    }

}