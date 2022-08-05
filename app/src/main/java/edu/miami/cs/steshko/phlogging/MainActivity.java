package edu.miami.cs.steshko.phlogging;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.miami.cs.steshko.phlogging.db.DataRoomDB;
import edu.miami.cs.steshko.phlogging.db.DataRoomEntity;

public class MainActivity extends AppCompatActivity implements UIDialogFragment.EditButton {
    private List<DataRoomEntity> phlogList;
    private DataRoomDB phlogDB;
    private MyListAdapter listAdapter;
    private static final String DATABASE_NAME = "Phlog.db";
    private int toEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        phlogDB = Room.databaseBuilder(getApplicationContext(),DataRoomDB.class,
                DATABASE_NAME).allowMainThreadQueries().build();

        //Get Permissions
        getPermissions.launch(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CAMERA});
    }

    private void goOnCreating() {
        setContentView(R.layout.activity_main);
        //phlogList = phlogDB.daoAccess().getList();

        showList();

    }

    //If Edit Button Selected in Dialog
    public void editButton(){
        Log.i("DEBUG", "Edit Button");
        Intent phlogEntryIntent = new Intent(MainActivity.this, PhlogData.class);
        //Set edit to take data from database in editor
        phlogEntryIntent.putExtra("edu.miami.cs.steshko.phlogging/edit",true);
        phlogEntryIntent.putExtra("edu.miami.cs.steshko.phlogging/position",toEdit);
        //Launch Editor
        phlogEntryActivityLauncher.launch(phlogEntryIntent);
    }
    //Show/Update ListView
    public void showList(){
        //Get new database list
        phlogList = phlogDB.daoAccess().getList();
        //Show list
        listAdapter = makeListAdapter();
        ListView listView = findViewById(R.id.listView);
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(this::onItemClick);

    }
    //Make adapter for List
    private MyListAdapter makeListAdapter(){
        ArrayList<HashMap<String,String>> listItems;
        HashMap<String,String> oneItem;
        //Pass Image and Text
        String[] fromHashMapFieldNames = {"title","date","text"};
        int[] toGridFieldIds = {R.id.tvListTitle, R.id.tvListDate, R.id.tvListText};

        listItems = new ArrayList<>();
        for (int i = 0; i < phlogList.size(); i++) {
            oneItem = new HashMap<>();
            oneItem.put("title", phlogList.get(i).getTitle());
            oneItem.put("date", getDateTimeString(phlogList.get(i).getDateTime()));
            oneItem.put("text", phlogList.get(i).getText());

            listItems.add(oneItem);
        }
        return(new MyListAdapter(this,listItems, R.layout.list_layout, fromHashMapFieldNames, toGridFieldIds));
    }

    //Convert long Date to String
    public String getDateTimeString(long dateTime){
        long dv = dateTime*1000;// its need to be in milisecond
        Date df = new java.util.Date(dv);
        String vv = new SimpleDateFormat("MM dd, yyyy hh:mma").format(df);
        return vv;
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long rowId){
        toEdit = position;

        //Build Dialog
        UIDialogFragment myDialogFragment = new UIDialogFragment();
        Bundle bundleToFragment = new Bundle();
        bundleToFragment.putString("title", phlogList.get(position).getTitle());
        if (phlogList.get(position).getText() != null){
            bundleToFragment.putString("text", phlogList.get(position).getText());
            bundleToFragment.putBoolean("textExist", true);
        }

        bundleToFragment.putLong("dateTime", phlogList.get(position).getDateTime());
        bundleToFragment.putString("location", phlogList.get(position).getLocation());

        //Photo Selected is what photos exist
        //0 is no photo
        //1 is only camera photo
        //2 is only gallery photos
        //3 is both
        int photosSelected = 0;
        if (phlogList.get(position).getPhoto() != null){
            Log.i("DEBUG", "Photo Exists");
            bundleToFragment.putByteArray("photo", phlogList.get(position).getPhoto());
            bundleToFragment.putString("orientation", phlogList.get(position).getOrientation());
            photosSelected = photosSelected + 1;
        }

        if (!phlogList.get(position).getGalleryPictures().equals("[]")){
            bundleToFragment.putString("galleryPictures", phlogList.get(position).getGalleryPictures());
            photosSelected = photosSelected + 2;
        }
        bundleToFragment.putInt("photosSelected", photosSelected);


        if (phlogList.get(position).getRecording() != null){
            bundleToFragment.putBoolean("recording", true);
        }else{
            bundleToFragment.putBoolean("recording", false);
        }

        //Start Dialog
        myDialogFragment.setArguments(bundleToFragment);
        myDialogFragment.show(getSupportFragmentManager(),"my_fragment");
    }




    public void myClickHandler(View view){
        //Start editor
        Intent phlogEntryIntent = new Intent(MainActivity.this, PhlogData.class);
        phlogEntryIntent.putExtra("edu.miami.cs.steshko.phlogging/edit",false);
        phlogEntryActivityLauncher.launch(phlogEntryIntent);
    }






    ActivityResultLauncher<Intent> phlogEntryActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK){
                        //if DataBase updated, update List
                        showList();
                    }

                }
            }
    );






    private ActivityResultLauncher<String[]> getPermissions = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            new ActivityResultCallback<Map<String, Boolean>>() {
                @Override
                public void onActivityResult(Map<String, Boolean> results) {

                    for (String key:results.keySet()) {
                        if (!results.get(key)) {
                            Toast.makeText(MainActivity.this,"Need permission",Toast.LENGTH_LONG).show();
                            finish();
                        }
                    }
                    goOnCreating();
                }
            });
}