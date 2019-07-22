package com.github.grishberg.myapplication

import javax.inject.Inject

class TestedClass @Inject constructor(private val expensiveField: ExpensiveClass) {

    fun doSomeExpensiveThing() {
        Thread.sleep(1000)
        expensiveField.doSomeExpensiveThing()
    }
}