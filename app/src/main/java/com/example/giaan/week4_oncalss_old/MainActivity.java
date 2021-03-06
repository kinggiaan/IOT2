package com.example.giaan.week4_oncalss_old;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
import android.widget.Toast;
import android.widget.ToggleButton;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
    TextView yourLocation;
    private FusedLocationProviderClient fusedLocationClient;

    //////GET weather by location
    private final String url = "http://api.openweathermap.org/data/2.5/weather";
    private final String appid = "e53301e27efa0b66d05045d91b2742d3";
    String latString,lonString;

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
        txtTemperate.setText("80??C");
        txtHumidity.setText("45%");
        txtLED.setText("State");
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
        yourLocation = findViewById(R.id.yourLocation);
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

        //Weather Detail



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
              txtTemperate.setText(message.toString()+"??C");

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
    /////// Location
    private void getLocation() {


        fusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {

            @Override
            public void onComplete(@NonNull Task<Location> task) {
                lat.setText("Changing");
                lon.setText("Changing");
                //Inilize Location
                Location location = task.getResult();

                if (location != null) {
                    try {
                        Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                        //initial addresslist

                        List<Address> addresses = geocoder.getFromLocation(
                                location.getLatitude(), location.getLongitude(), 1);
                        //Set latitu on text
                        lat.setText(Html.fromHtml(
                                "Latitude :"
                                        + addresses.get(0).getLatitude()));
                        //Set lontidtu on text
                        lon.setText(Html.fromHtml(

                                "Longtitude :"

                                        + addresses.get(0).getLongitude()));
                        latString =  String.valueOf(addresses.get(0).getLatitude()) ;
                        lonString = String.valueOf(addresses.get(0).getLongitude()) ;
                        Log.d("Result Location",latString);
                        Log.d("Result Location",lonString);
                        getWeatherDetails();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                }
            }
        });

    }

    ///// Get Weather Detail
    public void getWeatherDetails( ) {
        String tempUrl = "";
        if(latString.equals("")&&lonString.equals("")){
            yourLocation.setText("Your Location: NOT FOUND");
        }else{
            yourLocation.setText("Your Location: NOT FOUND");
                tempUrl = url + "?lat=" + latString + "&lon=" + lonString + "&appid=" + appid;

            StringRequest stringRequest = new StringRequest(Request.Method.POST, tempUrl, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    String output = "";
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        JSONArray jsonArray = jsonResponse.getJSONArray("weather");
                        JSONObject jsonObjectWeather = jsonArray.getJSONObject(0);
                        String description = jsonObjectWeather.getString("description");
                        JSONObject jsonObjectMain = jsonResponse.getJSONObject("main");
                        double temp = jsonObjectMain.getDouble("temp") - 273.15;
                        double feelsLike = jsonObjectMain.getDouble("feels_like") - 273.15;
                        float pressure = jsonObjectMain.getInt("pressure");
                        int humidity = jsonObjectMain.getInt("humidity");
                        JSONObject jsonObjectWind = jsonResponse.getJSONObject("wind");
                        String wind = jsonObjectWind.getString("speed");
                        JSONObject jsonObjectClouds = jsonResponse.getJSONObject("clouds");
                        String clouds = jsonObjectClouds.getString("all");
                        JSONObject jsonObjectSys = jsonResponse.getJSONObject("sys");
                        String countryName = jsonObjectSys.getString("country");
                        String cityName = jsonResponse.getString("name");
                        yourLocation.setTextColor(Color.rgb(68,134,199));

                        String tempS= String.valueOf(temp);
                        if(tempS.length()>4){
                            tempS=tempS.substring(0,4);
                        }

                        output += "Current weather of " + cityName + " (" + countryName + ")"
                                + "\n ??? Temp: "+ tempS + "??C"
                                + "\n ??? Humidity: " + humidity + "%"
                                + "\n ??? Wind Speed: " + wind + "m/s (meters per second)";

                        yourLocation.setText(output);



                        //// SEND DATA to Adafruit here
                        sendDataMQTT("kinggiaan/feeds/iot-lab.iot-humidity",String.valueOf(humidity));
                        sendDataMQTT("kinggiaan/feeds/iot-lab.iot-temp",tempS );
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }, new Response.ErrorListener(){

                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(getApplicationContext(), error.toString().trim(), Toast.LENGTH_SHORT).show();
                }
            });
            RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
            requestQueue.add(stringRequest);
        }
    }
}
