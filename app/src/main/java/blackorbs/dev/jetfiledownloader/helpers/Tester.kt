package blackorbs.dev.jetfiledownloader.helpers

import blackorbs.dev.jetfiledownloader.entities.Status

fun main(){
    val status = Status.Error.apply { text = "Some, error, occured so" }
    println(status.toString())
    println(status.name + status.text)
    val arrS = arrayOf(status.name, status.text).contentDeepToString()
    println(arrS)
    val separator = "\n"
    val arrA = "${status.name}$separator${status.text}"//arrayOf(status.name, status.text).joinToString()
    println(arrA)
    val arr = arrA.split(separator, limit = 2)
    println(arr[0])
    println(arr[1])
}