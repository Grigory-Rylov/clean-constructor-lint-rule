package com.github.grishberg.myapplication

class TestedClass {
    val someRef = ExpensiveClass()

    private fun doSomeExpensiveThing() {
        Thread.sleep(1000)
    }
}