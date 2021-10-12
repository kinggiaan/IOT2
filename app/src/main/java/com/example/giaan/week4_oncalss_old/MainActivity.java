package com.example.giaan.week4_oncalss_old;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Switch;
import android.widget.TextView;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.ToggleButton;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.Charset;
import java.util.Timer;
import java.util.TimerTask;

import static android.system.Os.connect;

public class MainActivity extends AppCompatActivity {

    MQTTHelper mqttHelper;
    TextView txtTemperate;
    TextView txtHumidity;
    Switch switch1;
    TextView textButton;
    TextView txtLED;
    ToggleButton btnLED;

    ToggleButton btn_get_location;
    TextView location_result;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtHumidity =findViewById(R.id.txtHumidity);
        txtTemperate = findViewById(R.id.txtTemperate);
        switch1 = (Switch)findViewById(R.id.switch1);
        textButton = (TextView)findViewById(R.id.textButton);
        txtLED=findViewById(R.id.txtLED);

        btnLED=findViewById(R.id.btnLED);
        txtTemperate.setText("80°C");
        txtHumidity.setText("45%");
        txtLED.setText("100");
        switch1.setChecked(true);
        btnLED.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){

            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b){
                if(b){
                    Log.d("mqtt","Button is ON");
                    sendDataMQTT("kinggiaan/f/iot-lab.iot-led","1");
                }else{
                    Log.d("mqtt","Button is OFF");
                    sendDataMQTT("kinggiaan/f/iot-lab.iot-led","0");
                }
            }


        });


//        LOCATION

        setupScheduler();
        startMQTT();

    }
    int waiting_period=0;
    boolean sending_mess_aigain=false;
    private void setupScheduler(){

        Timer aTimer =new Timer();
        TimerTask scheduler =new TimerTask() {
            @Override
            public void run() {
                Log.d("mqtt","Simplest  ...");
                if(waiting_period>0){
                    waiting_period--;
                    if(waiting_period==0){ //timer experied
                        sending_mess_aigain=true;
                    }
                }

            }
        };
        aTimer.schedule(scheduler, 30000,2000);

    }
    private void sendDataMQTT(String topic, String value){
        waiting_period=3;
        sending_mess_aigain=false;
        MqttMessage msg = new MqttMessage();
        msg.setId(1234);
        msg.setQos(0);
        msg.setRetained(true);


        byte[] b = value.getBytes(Charset.forName("UTF-8"));
        msg.setPayload(b);

        try {
            mqttHelper.mqttAndroidClient.publish(topic, msg);
        }catch (Exception e){}

    }
    private void startMQTT(){
  mqttHelper=new MQTTHelper(getApplicationContext(),"");
  mqttHelper.setCallback(new MqttCallbackExtended() {
      @Override
      public void connectComplete(boolean reconnect, String serverURI) {
          Log.d("mqtt","Connection is successful");
      }

      @Override
      public void connectionLost(Throwable cause) {

      }

      @Override
      public void messageArrived(String topic, MqttMessage message) throws Exception {
          Log.d("mqtt","Received: "+message.toString());
          if(topic.contains("iot-humidity")){
              txtHumidity.setText(message.toString()+"%");

          }
          if(topic.contains("iot-temp")){
              txtTemperate.setText(message.toString()+"°C");

          }
          if(topic.contains("iot-led")){
              if(message.toString().equals("1")) {
                  btnLED.setChecked(true);
                  txtLED.setText("ON");
              }
              else {
                  btnLED.setChecked(false);
                  txtLED.setText("OFF");
              }
             //if(message.toString()=="0") sendDataMQTT("kinggiaan/f/iot-lab.iot-led","0");;
              //txtLED.setText(message.toString());

       }
      }

      @Override
      public void deliveryComplete(IMqttDeliveryToken token) {

      }



  });

    }
}
