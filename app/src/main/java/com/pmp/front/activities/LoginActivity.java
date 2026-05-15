package com.pmp.front.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.pmp.front.R;

public class LoginActivity extends AppCompatActivity {

    private EditText etUser, etPassword;
    private TextView btnLogin;

    private final String USER = "admin";
    private final String PASSWORD = "1234";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUser = findViewById(R.id.etUser);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnLogin.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#006341")
        ));

        btnLogin.setOnClickListener(v -> {
            String user = etUser.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (user.equals(USER) && password.equals(PASSWORD)) {
                Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show();
            }
        });
    }
}