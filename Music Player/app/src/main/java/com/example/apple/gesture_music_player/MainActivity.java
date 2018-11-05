package com.example.apple.gesture_music_player;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, MediaPlayer.OnPreparedListener,MediaPlayer.OnErrorListener,MediaPlayer.OnCompletionListener,SensorEventListener {
    private SensorManager mySensormanager;
    private Sensor Light_Sensor;
    private List<String>Down_Up_counter;
    private List<Float>luminance;

    private String name[]=new String[1024];
    private String artical[]=new String[1024];
    private String url[]=new String[1024];
    private int id[]=new int[1024];
    private List<Map<String, Object>> list;
    private ListView listView;
    private MediaPlayer mediaPlayer;
    private int index=0;//当前播放音乐索引
    private boolean isPause=false;
    boolean flage;
    private Handler handler;
    private Handler mHandler = new Handler();
    private int Down_up_cont;
    private int changetime;
    private TextView tv;
    private TextView tv_2;


    @SuppressLint("HandlerLeak")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listView= (ListView) findViewById(R.id.listview);
        list = new ArrayList<Map<String, Object>>();
        Down_Up_counter = new ArrayList<>();
        luminance = new ArrayList<>();

        mediaPlayer=new MediaPlayer();
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnCompletionListener(this);


        mySensormanager= (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        assert mySensormanager != null;
        Light_Sensor = mySensormanager.getDefaultSensor(Sensor.TYPE_LIGHT);

        init();

        handler = new Handler(){
            @SuppressLint("SetTextI18n")
            public void handleMessage(Message msg){
                super.handleMessage(msg);
                int arg1 = msg.arg1;
                int arg2 =msg.arg2;
                String name=(String)msg.obj;
                tv_2.setText(name+arg1);
                tv.setText("Count: "+arg2);
            }
        };
        five_second_counter();

    }
    //按钮初始化
    private void init(){
        Button button1 = (Button) findViewById(R.id.up);
        Button button2 = (Button) findViewById(R.id.stop);
        Button button3 = (Button) findViewById(R.id.next);
        Button button4 = (Button) findViewById(R.id.start);
        Button button5 = (Button) findViewById(R.id.Access_music);
        tv=(TextView)findViewById(R.id.textView);
        tv_2=(TextView)findViewById(R.id.textView2);
        button1.setOnClickListener(this);
        button2.setOnClickListener(this);
        button3.setOnClickListener(this);
        button4.setOnClickListener(this);


    }

    public void Accecc_Local_music(View v){
        ContentResolver contentResolver=getContentResolver();
        @SuppressLint("Recycle") Cursor c=contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,null,null,null,null);
        if (c!=null){
            int i=0;
            while(c.moveToNext()){
                Map<String,Object> map= new HashMap<String, Object>();
                name[i]=c.getString(c.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
                id[i]=c.getInt(c.getColumnIndex(MediaStore.Audio.Media._ID));
                artical[i]=c.getString(c.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                url[i]=c.getString(c.getColumnIndex(MediaStore.Audio.Media.DATA));
                map.put("SongName", name[i]);
                map.put("id", id[i]);
                map.put("Artical", artical[i]);
                map.put("url", url[i]);
                list.add(map);
                i++;
            }

            SimpleAdapter adapter = new SimpleAdapter(getApplicationContext(), list, R.layout.content, new String[]{"SongName", "Artical"}, new int[]{R.id.name, R.id.artical});
            listView.setAdapter(adapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                    if (i<list.size()){
                        if (mediaPlayer.isPlaying()){
                            mediaPlayer.stop();
                            mediaPlayer.reset();
                        }
                        Uri conuri= ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,id[i]);
                        try {
                            mediaPlayer.setDataSource(getApplicationContext(),conuri);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        index=i;
                        isPause=false;
                        mediaPlayer.prepareAsync();
                        mediaPlayer.setLooping(true);



                    }
                }
            });
        }else{
            Toast.makeText(getApplicationContext(),"No Local Music",Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mediaPlayer.start();
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        mediaPlayer.reset();
        return true;
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer!=null){
            if (mediaPlayer.isPlaying())
                mediaPlayer.stop();
            mediaPlayer.release();
        }
        mySensormanager.unregisterListener(this);
    }

    //监听事件
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case  R.id.up:
                up();
                break;
            case R.id.start:
                start();
                break;
            case R.id.stop:
                pause();
                break;
            case R.id.next:
                next();
                break;
        }

    }
    //上一首
    private void up() {
        if (index-1>=0){
            index--;
        }else{
            index=list.size()-1;
        }
        Looping();

    }

    //下一首
    private void next(){
        if (isPause){
            mediaPlayer.stop();
            mediaPlayer.reset();
            isPause=false;
        }
        if (index+1<list.size()){
            index++;
        }else{
            index=0;
        }
        Looping();
    }

    //暂停
    private void pause() {
        if (mediaPlayer.isPlaying()){
            mediaPlayer.pause();
            isPause=true;
        }
    }

    //   播放
    private void start() {
        //恢复播放或者从头播放
        if (isPause){
            mediaPlayer.start();
            isPause=false;
        }else{
            Looping();
        }

    }

    //从头开始播放音乐
    private void Looping() {
        try {
            if (index < list.size()) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                    mediaPlayer.reset();
                }
                Uri conuri = ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id[index]);
                mediaPlayer.setDataSource(getApplicationContext(), conuri);
                mediaPlayer.prepareAsync();
                mediaPlayer.setLooping(true);
                isPause = false;

            }
        }catch (IOException e){e.printStackTrace();}
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        next();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_LIGHT:
                changetime++;
                float acc = event.accuracy;
                final float Y = event.values[0];

                if(luminance.size()<22){
                    luminance.add(Y);
                }
                else {
                    float sum =0;
                    for(int i=0;i<=21;i++){
                        sum = sum+luminance.get(i);
                    }
                    float average = sum / 22;
                    System.out.println(average);
                    luminance.remove(0);
                    luminance.add(Y);
                    double threhold = 0.78;
                    if(Y< average * threhold){
                        flage=true;

                    }
                    if(flage&&Y> average * threhold){
                        flage=false;
                        //Proximity=false;
                        Down_Up_counter.add("count one");

                    }


                }





        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
    protected void onResume(){
        super.onResume();
        mySensormanager.registerListener(this,Light_Sensor,SensorManager.SENSOR_DELAY_FASTEST);

    }
    protected void onPause(){
        super.onPause();
        mySensormanager.unregisterListener(this);
    }

    private void five_second_counter(){
        new Thread(new Runnable() {
            int count =1;
            @Override
            public void run() {
                while (count<6){
                    Down_up_cont=Down_Up_counter.size();
                    Message message=Message.obtain();

                    message.arg1 = count;
                    message.arg2 = Down_up_cont;
                    message.obj="Timer: ";

                    handler.sendMessage(message);
                    count++;
                    try{
                        Thread.sleep(1000);
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                }
                while (count>=6){


                    if(Down_up_cont==2){

                        pause();
                    }
                    if(Down_up_cont==1){

                        start();
                    }
                    if (Down_up_cont==3){
                        next();
                    }
                    if (Down_up_cont==4){
                        up();
                    }

                    Down_Up_counter.clear();
                    count=1;
                    run();
                }
            }
        }).start();
    }
}