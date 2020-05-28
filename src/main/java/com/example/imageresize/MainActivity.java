package com.example.imageresize;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS = 1;

    private static final int REQUEST_CODE_CAPTURE_IMAGE = 2;

    private String currentImagePath;

    private ImageView imageSmall, imageOriginal;

    OutputStream outputStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageSmall = findViewById(R.id.capturedImageSmall);
        imageOriginal = findViewById(R.id.capturedImageOriginal);

        //on apps that target Android 6.0 (API level 23) or higher,
        //we need to check for and request permissions at runtime.
        findViewById(R.id.buttonCaptureImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(
                        getApplicationContext(),
                        Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(
                                getApplicationContext(),
                                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{
                                    Manifest.permission.CAMERA,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                            },
                            REQUEST_CODE_PERMISSIONS);
                } else {
                    dispatchCaptureImageIntent();
                }
            }
        });
    }

    private void dispatchCaptureImageIntent() {

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(intent.resolveActivity(getPackageManager()) != null){
            File imageFile = null;
            try {
                imageFile = createImageFile();
            } catch (IOException exception){
                Toast.makeText(this, exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
            if(imageFile != null){
                Uri imageUri = FileProvider.getUriForFile(this, "com.example.imageresize.fileprovider",imageFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                //startActivityForResult() method, we can send information from one activity to another and vice-versa. The android startActivityForResult method, requires a result from the second activity (activity to be invoked).
                startActivityForResult(intent, REQUEST_CODE_CAPTURE_IMAGE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String fileName = "IMAGE_" + new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault()).format(new Date());
        File directory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(
                fileName,
                ".jpg",
                directory
        );
        currentImagePath = imageFile.getAbsolutePath();
        return imageFile;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CODE_PERMISSIONS && grantResults.length > 0) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED){
                dispatchCaptureImageIntent();
            } else {
                Toast.makeText(this, "Not all permissions granted", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {


        if(requestCode == REQUEST_CODE_CAPTURE_IMAGE && resultCode == RESULT_OK){

            try{
                BitmapDrawable drawable = (BitmapDrawable) imageSmall.getDrawable();
                Bitmap bitmap = drawable.getBitmap();

                // Display small image
                imageSmall.setImageBitmap(getScaledBitmap(imageSmall));

                //Display Original image
                imageOriginal.setImageBitmap(BitmapFactory.decodeFile(currentImagePath));

                File capturedImageFile = new File(currentImagePath);
                try{
                    outputStream = new FileOutputStream(capturedImageFile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                Toast.makeText(getApplicationContext(), "Image saved!", Toast.LENGTH_SHORT).show();
                outputStream.flush();
                outputStream.close();



            }catch (Exception exception){
                Toast.makeText(this, exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private Bitmap getScaledBitmap(ImageView imageView) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        int scaledFactor = Math.min(
                options.outWidth / imageView.getWidth(),
                options.outHeight / imageView.getHeight()
        );

        options.inJustDecodeBounds = false;
        options.inSampleSize = scaledFactor;
        options.inPurgeable = true;

        return BitmapFactory.decodeFile(currentImagePath, options);
    }
}
