package dev.asdevs.signalr_flutter

import android.os.Looper
import io.flutter.plugin.common.MethodChannel.Result
import microsoft.aspnet.signalr.client.*
import microsoft.aspnet.signalr.client.http.android.AndroidPlatformComponent
import microsoft.aspnet.signalr.client.hubs.HubConnection
import microsoft.aspnet.signalr.client.hubs.HubProxy
import microsoft.aspnet.signalr.client.transport.LongPollingTransport
import microsoft.aspnet.signalr.client.transport.ServerSentEventsTransport

enum class CallMethod(val value: String) {
    ConnectToServer("connectToServer"),
    Reconnect("reconnect"),
    Stop("stop"),

    //ADDED BY NICKBAR06 to check status of hub before any other method
    CheckStatus("checkStatus"),
    ListenToHubMethod("listenToHubMethod"),
    InvokeServerMethod("invokeServerMethod")
}

object SignalR {
    private lateinit var connection: HubConnection
    private lateinit var hub: HubProxy

    fun connectToServer(url: String, hubName: String, queryString: String, headers: Map<String, String>, transport: Int, result: Result) {
        try {
            connection = if (queryString.isEmpty()) {
                HubConnection(url)
            } else {
                HubConnection(url, queryString, true, Logger { _: String, _: LogLevel ->
                })
            }

            if (headers.isNotEmpty()) {
                val cred = Credentials() { request ->
                    request.headers = headers
                }
                connection.credentials = cred
            }
            hub = connection.createHubProxy(hubName)
            
          
            connection.connected {
                android.os.Handler(Looper.getMainLooper()).post {
                    SignalRFlutterPlugin.channel.invokeMethod("ConnectionStatus", connection.state.name)
                }
            }

            connection.reconnected {
                android.os.Handler(Looper.getMainLooper()).post {
                    SignalRFlutterPlugin.channel.invokeMethod("ConnectionStatus", connection.state.name)
                }
            }

            connection.reconnecting {
                android.os.Handler(Looper.getMainLooper()).post {
                    SignalRFlutterPlugin.channel.invokeMethod("ConnectionStatus", connection.state.name)
                }
            }

            connection.closed {
                android.os.Handler(Looper.getMainLooper()).post {
                    SignalRFlutterPlugin.channel.invokeMethod("ConnectionStatus", connection.state.name)
                }
            }

            connection.connectionSlow {
                android.os.Handler(Looper.getMainLooper()).post {
                    SignalRFlutterPlugin.channel.invokeMethod("ConnectionStatus", "Slow")
                }
            }

            connection.error { handler ->
                android.os.Handler(Looper.getMainLooper()).post {
                    SignalRFlutterPlugin.channel.invokeMethod("ConnectionStatus", handler.localizedMessage)
                }
            }

            when (transport) {
                1 -> connection.start(ServerSentEventsTransport(connection.logger))
                2 -> connection.start(LongPollingTransport(connection.logger))
                else -> {
                    connection.start()
                }
            }

            result.success(true)
        } catch (ex: Exception) {
            result.error("Error", ex.localizedMessage, null)
        }
    }

    fun reconnect(result: Result) {
        try {
            connection.start()
        } catch (ex: Exception) {
            result.error(ex.localizedMessage, ex.stackTrace.toString(), null)
        }
    }

    fun stop(result: Result) {
        try {
            connection.stop()
        } catch (ex: Exception) {
            result.error(ex.localizedMessage, ex.stackTrace.toString(), null)
        }
    }


  //ADDED BY NICKBAR06 to check status of hub before any other method
    fun checkStatus(result: Result) {
        try{ 
            result.success(connection.state.name)
        }
        catch(ex: Exception) {
            result.error(ex.localizedMessage, ex.stackTrace.toString(), null)
        }
    }

    fun listenToHubMethod(methodName: String, result: Result) {
        try {
            hub.on<String>(methodName, { res ->
                android.os.Handler(Looper.getMainLooper()).post {
                    SignalRFlutterPlugin.channel.invokeMethod("NewMessage", res)
                }
            }, String::class.java)
        } catch (ex: Exception) {
            result.error("Error", ex.localizedMessage, null)
        }
    }


    //ADDED BY NICKBAR06 to add extra arguments
    fun invokeServerMethod(methodName: String, args: List<Any>, result: Result) {
        try {
            val res: SignalRFuture<Any> = when (args.count()) {
                0 -> hub.invoke(Any::class.java, methodName)
                1 -> hub.invoke(Any::class.java, methodName, args[0])
                2 -> hub.invoke(Any::class.java, methodName, args[0], args[1])
                3 -> hub.invoke(Any::class.java, methodName, args[0], args[1], args[2])
                4 -> hub.invoke(Any::class.java, methodName, args[0], args[1], args[2], args[3])
                5 -> hub.invoke(Any::class.java, methodName, args[0], args[1], args[2], args[3], args[4])
                6 -> hub.invoke(Any::class.java, methodName, args[0], args[1], args[2], args[3], args[4], args[5])
                7 -> hub.invoke(Any::class.java, methodName, args[0], args[1], args[2], args[3], args[4], args[5], args[6])
                8 -> hub.invoke(Any::class.java, methodName, args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7])
                9 -> hub.invoke(Any::class.java, methodName, args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8])
                else -> throw Exception("Maximum 9 arguments supported. Your arguments List count is ${args.count()}.")
            }

            res.done { msg: Any? ->
                android.os.Handler(Looper.getMainLooper()).post {
                    result.success(msg)
                }
            }

            res.onError { throwable ->
                android.os.Handler(Looper.getMainLooper()).post {
                    result.error("Error", throwable.localizedMessage, null)
                }
            }
        } catch (ex: Exception) {
            result.error("Error", ex.localizedMessage, null)
        }
    }
}