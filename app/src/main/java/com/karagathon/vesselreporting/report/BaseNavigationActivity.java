package com.karagathon.vesselreporting.report;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karagathon.vesselreporting.R;
import com.karagathon.vesselreporting.common.SettingsActivity;
import com.karagathon.vesselreporting.login.LoginActivity;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.Objects;

public class BaseNavigationActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, FirebaseAuth.AuthStateListener {

    protected FrameLayout frameLayout;
    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private FirebaseAuth auth;
    private boolean authStateFlag;
    private ImageView image;
    private TextView navDisplayEmail;
    private TextView navDisplayName;

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseAuth.getInstance().addAuthStateListener(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base_navigation_acitivity);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawerLayout);
        frameLayout = findViewById(R.id.frag_container);

        auth = FirebaseAuth.getInstance();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout,
                toolbar, R.string.nav_drawer_open, R.string.nav_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        switch (itemId) {
            case R.id.item_history:
                Intent historyIntent =
                        new Intent(BaseNavigationActivity.this, HistoryActivity.class);
                startActivity(historyIntent);
                break;
            case R.id.item_settings:
                Intent settingsIntent = new Intent(BaseNavigationActivity.this, SettingsActivity.class);
                startActivity(settingsIntent);
                break;
        }

        DrawerLayout drawer = findViewById(R.id.drawerLayout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void initNavDetails() {

        FirebaseUser user = auth.getCurrentUser();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        image = navigationView.getHeaderView(0).findViewById(R.id.nav_image);
        navDisplayEmail = navigationView.getHeaderView(0).findViewById(R.id.nav_display_email);

        navDisplayName = navigationView.getHeaderView(0).findViewById(R.id.nav_display_name);
        TextView navLogout = findViewById(R.id.logout);
        navLogout.setOnClickListener(view -> {

            user.getProviderData().forEach(u -> {
                switch (u.getProviderId()) {
                    case "facebook.com":
                        auth.signOut();
                        LoginManager.getInstance().logOut();
                        break;
                    default:
                        auth.signOut();
                }
            });

            Intent signInIntent = new Intent(BaseNavigationActivity.this, LoginActivity.class);
            startActivity(signInIntent);
        });

        user.getProviderData().forEach(u -> {

            switch (u.getProviderId()) {
                case "google.com":
                    GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getApplicationContext());
                    Picasso.get()
                            .load(account.getPhotoUrl())
                            .placeholder(R.drawable.ic_person)
                            .into(image);
                    navDisplayName.setText(account.getDisplayName());
                    navDisplayEmail.setText(account.getEmail());
                    break;
                case "facebook.com":
                    navDisplayName.setText(user.getDisplayName());
                    requestData();
                    break;
                case "password":
                    retrieveName(user.getEmail());
                    navDisplayEmail.setText(user.getEmail());
            }
        });
    }

    private void retrieveName(String userEmail) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("User");
        dbRef.orderByChild("email").equalTo(userEmail).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                snapshot.getChildren().forEach(dataSnapshot -> {
                    Map<String, Object> userMap = (Map<String, Object>) dataSnapshot.getValue();
                    Map.Entry<String, Object> entry = userMap.entrySet().iterator().next();

                    navDisplayName.setText(String.valueOf(entry.getValue()));
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                //
            }
        });
    }
    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
        if (firebaseAuth.getCurrentUser() != null) {
            if (!authStateFlag) {
                initNavDetails();
                authStateFlag = true;
            }
        }
    }

    private void requestData() {
        GraphRequest request = GraphRequest.newMeRequest(AccessToken.getCurrentAccessToken(), (object, response) -> {
            JSONObject json = response.getJSONObject();
            try {
                if (Objects.nonNull(json)) {
                    String fbPhoto = object.getJSONObject("picture")
                            .getJSONObject("data")
                            .getString("url");
                    String email = object.getString("email");

                    Picasso.get()
                            .load(fbPhoto)
                            .placeholder(R.drawable.ic_person)
                            .into(image);

                    navDisplayEmail.setText(email);
                }

            } catch (JSONException e) {
                Log.e("Request Data JSON Exception", e.getMessage());
            }
        });
        Bundle parameters = new Bundle();
        parameters.putString("fields", "email,picture");
        request.setParameters(parameters);
        request.executeAsync();
    }
}

