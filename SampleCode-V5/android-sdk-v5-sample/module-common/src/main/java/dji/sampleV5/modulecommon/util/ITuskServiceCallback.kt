package dji.sampleV5.modulecommon.util

interface ITuskServiceCallback {
    fun callReconnectWebsocket()
    fun callSetIP(ip : String)
}