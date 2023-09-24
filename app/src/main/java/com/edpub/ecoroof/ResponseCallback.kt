package com.edpub.ecoroof

interface ResponseCallback {
    fun onSuccess(response:String)
    fun onError(response: String)
}