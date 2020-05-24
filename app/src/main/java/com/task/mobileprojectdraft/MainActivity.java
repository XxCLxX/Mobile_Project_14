package com.task.mobileprojectdraft;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Picture;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    private static final int REQUEST_PERMISSIONS = 2;
    private static final String[] PERMISSIONS ={
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static final int PERMISSIONS_COUNT = 2;
    @SuppressLint("NewApi")
    private boolean notPermissions(){
        for(int i = 0; i<PERMISSIONS_COUNT; i++){
            if(checkSelfPermission(PERMISSIONS[i]) != PackageManager.PERMISSION_GRANTED){
                return true;
            }
        }
        return false;
    }

    /*static{
        System.loadLibrary("photoEditor");
    }*/

    @Override
    protected void onResume(){
        super.onResume();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notPermissions()){
           requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_PERMISSIONS && grantResults.length > 0){
          if(notPermissions()){
              ((ActivityManager) this.getSystemService(ACTIVITY_SERVICE)).clearApplicationUserData();
              recreate();
          }
        }
    }

    public void onBackPressed(){
        if(editMode){
            findViewById(R.id.editScreen).setVisibility(View.GONE);
            findViewById(R.id.welcomeScreen).setVisibility(View.VISIBLE);
            editMode = false;
        }else{
            super.onBackPressed();
        }
    }

    private static final int REQUEST_PICK_PHOTO = 12345;
    private ImageView imageView;

    private void init() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
        }

        imageView = findViewById(R.id.imageView);

        if (!MainActivity.this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            findViewById(R.id.takePhotoButton).setVisibility(View.GONE);
        }

        //Select image button
        final Button selectPhotoButton = findViewById(R.id.selectPhotoButton);
        selectPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                final Intent pickIntent = new Intent(Intent.ACTION_PICK);
                pickIntent.setDataAndType(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                final Intent chooserIntent = Intent.createChooser(intent, "Select Photo");
                startActivityForResult(chooserIntent, REQUEST_PICK_PHOTO);

            }
        });

        //Take photo with camera
        final Button takePhotoButton = findViewById(R.id.takePhotoButton);
        takePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePhotoIntent.resolveActivity(getPackageManager()) != null) {
                    //create a file for the photo that was taken
                    final File photoFile = createImageFile();
                    imageUri = Uri.fromFile(photoFile);
                    final SharedPreferences myPrefs = getSharedPreferences(appID, 0);
                    myPrefs.edit().putString("path", photoFile.getAbsolutePath()).apply();
                    takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                    startActivityForResult(takePhotoIntent, REQUEST_IMAGE_CAPTURE);
                } else {
                    Toast.makeText(MainActivity.this, "Camera is not compatible",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        //Rotation Button
        final Button rotateButton = findViewById(R.id.rotate);
        rotateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread() {
                    @Override
                    public void run() {
                        final Bitmap newBitmap = Bitmap.createBitmap(width, height, bitmap.getConfig());
                        double x0 = 0.5 * (width - 1);
                        double y0 = 0.5 * (height - 1);

                        int sum = (int) (y0 + x0);
                        int difference = (int) (y0 - x0);

                        int right = Math.min(width, height - difference);
                        int bottom = Math.min(height, sum + 1);

                        for (int x = Math.max(0, -difference); x < right; x++) {
                            for (int y = Math.max(0, sum - width + 1); y < bottom; y++) {
                                int xx = sum - y;
                                int yy = difference + x;
                                newBitmap.setPixel(x,y, bitmap.getPixel(xx, yy));
                            }
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                imageView.setImageBitmap(newBitmap);
                            }
                        });
                    }
                }.start();
            }
        });
        /*final Button rotateButton = findViewById(R.id.rotate);
        rotateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread() {
                    @Override
                    public void run(){
                        double degree = 90;

                        double angle = Math.toRadians(degree);
                        double sin = Math.sin(angle);
                        double cos = Math.cos(angle);
                        double x0 = 0.5 * (width - 1);
                        double y0 = 0.5 * (height - 1);

                        for (int x = 0; x < width; x++) {
                            for (int y = 0; y < height; y++) {
                                double a = x - x0;
                                double b = y - y0;
                                int xx = (int) (+a * cos - b * sin + x0);
                                int yy = (int) (+a * sin + b * cos + y0);

                                if (xx >= 0 && xx < width && yy >= 0 && yy < height) {
                                   newBitmap.setPixel(x, y, bitmap.getPixel(xx, yy));
                                }
                            }
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                imageView.setImageBitmap(newBitmap);
                            }
                        });
                    }
                }.start();
            }
        });*/

        //Greyscale Button
        Button greyscaleButton = findViewById(R.id.greyscale);
        greyscaleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(){
                    public void run(){
                        for(int i = 0; i < bitmap.getWidth(); i++){
                            for(int j = 0; j < bitmap.getHeight(); j++) {
                                int pix = bitmap.getPixel(i, j);
                                int R = Color.red(pix);
                                int G = Color.green(pix);
                                int B = Color.blue(pix);

                                R = G = B = (int)(0.299 * R + 0.587 * G + 0.114 * B);

                                bitmap.setPixel(i, j, Color.argb(Color.alpha(pix), R, G, B));
                            }
                        }
                        runOnUiThread(new Runnable(){
                            @Override
                            public void run(){
                                imageView.setImageBitmap(bitmap);
                            }
                        });
                    }
                }.start();
            }
        });

        //Invert Button
        Button invertButton = findViewById(R.id.invert);
        invertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(){
                    public void run(){
                        int A, R, G, B;

                        for(int x = 0; x < width; x++)
                        {
                            for(int y = 0; y < height; y++)
                            {
                                int pix = bitmap.getPixel(x, y);
                                A = Color.alpha(pix);
                                R = 255 - Color.red(pix);
                                G = 255 - Color.green(pix);
                                B = 255 - Color.blue(pix);
                                bitmap.setPixel(x,y,Color.argb(A,R,G,B));
                            }
                        }
                        runOnUiThread(new Runnable(){
                            @Override
                            public void run(){
                                imageView.setImageBitmap(bitmap);
                            }
                        });
                    }
                }.start();
            }
        });

        //Save Button
        final Button saveImageButton = findViewById(R.id.saveImage);
        saveImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                final DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            final File outFile = createImageFile();
                            try (FileOutputStream out = new FileOutputStream(outFile)) {
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                                imageUri = Uri.parse("file://" + outFile.getAbsolutePath());
                                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, imageUri));
                                Toast.makeText(MainActivity.this, "Image Saved", Toast.LENGTH_SHORT).show();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                };
                builder.setMessage("Save current photo to Gallery?").
                        setPositiveButton("Yes", dialogClickListener).
                        setNegativeButton("No", dialogClickListener).show();
            }
        });

        //Back Button
        final Button backButton = findViewById(R.id.back);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.editScreen).setVisibility(View.GONE);
                findViewById(R.id.welcomeScreen).setVisibility(View.VISIBLE);
                editMode = false;
            }
        });
    }
