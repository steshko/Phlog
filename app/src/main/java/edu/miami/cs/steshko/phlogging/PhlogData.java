package edu.miami.cs.steshko.phlogging;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.Image;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.miami.cs.steshko.phlogging.db.DataRoomDB;
import edu.miami.cs.steshko.phlogging.db.DataRoomEntity;


public class PhlogData extends AppCompatActivity implements SensorEventListener{
    private static final String DATABASE_NAME = "Phlog.db";
    private final int CAMERA_PHOTO_RESULT_CODE = 1;
    private boolean photoExists;
    private boolean recordingExist;
    private boolean recordingOngoing;

    private Bitmap photo;
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;
    private LocationRequest locationRequest;

    private float[] orientation = new float[3];
    private SensorManager sensorManager;

    private float[] gravity = new float[3];
    private float[] magneticField = new float[3];
    private ImageView ivPreview;
    private MediaRecorder recorder;
    private String recordFileName;
    private int currentPreviewImage;
    private int totalPreviewImages;
    ArrayList<String> galleryPhotoList;
    boolean isEdit;
    List<DataRoomEntity> phlogList;
    int position;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phlog_data);
        ivPreview = findViewById(R.id.ivPreview);
        ivPreview.setVisibility(View.GONE);
        galleryPhotoList = new ArrayList<String>();

        Log.i("DEBUG", "START");


        //Set checker global variables
        photoExists = false;
        recordingExist = false;
        recordingOngoing = false;
        currentPreviewImage = -2;
        totalPreviewImages = 0;

        //Location setters
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = new LocationRequest();
        locationRequest.setInterval(getResources().getInteger(
                R.integer.time_between_location_updates_ms));
        locationRequest.setFastestInterval(getResources().getInteger(
                R.integer.time_between_location_updates_ms) / 2);

        fusedLocationClient.removeLocationUpdates(myLocationCallback);
        startLocating(LocationRequest.PRIORITY_HIGH_ACCURACY);

        //Recorder setters
        recordFileName = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC).toString() + "/" +
                getResources().getString(R.string.audio_file_name);
        recorder = new android.media.MediaRecorder();
        recorder.setAudioSource(android.media.MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        recorder.setOutputFile(recordFileName);

        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.i("AUDIO ERROR","PREPARING RECORDER");
        }

        //Get is edit Boolean
        isEdit = this.getIntent().getBooleanExtra("edu.miami.cs.steshko.phlogging/edit", false);

        //If is edit, then load previous data into edtor
        Log.i("DEBUG", "isEdit: "+isEdit);
        if (isEdit){
            //Change buttons to Update Buttons
            ((Button)findViewById(R.id.btnDeletePhlog)).setVisibility(View.VISIBLE);
            ((Button)findViewById(R.id.btnSave)).setText(R.string.update);
            position = this.getIntent().getIntExtra("edu.miami.cs.steshko.phlogging/position", 0);
            Log.i("DEBUG", "position: "+position);
            DataRoomDB phlogDB = Room.databaseBuilder(getApplicationContext(), DataRoomDB.class,
                    DATABASE_NAME).allowMainThreadQueries().build();
            phlogList = phlogDB.daoAccess().getList();
            Log.i("DEBUG", "Got List");

            ((EditText) findViewById(R.id.etTitle)).setText(phlogList.get(position).getTitle());
            ((EditText) findViewById(R.id.etText)).setText(phlogList.get(position).getText());
            Log.i("DEBUG", "Set Title and Text");

            //Get and Set gallery photos
            if (!phlogList.get(position).getGalleryPictures().equals("[]")){
                Log.i("DEBUG", "Getting Gallery List");

                galleryPhotoList = new ArrayList<String>(Arrays.asList(phlogList.get(position).getGalleryPictures()
                        .substring(1, phlogList.get(position).getGalleryPictures().length() - 1).split(", ")));
                currentPreviewImage = 0;
                totalPreviewImages = galleryPhotoList.size();
                ivPreview.setImageURI(Uri.parse(galleryPhotoList.get(0)));
                Log.i("DEBUG", "Set Preview to first Photo");

                imageExists();

            }

            //Get and Set camera Photo
            if (phlogList.get(position).getPhoto() != null){
                Log.i("DEBUG", "Getting Photo Byte[]");
                byte[] photoBitmap = phlogList.get(position).getPhoto();
                photo = BitmapFactory.decodeByteArray(photoBitmap, 0, photoBitmap.length);
                currentPreviewImage = -1;
                photoExists = true;
                totalPreviewImages++;
                ivPreview.setImageBitmap(photo);

                imageExists();
            }

            if (totalPreviewImages > 1){
                Log.i("DEBUG", "totalPreviewImages > 1");
                ImageView btnPrevious = findViewById(R.id.btnPrevious);
                ImageView btnNext = findViewById(R.id.btnNext);
                btnPrevious.setVisibility(View.VISIBLE);
                btnNext.setVisibility(View.VISIBLE);
            }
            phlogDB.close();
        }

    }

    //If image exists set delete button to visible and set preview text
    public void imageExists(){
        Button btnDelete = findViewById(R.id.btnDeleteImage);
        btnDelete.setVisibility(View.VISIBLE);
        ivPreview.setVisibility(View.VISIBLE);
        changePreviewText();
        ((TextView)findViewById(R.id.tvPreview)).setVisibility(View.VISIBLE);
    }

    //Method for updating the image scroller
    public void viewer(int change){
        Button delete = findViewById(R.id.btnDeleteImage);
        if (totalPreviewImages == 0){
            ivPreview.setVisibility(View.VISIBLE);
            TextView tvPreview = findViewById(R.id.tvPreview);
            tvPreview.setVisibility(View.VISIBLE);
            delete.setVisibility(View.VISIBLE);
            if (photoExists){
                ivPreview.setImageBitmap(photo);
                currentPreviewImage = -1;
            }else{
                ivPreview.setImageURI(Uri.parse(galleryPhotoList.get(0)));
                currentPreviewImage = 0;
            }
        }

        //If not changing scroller but adding photos
        if (change == 0){
            //increase total counter
            totalPreviewImages++;
            //if more than two total photos, set buttons
            if (totalPreviewImages == 2){
                ImageView btnPrevious = findViewById(R.id.btnPrevious);
                ImageView btnNext = findViewById(R.id.btnNext);
                btnPrevious.setVisibility(View.VISIBLE);
                btnNext.setVisibility(View.VISIBLE);
            }
            Log.i("DEBUG", "Exit: current " + currentPreviewImage);
            Log.i("DEBUG", "Exit: total " + totalPreviewImages);
        }
        //String[] galleryImageStringArray = galleryPhotoList.Array();


        //Delete Photo option
        if (change == -2){
            Log.i("DEBUG", "Change -2");
            totalPreviewImages--;
            //If only one preview image, remove change buttons
            if (totalPreviewImages == 1){
                ImageView btnPrevious = findViewById(R.id.btnPrevious);
                ImageView btnNext = findViewById(R.id.btnNext);
                btnPrevious.setVisibility(View.GONE);
                btnNext.setVisibility(View.GONE);
            }
            Log.i("DEBUG", "New total: "+totalPreviewImages);
            if (currentPreviewImage == -1){
                Log.i("DEBUG", "Option: currentPreviewImage == -1");
                photoExists = false;
                if (totalPreviewImages > 0){
                    Log.i("DEBUG", "Option: totalPreviewImages > 0");
                    currentPreviewImage = 0;
                    ivPreview.setImageURI(Uri.parse(galleryPhotoList.get(currentPreviewImage)));
                }else{
                    Log.i("DEBUG", "else");
                    ivPreview.setVisibility(View.GONE);
                    TextView tvPreview = findViewById(R.id.tvPreview);
                    tvPreview.setVisibility(View.GONE);
                    delete.setVisibility(View.GONE);
                    Log.i("DEBUG", "else completed");
                }
            }else if (currentPreviewImage > -1){
                galleryPhotoList.remove(currentPreviewImage);
                if (totalPreviewImages == 1 && photoExists){
                    ivPreview.setImageBitmap(photo);
                    currentPreviewImage = -1;
                }else if (totalPreviewImages > 0){
                    Log.i("DEBUG", "totalPreviewImages > 0");
                    if (currentPreviewImage == totalPreviewImages - 1 && photoExists || currentPreviewImage == totalPreviewImages && !photoExists){
                        Log.i("DEBUG", "or ENTERED");
                        if (photoExists){
                            Log.i("DEBUG", "photo exists");
                            currentPreviewImage = -1;
                            ivPreview.setImageBitmap(photo);
                        }else{
                            Log.i("DEBUG", "photo does not exist");
                            currentPreviewImage = 0;
                            ivPreview.setImageURI(Uri.parse(galleryPhotoList.get(currentPreviewImage)));
                        }
                    }else{
                        Log.i("DEBUG", "Increment, and set Image");
                        Log.i("DEBUG", "currentPreviewImage: "+currentPreviewImage);
                        Log.i("DEBUG", "galleryPhotoList.size(): "+galleryPhotoList.size());
                        Log.i("DEBUG", "galleryPhotoList: "+galleryPhotoList);
                        ivPreview.setImageURI(Uri.parse(galleryPhotoList.get(currentPreviewImage)));
                    }
                }else{
                    Log.i("DEBUG","DELETE MIDDLE PICTURE");
                    ivPreview.setVisibility(View.GONE);
                    TextView tvPreview = findViewById(R.id.tvPreview);
                    tvPreview.setVisibility(View.GONE);
                    delete.setVisibility(View.GONE);
                }
            }
        }

        Log.i("DEBUG", "-2 completed");

        //To change to previous image
        if (change == -1){
            Log.i("DEBUG", "decrease");
            if (totalPreviewImages == 0) {return;}
            if (currentPreviewImage == 0 && photoExists){
                ivPreview.setImageBitmap(photo);
                currentPreviewImage = -1;
            }else if(currentPreviewImage == 0 && !photoExists){
                currentPreviewImage = totalPreviewImages - 1;
                ivPreview.setImageURI(Uri.parse(galleryPhotoList.get(currentPreviewImage)));

            }else if (currentPreviewImage == -1){
                Log.i("DEBUG", "currentPreviewImage == -1");
                if (totalPreviewImages > 1){
                    Log.i("DEBUG", "totalPreviewImages > 1");
                    currentPreviewImage = totalPreviewImages - 2;
                    Log.i("DEBUG", "Total - 2: "+(currentPreviewImage));
                    ivPreview.setImageURI(Uri.parse(galleryPhotoList.get(currentPreviewImage)));

                }
            }else{
                ivPreview.setImageURI(Uri.parse(galleryPhotoList.get(currentPreviewImage - 1)));
                Log.i("DEBUG", "tester: current-1 " + (currentPreviewImage-1));
                Log.i("DEBUG", "URI STRING: "+ galleryPhotoList.get(currentPreviewImage - 1));
                currentPreviewImage--;
            }
        }

        //To change to next image
        if (change == 1){
            Log.i("DEBUG", "increase");
            if (totalPreviewImages == 0) {return;}
            if (currentPreviewImage == (totalPreviewImages - 2) && photoExists){
                ivPreview.setImageBitmap(photo);
                currentPreviewImage = -1;
            }else if(currentPreviewImage == (totalPreviewImages - 1) && !photoExists){
                currentPreviewImage = 0;
                ivPreview.setImageURI(Uri.parse(galleryPhotoList.get(currentPreviewImage)));
            }else if (currentPreviewImage == -1){
                Log.i("DEBUG", "currentPreviewImage == -1");
                if (totalPreviewImages > 1){
                    Log.i("DEBUG", "totalPreviewImages > 1");
                    currentPreviewImage = 0;
                    ivPreview.setImageURI(Uri.parse(galleryPhotoList.get(currentPreviewImage)));
                }
            }else{
                currentPreviewImage++;
                ivPreview.setImageURI(Uri.parse(galleryPhotoList.get(currentPreviewImage)));
                Log.i("DEBUG", "tester: current+1 " + (currentPreviewImage-1));
                Log.i("DEBUG", "URI STRING: "+ galleryPhotoList.get(currentPreviewImage));

            }

        }

        Log.i("DEBUG", "array length "+galleryPhotoList.size());

        Log.i("DEBUG", "Exit: current " + currentPreviewImage);
        Log.i("DEBUG", "Exit: total " + totalPreviewImages);

        changePreviewText();

    }

    //Methid for setting the prevuew text under image
    public void changePreviewText(){
        if (totalPreviewImages > 0){
            TextView tvPreview = findViewById(R.id.tvPreview);
            if (photoExists){
                tvPreview.setText("Photo: "+(currentPreviewImage+2) + "/"+totalPreviewImages);
            }else{
                tvPreview.setText("Photo: "+(currentPreviewImage+1) + "/"+totalPreviewImages);
            }
        }
    }

    //Method for saving Phlog
    public void save(boolean isSave){
        Intent returnIntent = new Intent();

        //make sure title exists
        EditText title = findViewById(R.id.etTitle);
        if (title.getText().toString().equals("")){
            Toast.makeText(PhlogData.this, "Please add title", Toast.LENGTH_SHORT).show();
            return;
        }

        //Open DataRoom
        DataRoomDB phlogDB = Room.databaseBuilder(getApplicationContext(), DataRoomDB.class,
                DATABASE_NAME).allowMainThreadQueries().build();
        //Create New Entity
        DataRoomEntity toAdd = new DataRoomEntity();

        //If is edit set ID
        if (isEdit){
            toAdd.setId(phlogList.get(position).getId());
        }

        //if is Delete, remove Phlog and return
        if (!isSave){
            setResult(RESULT_OK, returnIntent);
            Log.i("DEBUG","DELETED");
            phlogDB.daoAccess().deletePhlog(toAdd);
            phlogDB.close();
            finish();
            return;
        }

        EditText text = findViewById(R.id.etText);

        toAdd.setTitle(title.getText().toString());
        toAdd.setText(text.getText().toString());
        toAdd.setDateTime(System.currentTimeMillis() / 1000);
        
        if (photoExists){

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            photo.compress(Bitmap.CompressFormat.PNG, 0 /* Ignored for PNGs */, byteArrayOutputStream);
            byte[] bitmapdata = byteArrayOutputStream.toByteArray();

            toAdd.setPhoto(bitmapdata);

            toAdd.setOrientation(orientation[0]+" "+orientation[1]+" "+orientation[2]);
        }

        try {
            LatLng position = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

            toAdd.setLocation(position.toString());
        }catch(Exception e){
            Toast.makeText(PhlogData.this, "Could not determine position", Toast.LENGTH_SHORT).show();
        }



        Log.i("DEBUG", galleryPhotoList.toString());

        toAdd.setGalleryPictures(galleryPhotoList.toString());

        Log.i("DEBUG", "SAVED: "+galleryPhotoList.toString());
        if (recordingOngoing){
            recorder.stop();
        }
        recorder.release();

        if (recordingExist){
            try {
                byte[] recording = convert(recordFileName);
                toAdd.setRecording(recording);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //if is edit, then edit phlog, otherwise make new
        if (isEdit){
            phlogDB.daoAccess().updatePhlog(toAdd);
        }else{
            phlogDB.daoAccess().addPhlog(toAdd);
        }

        //Close data base
        phlogDB.close();
        setResult(RESULT_OK, returnIntent);
        Log.i("DEBUG", ""+galleryPhotoList);
        finish();
    }

    //Method for opening camera
    public void camera(){
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_PHOTO_RESULT_CODE);

        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        startSensor(Sensor.TYPE_MAGNETIC_FIELD);
        startSensor(Sensor.TYPE_ACCELEROMETER);
    }

    //Camera result Launcher
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_PHOTO_RESULT_CODE) {
            sensorManager.unregisterListener((SensorEventListener) PhlogData.this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
            sensorManager.unregisterListener((SensorEventListener) PhlogData.this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
        }

        if (resultCode == Activity.RESULT_OK){
            if (requestCode == CAMERA_PHOTO_RESULT_CODE){
                photo = (Bitmap) data.getExtras().get("data");

                if (!photoExists){
                    photoExists = true;
                    viewer(0);
                }


                if (totalPreviewImages > 0){
                    currentPreviewImage = - 1;
                    ivPreview.setImageBitmap(photo);
                    changePreviewText();
                }
            }
        }

    }

    //Click Listener
    public void onClickListener(View view){
        switch (view.getId()){
            case R.id.btnTakePhoto:
                camera();
                break;
            case R.id.btnGalleryPhoto:
                openGallery.launch("image/*");
                break;

            case R.id.btnVoiceRecording:
                recordingExist = true;
                Button btnRecord = findViewById(R.id.btnVoiceRecording);
                if (!recordingOngoing){
                    Log.i("DEBUG","START RECORDING");
                    recorder.start();
                    recordingOngoing = true;
                    btnRecord.setText("Stop Recording");
                }else{
                    recorder.stop();
                    recordingOngoing = false;
                    btnRecord.setText("Start Recording");
                }

                break;
            case R.id.btnSave:
                save(true);
                break;

            case R.id.btnPrevious:
                if (totalPreviewImages > 0){
                    viewer(-1);
                }
                break;
            case R.id.btnNext:
                if (totalPreviewImages > 0){
                    viewer(+1);
                }
                break;
            case R.id.btnDeleteImage:
                viewer(-2);
                break;
            case R.id.btnDeletePhlog:
                save(false);
                break;

        }
    }

    //Galery Result Launcher
    ActivityResultLauncher<String> openGallery = registerForActivityResult(
            new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri result) {
                    if (result != null){
                        try {
                            galleryPhotoList.add(getPath(PhlogData.this, result));
                            Log.i("DEBUG", getPath(PhlogData.this, result));
                            viewer(0);
                            if (totalPreviewImages > 0){
                                if (photoExists){
                                    currentPreviewImage = totalPreviewImages - 2;
                                    Log.i("DEBUG","current: "+currentPreviewImage);
                                    Log.i("DEBUG","total: "+totalPreviewImages);
                                }else{
                                    currentPreviewImage = totalPreviewImages - 1;
                                }
                                ivPreview.setImageURI(Uri.parse(galleryPhotoList.get(currentPreviewImage)));

                                changePreviewText();
                            }
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                        }
                        Log.i("DEBUG","5");


                    }
                }
            }
    );

    //Method for getting absolute path from uri
    public static String getPath(Context context, Uri uri) throws URISyntaxException {
        final String[] split = DocumentsContract.getDocumentId(uri).split(":");

        uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        String selection = "_id=?";
        String[] selectionArgs = new String[]{ split[1] };
        Log.i("DEBUG", ""+selectionArgs);
        
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            if (cursor.moveToFirst()) {
                return cursor.getString(column_index);
            }
        } catch (Exception e) {
        }

        return null;
    }

    public byte[] convert(String path) throws IOException {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        FileInputStream fileInputStream = new FileInputStream(path);
        byte[] b = new byte[1024];

        for (int readNum; (readNum = fileInputStream.read(b)) != -1;) {
            byteArrayOutputStream.write(b, 0, readNum);
        }

        byte[] bytes = byteArrayOutputStream.toByteArray();

        return bytes;
    }

    //Sensor Stuff
    private boolean startSensor(int sensorType) {

        if (sensorManager.getSensorList(sensorType).isEmpty()) {
            return(false);
        } else {
            sensorManager.registerListener((SensorEventListener) PhlogData.this,sensorManager.getDefaultSensor(sensorType),
                    SensorManager.SENSOR_DELAY_NORMAL);
            return(true);
        }
    }

    public void onSensorChanged(SensorEvent event) {

        boolean gravityChanged,magneticFieldChanged,orientationChanged;
        float R[] = new float[9];
        float I[] = new float[9];
        float newOrientation[] = new float[3];

        gravityChanged = magneticFieldChanged = orientationChanged = false;
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                gravityChanged = arrayCopyChangeTest(event.values,gravity, 1);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                magneticFieldChanged = arrayCopyChangeTest(event.values,magneticField, 1);
                break;
            default:
                break;
        }

        if ((gravityChanged || magneticFieldChanged) &&
                SensorManager.getRotationMatrix(R,I,gravity,magneticField)) {
            SensorManager.getOrientation(R,newOrientation);
            newOrientation[0] = (float)Math.toDegrees(newOrientation[0]);
            newOrientation[1] = (float)Math.toDegrees(newOrientation[1]);
            newOrientation[2] = (float)Math.toDegrees(newOrientation[2]);
            orientationChanged = arrayCopyChangeTest(newOrientation,orientation,1);
            Log.i("DEBUG", ""+orientation[0]+" "+orientation[1]+" "+orientation[2]);
        }

    }

    private boolean arrayCopyChangeTest(float[] from,float[] to,float amountForChange) {

        int copyIndex;
        boolean changed = false;

        for (copyIndex=0;copyIndex < to.length;copyIndex++) {
            if (Math.abs(from[copyIndex] - to[copyIndex]) > amountForChange) {
                changed = true;
            }
        }
        if (changed) {
            for (copyIndex = 0; copyIndex < to.length; copyIndex++) {
                to[copyIndex] = from[copyIndex];
            }
        }
        return(changed);
    }

    private void startLocating(int accuracy) {

        locationRequest.setPriority(accuracy);
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    myLocationCallback, Looper.myLooper());


        } catch (SecurityException e) {
            Toast.makeText(this,"Permission denied",Toast.LENGTH_SHORT).show();
        }
    }

    LocationCallback myLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {

            onLocationChanged(locationResult.getLastLocation());

        }
    };

    public void onLocationChanged(Location newLocation) {
        currentLocation = newLocation;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        recorder.release();
        fusedLocationClient.removeLocationUpdates(myLocationCallback);
    }
}