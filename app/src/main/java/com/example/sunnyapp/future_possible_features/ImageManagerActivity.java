package com.example.sunnyapp.future_possible_features;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.os.Bundle;



import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.example.sunnyapp.FirebaseController;
import com.example.sunnyapp.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageTask;
import com.squareup.picasso.Picasso;

import java.util.Date;

public class ImageManagerActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String IMAGE = "image/*";
    private static final String UPLOADS = "uploads/";

    private Button mButtonChooseImage;
        private Button mButtonUpload;
        private TextView mTextViewShowUploads;
        private EditText mEditTextFileName;
        private ImageView mImageView;
        private ProgressBar mProgressBar;

        private StorageReference mStorageRef;
        private FirebaseFirestore mDatabaseRef;
        private DocumentReference mDocRef;
        private FirebaseAuth mAuth;
        private FirebaseController firebaseController;

        private StorageTask mUploadTask;

        private Uri mImageUri;
        private ImageFlowController imageFlowController;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_image_manager);

            initActivityVars();
            initFireBaseVars();

            activateChooseImageListener();
            activateUploadButtonListener();
            activateShowUploadsListener();
        }


    private void activateShowUploadsListener() {
        mTextViewShowUploads.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    private void activateUploadButtonListener() {
        mButtonUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mUploadTask != null && mUploadTask.isInProgress()) {
                    Toast.makeText(ImageManagerActivity.this, "Upload in progress", Toast.LENGTH_SHORT).show();
                } else {
                    uploadFile();
                }
            }
        });
    }

    private void activateChooseImageListener() {
        mButtonChooseImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser();
            }
        });
    }

    private void initActivityVars() {
        mButtonChooseImage = findViewById(R.id.button_choose_image);
        mButtonUpload = findViewById(R.id.button_upload);
        mTextViewShowUploads = findViewById(R.id.text_view_show_uploads);
        mEditTextFileName = findViewById(R.id.edit_text_file_name);
        mImageView = findViewById(R.id.image_view);
        mProgressBar = findViewById(R.id.progress_bar);
    }

    private void initFireBaseVars() {
        imageFlowController = ImageFlowController.getInstance();
        firebaseController = FirebaseController.getInstance();
        mStorageRef = firebaseController.storage.getReference("uploads");
        mDatabaseRef = firebaseController.db;
        mAuth = firebaseController.mAuth;
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType(IMAGE);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            mImageUri = data.getData();
            Picasso.get().load(mImageUri).into(mImageView);
        }
    }

    private String getFileExtension(Uri uri) {
        ContentResolver cR = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cR.getType(uri));
    }

    private void uploadFile() {
        if(validateGoogleUser()) {
            mDocRef = mDatabaseRef.document(UPLOADS + mAuth.getUid());
            if (mImageUri != null) {
                StorageReference fileReference = mStorageRef.child(System.currentTimeMillis()
                        + "." + getFileExtension(mImageUri));

                uploadImageToStorage(fileReference);


            } else {
                Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show();
            }
        }
        // else use a google account.
    }

    private void uploadImageToStorage(StorageReference fileReference) {
        mUploadTask = fileReference.putFile(mImageUri)
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

                        getDownloadUrl(taskSnapshot);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ImageManagerActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                        mProgressBar.setProgress((int) progress);
                    }
                });
    }

    private void getDownloadUrl(UploadTask.TaskSnapshot taskSnapshot){
        Task<Uri> firebaseUri = taskSnapshot.getStorage().getDownloadUrl();
        firebaseUri.addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {

                String url = uri.toString();
                Log.e("TAG:", "the url is: " + url);

                String ref = mStorageRef.getName();
                Log.e("TAG:", "the ref is: " + ref);

                Toast.makeText(ImageManagerActivity.this,
                        "Upload successful", Toast.LENGTH_LONG).show();

                uploadMetaDataToDB(url);
            }
        });
    }

    private boolean validateGoogleUser() {
        return mAuth.getCurrentUser() != null;
    }

    public void uploadMetaDataToDB(String url)
    {
        String imageName = mEditTextFileName.getText().toString().trim();
        Date date = new Date();
        Timestamp now = new Timestamp(date);
        ImageDataDTO imageDataDTO = new ImageDataDTO(now, url, imageName);
        mDocRef.set(imageDataDTO)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d("Image metadata:", "Image meta data has been saved!");
                // Call some intent to move screen on success?
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("Image metadata:", "Failure: Image metadata has not been saved!", e);
            }
        });
    }
    }


