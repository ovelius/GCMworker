package com.example.gcmworker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getSystemService
import com.example.gcmworker.ui.theme.GCMWorkerTheme
import java.lang.Long.max
import java.lang.Long.min
import java.util.Date


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GCMWorkerTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Greeting(this)
                }
            }
        }
    }
}

const val JOB_ID = 0xdead
const val GCM_ALARM = 0xdeadb


fun hasWifi(context : Context) : Boolean {
    val wifiMgr = context.getSystemService(Context.WIFI_SERVICE) as WifiManager?
    return if (wifiMgr!!.isWifiEnabled) {
        val wifiInfo = wifiMgr!!.connectionInfo
        wifiInfo.networkId != -1
    } else {
        false
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, p1: Intent?) {
        if (hasWifi(context!!)) {
            println("Sending HB")
            context.sendBroadcast(hB2)
        } else {
            println("Not sending HB because not on Wifi")
        }
    }
}

val hB2 = Intent("com.google.android.intent.action.MCS_HEARTBEAT")
var alarmInterval = 60 * 1000L

private  fun scheduleJob(context : Context, alertIntent: Intent, id: Int, cancel : Boolean) {
    val pendingIntent = PendingIntent.getBroadcast(
        context, id,
        alertIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
    )
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    if (cancel) {
        alarmManager.cancel(pendingIntent)
    } else {
        alarmManager.setInexactRepeating(
            AlarmManager.RTC,
            System.currentTimeMillis() + alarmInterval,
            alarmInterval,
            pendingIntent
        )
    }
}

@Composable
fun Greeting(context : Context, modifier: Modifier = Modifier) {
    val size = 100.dp
    Column (Modifier.fillMaxHeight(),
        verticalArrangement =  Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {

        Text(modifier= modifier.padding(20.dp), text = "This uses AlarmManager.setInexactRepeating() alarms which trigger when OS thinks it's power efficient to trigger =)")

        Button(modifier = Modifier.height(size), onClick = {
            scheduleJob(context, Intent(context, AlarmReceiver::class.java), JOB_ID,false)
            Toast.makeText(context, "Alarm scheduled!", Toast.LENGTH_SHORT).show()
        }) {
            Text(text = "Schedule extra GCM heartbeats only on Wifi - Via this APP")
        }
        Button(modifier = Modifier.height(size), onClick = {
            scheduleJob(context, hB2, GCM_ALARM,false)
            Toast.makeText(context, "Gcm HB Alarm scheduled!", Toast.LENGTH_SHORT).show()
        }) {
            Text(text = "Schedule extra GCM heartbeats - direct Intent to FCM")
        }
        Button(modifier = Modifier.height(size), onClick = {
            context.sendBroadcast(hB2)
            Toast.makeText(context, "Gcm HB sent!", Toast.LENGTH_SHORT).show()
        }) {
            Text(text = "Send GCM heartbeat now!")
        }
        Button(modifier = Modifier.height(size), onClick = {
            scheduleJob(context, hB2 , GCM_ALARM,true)
            scheduleJob(context, Intent(context, AlarmReceiver::class.java), JOB_ID,true)
            Toast.makeText(context, "All cancelled!", Toast.LENGTH_SHORT).show()
        }) {
            Text(text = "Cancel all scheduled alarms!")
        }
        LabelAndPlaceHolder(context)

        Text(text = "Use 'adb shell dumpsys alarm' to see alarms search 'com.example.gcmworker'. Logcat for 'GCM_HB_ALARM' to see triggers.")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelAndPlaceHolder(context : Context) {
    var text by remember { mutableStateOf(TextFieldValue("${alarmInterval / 1000}")) }
    TextField(
        modifier = Modifier.padding(20.dp),
        value = text,
        onValueChange = {
            text = it
            if (it.text.isNotEmpty()) {
                val seconds = max(30L, it.text.toLong())
                alarmInterval = seconds * 1000L
                Toast.makeText(
                    context,
                    "Alarm interval set $seconds seconds for future alarms!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        label = { Text(text = "Roughly seconds between triggers") },
        placeholder = { Text(text = "60") },
    )
}