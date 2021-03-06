package com.toharifqi.um.ukmq;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;


import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.github.javiersantos.materialstyleddialogs.MaterialStyledDialog;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.toharifqi.um.ukmq.helpers.Config;
import com.toharifqi.um.ukmq.model.ProductModel;
import com.toharifqi.um.ukmq.model.ProgressModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import es.dmoral.toasty.Toasty;

public class AddReport extends AppCompatActivity {
    Toolbar mToolbar;

    private static final String REQUIRED = "Mohon masukkan data dengan benar.";
    private DatabaseReference mDatabaseReference;
    private StorageReference storageReference;
    private FirebaseAuth fAuth;
    private String productCat;

    private TextInputLayout txtProgressWritter, txtProgressTitle, txtProgressDesc;

    private ImageView progressPic;
    private Uri progressPicUri;

    Button addProgressBtn;
    public ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_report);

        // Action Bar
        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.close);

        mDatabaseReference = FirebaseDatabase.getInstance().getReference();

        txtProgressWritter = findViewById(R.id.progress_writter);
        txtProgressTitle = findViewById(R.id.progress_title);
        txtProgressDesc = findViewById(R.id.progress_desc);

        progressPic = findViewById(R.id.progress_image);
        progressPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureImage();
            }
        });

        addProgressBtn = findViewById(R.id.add_progress);
        addProgressBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitPost();
            }
        });
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        fAuth = FirebaseAuth.getInstance();
    }

    private void submitPost() {
        dialog = ProgressDialog.show(AddReport.this, "",
                "Menambahkan data laporan progress baru. Mohon tunggu ...", true);

        String progressWritter = txtProgressWritter.getEditText().getText().toString();
        String progressTitle = txtProgressTitle.getEditText().getText().toString();
        String progreesDesc = txtProgressDesc.getEditText().getText().toString();

        boolean isNullPhotoUrl = progressPicUri == null;

        if (progressWritter.isEmpty()){
            showDialogNoInput();
            txtProgressWritter.setError(REQUIRED);
        }else if (progressTitle.isEmpty()){
            showDialogNoInput();
            txtProgressTitle.setError(REQUIRED);
        }else if (progreesDesc.isEmpty()){
            showDialogNoInput();
            txtProgressDesc.setError(REQUIRED);
        }else if(isNullPhotoUrl){
            showDialogNoInput();
            Toasty.error(getApplicationContext(), Config.IMAGE_URL_NULL_MESSAGE, Toasty.LENGTH_SHORT, true).show();
        }else {
            setEditingEnabled(false);
        }

        if (!isNullPhotoUrl){
            writeNewPost(progressWritter, progressTitle, progreesDesc, progressPicUri);
            setEditingEnabled(true);
        }
    }

    private void writeNewPost(final String progressWritter, final String progressTitle,
                              final String progreesDesc, Uri progressPicUri) {
        String charRandom = generateString();
        String productCodeNospaces = progressTitle.replace(" ", "");
        final String progressId = productCodeNospaces.concat("-" + charRandom);
        final String productIdUser = fAuth.getCurrentUser().getUid();

        final String uniqueKey = mDatabaseReference.push().getKey();
        storageReference = FirebaseStorage.getInstance().getReference().child("progress/"+uniqueKey).child(Config.STORAGE_PATH + progressPicUri.getLastPathSegment());
        StorageTask storageTask = storageReference.putFile(progressPicUri);
        Task<Uri> uriTask=storageTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {

            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> taskSnapshot) throws Exception {
                if(!taskSnapshot.isSuccessful()){
                    throw taskSnapshot.getException();
                }
                return storageReference.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if(task.isSuccessful()){
                    Uri downloadURi=task.getResult();


                    String city = Config.userKecamatan + ", " + Config.userKabupaten;
                    ProgressModel progress = new ProgressModel(progressWritter, progressTitle,
                            progreesDesc, downloadURi.toString(), productIdUser);

                    Map<String, Object> postValues = progress.addProgress();
                    Map<String, Object> childUpdates = new HashMap<>();

                    childUpdates.put("/products/" + progressId, postValues);

                    mDatabaseReference.updateChildren(childUpdates);
                    Toasty.success(getApplicationContext(), Config.BOOK_ADD_SUCCESS_MSG, Toasty.LENGTH_SHORT, true).show();
                }
                dialog.dismiss();
                // Finish this Activity, back to the stream
                finish();
            }
        });
    }

    private void setEditingEnabled(boolean enabled) {
        if (enabled) {
            addProgressBtn.setVisibility(View.VISIBLE);
        } else {
            addProgressBtn.setVisibility(View.GONE);
        }
    }

    private String generateString() {
        char[] chars = "QWERTYUIOPASDFGHJKLZXCVBNMqwertyuiopasdfghjklzxcvbnm1234567890".toCharArray();
        StringBuilder stringBuilder = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i< 5; i++){
            char c = chars[random.nextInt(chars.length)];
            stringBuilder.append(c);
        } return stringBuilder.toString();
    }

    private void captureImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, Config.PHOTO_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Config.PHOTO_REQUEST_CODE && requestCode == RESULT_OK && data!=null){
            progressPicUri = data.getData();
            progressPic.setImageURI(progressPicUri);
        }
    }

    private void showDialogNoInput() {
        dialog.dismiss();
        new MaterialStyledDialog.Builder(this)
                .setTitle("Oops...")
                .setDescription("Masukkan data produk Anda dengan benar. Pastikan tidak ada data yang kosong.")
                .setPositiveText("OK")
                .setHeaderDrawable(R.drawable.unavailable)
                .setCancelable(true)
                .setScrollable(true)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    }
                }).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}