package kr.ac.konkuk.alarmapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kr.ac.konkuk.alarmapp.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        //뷰를 초기화해주기
        initOnOffButton()
        initChangeAlarmTimeButton()

        //데이터 가져오기
        val model = fetchDataFromSharedPreferences()

        //뷰에 데이터를 그려주기
        renderView(model)


    }

    private fun initOnOffButton() {
        binding.onOffButton.setOnClickListener {

            //모델을 가져온다
            val model = it.tag as? AlarmDisplayModel ?: return@setOnClickListener
            //데이터를 저장
            val newModel = saveAlarmModel(model.hour, model.minute, model.onOff.not())
            renderView(newModel)
            //데이터를 확인한다
            //온오프에 따라 작업을 처리한다

            if (newModel.onOff){
                //켜진 경우 -> 알람을 등록
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, newModel.hour)
                    set(Calendar.MINUTE, newModel.minute)

                    //현재시각보다 이전 시각을 입력하면 다음날 시각으로
                    if(before(Calendar.getInstance())) {
                        add(Calendar.DATE,1)
                    }
                }

                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(this, AlarmReceiver::class.java)

                //기존에 존재하는 PendingIntent가 있다면 업데이트 하겠다.
                val pendingIntent = PendingIntent.getBroadcast(this, ALARM_REQUEST_CODE,
                    intent, PendingIntent.FLAG_UPDATE_CURRENT)

                //정확한 것은 alarmManager.setExact()
                //반복이 발생하지 않아 다음알람을 설정해주어야 함

                //잠자기 모드에서도 실행이 되려면

                //비정확한 api
                alarmManager.setInexactRepeating(
                    //ELAPSED_REAL_TIME_WAKEUP을 권장하지만 Calendar를 사용하기에 RTC_WAKEUP
                    //정확한 시간에 알람이 울리려면 1초마다 기기가 체크를 하기때문에 자원을 많이 소모함
                    //6시59분에 알람을 설정하면 6시59분과 7시 사이에 언저리에 알람이 울리게 됨
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    //하루에 한번씩 펜딩인텐트가 실행
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            } else {
                //꺼진 경우 -> 알람을 제거
                cancelAlarm()
            }
        }
    }

    private fun initChangeAlarmTimeButton() {
        binding.changeAlarmTimeButton.setOnClickListener {
            //현재시간을 일단 가져온다
            val calendar = Calendar.getInstance()

            //TimePickDialog 띄워줘서 시간을 설정을 하도록 하게끔 하고, 그 시간을 가져와서
            TimePickerDialog(this, { picker, hour, minute ->

                //데이터를 저장한다.
                val model = saveAlarmModel(hour, minute, false)

                //뷰를 업데이트 한다.
                renderView(model)

                //기존에 있던(등록되어있던) 알람을 삭제한다.
                cancelAlarm()
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false)
                .show()
        }
    }

    private fun saveAlarmModel(
        hour: Int,
        minute: Int,
        onOff: Boolean
    ): AlarmDisplayModel {
        val model = AlarmDisplayModel(
            hour = hour,
            minute = minute,
            onOff = onOff
        )

        val sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()){
            putString(AlARM_KEY, model.makeDataForDB())
            putBoolean(ONOFF_KEY, model.onOff)
            commit()
        }
        return model
    }

    private fun fetchDataFromSharedPreferences(): AlarmDisplayModel {
        val sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)

        //@Nullable이기 때문에
        val timeDBValue = sharedPreferences.getString(AlARM_KEY, "9:30") ?: "9:30"

        val onOffDBValue = sharedPreferences.getBoolean(ONOFF_KEY, false)

        val alarmData = timeDBValue.split(":")

        val alarmModel = AlarmDisplayModel(
            hour = alarmData[0].toInt(),
            minute = alarmData[1].toInt(),
            onOff = onOffDBValue
        )

        //예외처리
        //알람이 등록이 되어있는지, 안되어있는지 확인하는 방법 -> BroadCast 를 가져와서 펜딩인텐트가 실제로 등록이 되어있는지 안되어있는지를 확인을 해보면 됨
        val pendingIntent = PendingIntent.getBroadcast(this,
            ALARM_REQUEST_CODE, Intent(this,
                AlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE)

        if ((pendingIntent == null) and alarmModel.onOff) {
            //알람은 꺼져있는데, 데이터는 커져있는 경우
            alarmModel.onOff = false
        }
        else if ((pendingIntent != null) and alarmModel.onOff.not()){
            //알람은 켜져있는데, 데이터는 꺼져있는 경우
            //알람을 취소함
            pendingIntent.cancel()
        }

        return alarmModel

    }

    private fun renderView(model: AlarmDisplayModel) {
        binding.ampmTextView.apply{
            text = model.ampmText
        }
        binding.timeTextView.apply {
            text = model.timeText
        }
        binding.onOffButton.apply{
            text = model.onOffText
            //model을 tag에 잠시 저장, 버튼을 눌렀을 때 tag에 있는 데이터를 가져와서 구성하는 형식
            tag = model
        }
    }

    private fun cancelAlarm() {
        val pendingIntent = PendingIntent.getBroadcast(this,
            ALARM_REQUEST_CODE, Intent(this,
                AlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE)
        pendingIntent?.cancel()
    }

    companion object {
        private const val SHARED_PREFERENCES_KEY = "time"
        private const val AlARM_KEY = "alarm"
        private const val ONOFF_KEY = "onOff"
        private const val ALARM_REQUEST_CODE = 1000
    }

}