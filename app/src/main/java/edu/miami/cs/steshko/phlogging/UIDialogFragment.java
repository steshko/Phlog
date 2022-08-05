package edu.miami.cs.steshko.phlogging;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UIDialogFragment extends DialogFragment {

    public String[] galleryPhotoList;
    public boolean photoExist;
    public int currentPosition;
    public Bitmap bitmap;
    String orientation;
    public ImageView dialogPreview;
    public TextView tvPreview;
    public TextView tvOrientation;

    public Dialog onCreateDialog(Bundle savedInstanceState){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());

        String title = this.getArguments().getString("title");

        long dateTime = this.getArguments().getLong("dateTime");
        String location = this.getArguments().getString("location");

        int photosSelected = this.getArguments().getInt("photosSelected");

        dialogBuilder.setTitle(title);
        dialogBuilder.setNegativeButton("Edit", editListener);
        dialogBuilder.setPositiveButton("dismiss", dismissListener);

        //set View
        LayoutInflater inflater = LayoutInflater.from(getContext());
        final View dialogContext = inflater.inflate(R.layout.dialog_view, null);
        //ImageView imageToShow = dialogContext.findViewById(R.id.dialog_image);

        Boolean textExist = this.getArguments().getBoolean("textExist");
        if (textExist){
            String text = this.getArguments().getString("text");
            TextView dialogText = dialogContext.findViewById(R.id.tvDialogText);
            dialogText.setText(text);
        }

        dialogPreview = dialogContext.findViewById(R.id.ivDialogPreview);
        tvPreview = dialogContext.findViewById(R.id.tvDialogPreview);
        tvOrientation = dialogContext.findViewById(R.id.tvOrientation);

        if (photosSelected == 1){
            dialogPreview.setVisibility(View.VISIBLE);

            getAndSetBitmap();
            tvPreview.setVisibility(View.VISIBLE);
            tvPreview.setText("Photo: 1/1");
        }else if(photosSelected == 2){
            Log.i("DEBUG", "photos selected == 2");
            inputGalleyPhotoList();
            photoExist = false;

            Log.i("DEBUG", ""+galleryPhotoList[0]);
            dialogPreview.setImageURI(Uri.parse(galleryPhotoList[0]));
            dialogPreview.setVisibility(View.VISIBLE);
            currentPosition = 0;
            if (galleryPhotoList.length > 1){
                setBtnsVisible(dialogContext);
            }
            changePreviewText();

        }else if (photosSelected == 3){
            inputGalleyPhotoList();
            photoExist = true;
            setBtnsVisible(dialogContext);

            currentPosition = -1;

            getAndSetBitmap();
            changePreviewText();
        }


        ImageView ivPrevious = dialogContext.findViewById(R.id.ivDialogPrevious);
        ivPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cycler(-1);
            }
        });

        ImageView ivNext = dialogContext.findViewById(R.id.ivDialogNext);
        ivNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cycler(1);
            }
        });

        TextView tvDateTime = dialogContext.findViewById(R.id.tvDialogDateTime);
        tvDateTime.setText(getDateTimeString(dateTime));

        TextView tvLocationLatLng = dialogContext.findViewById(R.id.tvLocationLatLng);
        tvLocationLatLng.setText(location);




        dialogBuilder.setView(dialogContext);

        return (dialogBuilder.create());
    }

    public void cycler(int change){
        if (change == -1){
            if (currentPosition == -1){
                currentPosition = galleryPhotoList.length - 1;
                dialogPreview.setImageURI(Uri.parse(galleryPhotoList[currentPosition]));
            }else if(currentPosition == 0 && photoExist){
                currentPosition = -1;
                dialogPreview.setImageBitmap(bitmap);
            }else if (currentPosition == 0 && !photoExist){
                currentPosition = galleryPhotoList.length - 1;
                dialogPreview.setImageURI(Uri.parse(galleryPhotoList[currentPosition]));
            }else{
                currentPosition = currentPosition - 1;
                dialogPreview.setImageURI(Uri.parse(galleryPhotoList[currentPosition]));
            }
        }

        if (change == 1){
            if (currentPosition == galleryPhotoList.length - 1 && photoExist){
                currentPosition = -1;
                dialogPreview.setImageBitmap(bitmap);;
            }else if(currentPosition == galleryPhotoList.length - 1 && !photoExist){
                currentPosition = 0;
                dialogPreview.setImageURI(Uri.parse(galleryPhotoList[currentPosition]));
            }else{
                currentPosition = currentPosition + 1;
                dialogPreview.setImageURI(Uri.parse(galleryPhotoList[currentPosition]));
            }
        }
        changePreviewText();
    }

    public void changePreviewText(){
        tvPreview.setVisibility(View.VISIBLE);
        if (currentPosition == -1 && !(tvOrientation.getVisibility() == View.VISIBLE)){
            tvOrientation.setVisibility(View.VISIBLE);
            tvOrientation.setText("Orientation when Photo was Taken: "+orientation);
        }else if(currentPosition != -1 && tvOrientation.getVisibility() == View.VISIBLE){
            tvOrientation.setVisibility(View.INVISIBLE);
        }

        if (photoExist){
            tvPreview.setText("Photo: "+(currentPosition+2) + "/"+(galleryPhotoList.length + 1));
        }else{
            tvPreview.setText("Photo: "+(currentPosition+1) + "/"+galleryPhotoList.length);
        }
    }

    public void getAndSetBitmap(){
        byte[] photo = this.getArguments().getByteArray("photo");
        orientation = this.getArguments().getString("orientation");
        tvOrientation.setVisibility(View.VISIBLE);
        tvOrientation.setText("Orientation when Photo was Taken: "+orientation);

        bitmap = BitmapFactory.decodeByteArray(photo, 0, photo.length);
        dialogPreview.setImageBitmap(bitmap);
    }

    public void setBtnsVisible(View dialogContext){
        dialogPreview.setVisibility(View.VISIBLE);
        ImageView ivPrevious = dialogContext.findViewById(R.id.ivDialogPrevious);
        ImageView ivNext = dialogContext.findViewById(R.id.ivDialogNext);
        ivPrevious.setVisibility(View.VISIBLE);
        ivNext.setVisibility(View.VISIBLE);
    }

    public void inputGalleyPhotoList(){
        String galleryPhotos = this.getArguments().getString("galleryPictures");
        galleryPhotos = galleryPhotos.substring(1, galleryPhotos.length() - 1);

        galleryPhotoList = galleryPhotos.split(", ");
    }

    public interface EditButton{
        public void editButton();
    }

    private final DialogInterface.OnClickListener dismissListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            dismiss();
        }
    };

    private final DialogInterface.OnClickListener editListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            EditButton mainActivity;
            mainActivity = (EditButton) getActivity();

            mainActivity.editButton();
            dismiss();
        }
    };


    public String getDateTimeString(long dateTime){
        long dv = dateTime*1000;// its need to be in milisecond
        Date df = new java.util.Date(dv);
        String vv = new SimpleDateFormat("MM dd, yyyy hh:mma").format(df);
        return vv;
    }
}
