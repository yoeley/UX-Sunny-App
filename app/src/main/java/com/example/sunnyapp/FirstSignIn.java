package com.example.sunnyapp;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;
import com.squareup.picasso.Picasso;

public class FirstSignIn extends AppCompatActivity{

    static final int GOOGLE_SIGN = 123;
    FirebaseAuth mAuth;
    GoogleSignInClient googleSignInClient;
    Button loginButton, logoutButton;
    TextView loginText;
    ImageView loginImage;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.first_sign_in);

        loginButton = findViewById(R.id.login);
        logoutButton = findViewById(R.id.logout);
        loginText = findViewById(R.id.firebase_text);
        loginImage = findViewById(R.id.firebase_icon);
        progressBar = findViewById(R.id.progress_circular);
        mAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions
                .Builder()
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);

        loginButton.setOnClickListener(v -> signInGoogle());
        logoutButton.setOnClickListener(v -> logout());

        if(mAuth.getCurrentUser() != null)
        {
            FirebaseUser user = mAuth.getCurrentUser();
            updateUI(user);
        }
    }

    void signInGoogle(){
        progressBar.setVisibility(View.VISIBLE);

        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, GOOGLE_SIGN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GOOGLE_SIGN) {
            Task<GoogleSignInAccount> task = GoogleSignIn
                    .getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null)
                {
                    fireBaseAuthWithGoogle(account);
                }
            }catch (ApiException e)
            {
                e.printStackTrace();
            }

        }
    }

    private void fireBaseAuthWithGoogle(GoogleSignInAccount account) {
        Log.d("TAG", "FirebaseAuthWithGoogle: " + account.getId());
        AuthCredential authCredential = GoogleAuthProvider.getCredential(account.getIdToken(),
                null);
        mAuth.signInWithCredential(authCredential)
                .addOnCompleteListener(this, task -> {
                   if(task.isSuccessful()){
                       progressBar.setVisibility(View.INVISIBLE);
                       Log.d("TAG", "SignIn Successful");
                       FirebaseUser user = mAuth.getCurrentUser();
                       updateUI(user);
                   }
                   else {
                       progressBar.setVisibility(View.INVISIBLE);
                       Log.d("TAG", "SignIn Failed", task.getException());
                       Toast.makeText(this, "Sign In Failed",
                               Toast.LENGTH_LONG).show();
                       updateUI(null);
                   }
                });
    }

    private void updateUI(FirebaseUser user) {
        if(user != null)
        {
            String name = user.getDisplayName();
            String email = user.getEmail();
            String photo = String.valueOf(user.getPhotoUrl());

            loginText.append("Info :\n");
            loginText.append(name + "\n");
            loginText.append(email);

            Picasso.get().load(photo).into(loginImage);
            loginButton.setVisibility(View.INVISIBLE);
            logoutButton.setVisibility(View.VISIBLE);
        }
        else {
            loginText.setText(getString(R.string.firebase_login));
            Picasso.get().load(R.drawable.ic_firebase_logo).into(loginImage);
            loginButton.setVisibility(View.VISIBLE);
            logoutButton.setVisibility(View.INVISIBLE);
        }
    }

    private void logout(){
        FirebaseAuth.getInstance().signOut();
        googleSignInClient.signOut()
                .addOnCompleteListener(this,
                        task -> updateUI(null));
    }
}
