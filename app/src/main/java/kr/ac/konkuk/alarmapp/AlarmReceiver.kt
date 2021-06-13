package kr.ac.konkuk.alarmapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class AlarmReceiver: BroadcastReceiver() {

    //activity가 아니기 때문에 context를 따로 받아와야함
    //context: 실행하고 있는 앱의 상태나 맥락의 의미를 담고있음
    //context가 하는 일: 안드로이드 앱이 환경에서 global한 정보나 안드로이드API나, 시스템이 관리하고 있는 정보
    //(sharedPreferencese, 리소스파일에 접근한다거나), 기능들을 저장을 해놓은 곳에 접근할때 필요한 객체
    //Activity는 하나의 도화지, spf나 리소스파일에 접근하기 용이한 상태이기 때문에 Activity자체가 context라고 할 수 있음
    //Activity가 context를 상속을 하고 있기 때문에 ㅇㅇ
    override fun onReceive(context: Context, intent: Intent) {

        //실제로 BroadCast에서 알람이 내려오면 알람이 onReceive를 통해
        //createNotificationChannel로 채널이 없으면 채널을 만들어주고
        createNotificationChannel(context)
        //채널에 notify를 해줌
        notifyNotification(context)
    }

    private fun createNotificationChannel(context: Context) {
        //public static final int O = 26; 26버전 이상일 경우
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "기상 알람",
                NotificationManager.IMPORTANCE_HIGH
            )

            NotificationManagerCompat.from(context).createNotificationChannel(notificationChannel)
        }
    }

    private fun notifyNotification(context: Context) {
        with(NotificationManagerCompat.from(context)) {
            val build = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("알람")
                .setContentText("일어날 시간입니다.")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)

            notify(NOTIFICATION_ID, build.build())
        }
    }

    companion object {
        const val NOTIFICATION_ID = 100
        const val NOTIFICATION_CHANNEL_ID = "1000"
    }

}