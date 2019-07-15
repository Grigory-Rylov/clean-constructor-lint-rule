package com.github.grishberg.myapplication

class ExpensiveClass {
    init {
        doSomeExpensiveThing()
    }

    private fun doSomeExpensiveThing() {
        Thread.sleep(1000)
    }

}