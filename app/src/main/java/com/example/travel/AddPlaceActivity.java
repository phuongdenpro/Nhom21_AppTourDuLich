package com.example.travel;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.os.Handler;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

public class AddPlaceActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private Button btnAdd;
    private EditText edName, edDes, edLocal,edPrice;
    private ProgressBar mProgressBar;
    private ImageView imageView;
    private FirebaseAuth mAuth;
    private DatabaseReference myReference;
    private FirebaseDatabase database;
    private Context context;
    private FirebaseStorage ref;
    private StorageReference storageReference;
    private Uri imgUri;
    private StorageTask mUploadTask;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addnew);

        btnAdd = findViewById(R.id.addNew);
        edName = findViewById(R.id.yourNamePlace);
        edDes = findViewById(R.id.descriptivePlace);
        edLocal = findViewById(R.id.locatedAt);
        edPrice = findViewById(R.id.price);
        imageView = findViewById(R.id.imgAdd);
        mProgressBar = findViewById(R.id.progress_bar);
        mProgressBar.setVisibility(View.INVISIBLE);
        storageReference = FirebaseStorage.getInstance().getReference("tours");
        myReference = FirebaseDatabase.getInstance().getReference();


        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openFileChooser();
            }
        });
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mUploadTask != null && mUploadTask.isInProgress()) {
                    Toast.makeText(AddPlaceActivity.this, "Upload in progress", Toast.LENGTH_SHORT).show();
                } else {
                    if(checkData()){
                        uploadFile();
                    }

                }
            }
        });
    }
    private void openFileChooser(){
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent,PICK_IMAGE_REQUEST);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            imgUri = data.getData();
            Picasso.get().load(imgUri).fit().centerCrop().into(imageView);
        }
    }
    private String getFileExtension(Uri uri) {
        ContentResolver cR = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cR.getType(uri));
    }
    private void uploadFile() {
        if (imgUri != null) {
            StorageReference fileReference = storageReference.child(System.currentTimeMillis()
                    + "." + getFileExtension(imgUri));

            mUploadTask = fileReference.putFile(imgUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mProgressBar.setProgress(0);
                                }
                            }, 500);


                            fileReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    String url = uri.toString();
                                    Tour tour = new Tour(edName.getText().toString().trim(), edDes.getText().toString().trim(),
                                            edLocal.getText().toString().trim(), Integer.parseInt(edPrice.getText().toString().trim()),
                                            url);
                                    String tourId = myReference.push().getKey();
                                    tour.setTourId(tourId);
                                    myReference.child("tours").child(tourId).setValue(tour).addOnCompleteListener(task -> {
                                        if(task.isSuccessful()){
                                            edName.setText("");
                                            edName.requestFocus();
                                            edDes.setText("");
                                            edLocal.setText("");
                                            edPrice.setText("");
                                            imageView.setImageResource(R.drawable.botron_image);
                                            Toast.makeText(AddPlaceActivity.this, "Upload successful", Toast.LENGTH_LONG).show();
                                            startActivity(new Intent(AddPlaceActivity.this, HomeActivity.class));
                                        }else{
                                            Toast.makeText(AddPlaceActivity.this, "Failure! please check internet connection!", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(AddPlaceActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                            mProgressBar.setProgress((int) progress);
                        }
                    });
        } else {
            Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show();
        }
    }
    public boolean checkData(){
        if(edName.getText().toString().equals("")){
            edName.setError("Name is required!");
            edName.requestFocus();
            return false;
        }else if (edDes.getText().toString().equals("")){
            edDes.setError("Please type description for this place!");
            edDes.requestFocus();
            return false;
        }else if(edLocal.getText().toString().equals("")){
            edLocal.setError("Address is required!");
            edLocal.requestFocus();
            return false;
        }else if(edPrice.getText().toString().equals("")){
            edPrice.setError("Price is required!");
            edPrice.requestFocus();
            return false;
        }
        return true;
    }
}
