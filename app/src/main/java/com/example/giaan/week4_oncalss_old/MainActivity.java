package com.example.giaan.week4_oncalss_old;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.ToggleButton;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
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

    Button btn_get_location;
    TextView lat;
    TextView lon;

    private FusedLocationProviderClient fusedLocationClient;

    //////GET weather by location
    private final String url = "https://api.openweathermap.org/data/2.5/weather";
    private final String appid = "e53301e27efa0b66d05045d91b2742d3";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtHumidity = findViewById(R.id.txtHumidity);
        txtTemperate = findViewById(R.id.txtTemperate);
        switch1 = (Switch) findViewById(R.id.switch1);
        textButton = (TextView) findViewById(R.id.textButton);
        txtLED = findViewById(R.id.txtLED);

        btnLED = findViewById(R.id.btnLED);
        txtTemperate.setText("80°C");
        txtHumidity.setText("45%");
        txtLED.setText("100");
        switch1.setChecked(true);
        btnLED.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    Log.d("mqtt", "Button is ON");
                    sendDataMQTT("kinggiaan/f/iot-lab.iot-led", "1");
                } else {
                    Log.d("mqtt", "Button is OFF");
                    sendDataMQTT("kinggiaan/f/iot-lab.iot-led", "0");
                }
            }


        });


//        LOCATION
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        btn_get_location = findViewById(R.id.btn_get_location);
        lat = findViewById(R.id.lat);
        lon = findViewById(R.id.lon);

        btn_get_location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Check Permission
                getLocation();
                if (ActivityCompat.checkSelfPermission(MainActivity.this
                        , Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    //IF GRANTED -> GETLOCATION

                } else {
                    ActivityCompat.requestPermissions(MainActivity.this
                            , new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 44);
                }
            }
        });


        setupScheduler();
        startMQTT();

    }

    @SuppressLint("MissingPermission")
    private void getLocation() {


        fusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {

            @Override
            public void onComplete(@NonNull Task<Location> task) {

                //Inilize Location
                Location location = task.getResult();

                if (location != null) {
                    lat.setText("Changing");
                        lon.setText("Changing");
                        Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                        //initial addresslist

                    List<Address> addresses = null;
                    try {
                        addresses = geocoder.getFromLocation(
                                location.getLatitude(), location.getLongitude(), 1);
<<<<<<< HEAD
                        //Set latitu on text
                        lat.setText(Html.fromHtml(
                                "<b><Latitude :</b>"
                                        + addresses.get(0).getLatitude()));
                        //Set lontidtu on text
                        lon.setText(Html.fromHtml(
                                "<b><Longtitude :</b>"
                                        + addresses.get(0).getLongitude()));
=======
>>>>>>> parent of d14e98f (RA LOCATION roi)
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.d("Location","Problem");
                    }
                    //Set latitu on text
                        lat.setText(((int) addresses.get(0).getLatitude()));
                        //Set lontidtu on text
                        lon.setText((int) addresses.get(0).getLongitude());



                }
            }
        });

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