//------------------------------------------------------------------------------------

    private static final int REQUEST_IMAGE_CAPTURE = 1346;
    private static final String appID = "mobileProject";
    private Uri imageUri;


    private File createImageFile(){
        @SuppressLint("SimpleDateFormat") final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date());
        final String imageFileName = "/JPEG_"+timeStamp + ".jpg";
        final File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return new File(storageDir + imageFileName);
    }


    private boolean editMode=false;
    private Bitmap bitmap;
    private int width=0;
    private int height=0;
    private static final int MAX_PIXEL_COUNT = 2048;

    private int[] pixels; //store pixel of the image
    private int pixelCount = 0; //How many pixels an image has


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode != RESULT_OK){
            return;
        }
        if(requestCode == REQUEST_IMAGE_CAPTURE){
            if(imageUri == null){
                final SharedPreferences p = getSharedPreferences(appID, 0);
                final String path = p.getString("path", "");
                if(path.length() < 1){
                    recreate();
                    return;
                }
                imageUri = Uri.parse("file://" + path);
            }
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, imageUri));
        }else if(data == null){
            recreate();
            return;
        }else if(requestCode == REQUEST_PICK_PHOTO){
            imageUri = data.getData();
        }
        final ProgressDialog dialog = ProgressDialog.show(MainActivity. this, "Loading", "Please Wait", true);
        editMode = true;
        findViewById(R.id.welcomeScreen).setVisibility(View.GONE);
        //findViewById(R.id.editScreen).setVisibility(View.VISIBLE);


        new Thread(){
            public void run() {
                bitmap = null;
                final BitmapFactory.Options bmpOptions = new BitmapFactory.Options();
                bmpOptions.inBitmap = bitmap;
                bmpOptions.inJustDecodeBounds = true;
                try (InputStream input = getContentResolver().openInputStream(imageUri)) {
                    bitmap = BitmapFactory.decodeStream(input, null, bmpOptions);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                bmpOptions.inJustDecodeBounds = false;
                width = bmpOptions.outWidth;
                height = bmpOptions.outHeight;
                int resizeScale = 1;
                if (width > MAX_PIXEL_COUNT) {
                    resizeScale = width/MAX_PIXEL_COUNT;
                } else if (height > MAX_PIXEL_COUNT) {
                    resizeScale = height/MAX_PIXEL_COUNT;
                }
                if (width/resizeScale > MAX_PIXEL_COUNT || height/resizeScale > MAX_PIXEL_COUNT) {
                    resizeScale++;
                }
                bmpOptions.inSampleSize = resizeScale;
                InputStream input = null;
                try {
                    input = getContentResolver().openInputStream(imageUri);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    recreate();
                    return;
                }
                bitmap = BitmapFactory.decodeStream(input, null, bmpOptions);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImageBitmap(bitmap);
                        dialog.cancel();
                    }
                }); //after image has been loaded, update width & height values
                width = bitmap.getWidth();
                height = bitmap.getHeight();
                bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

                pixelCount = width * height;
                pixels = new int[pixelCount];
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            }
        }.start();
    }
}
