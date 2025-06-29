package com.example.expensetracker;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AuthGuard {

    public static void checkAccess(MainActivity activity, Runnable onSuccess) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            activity.redirectToLogin();
            return;
        }

        // Validación mínima usando Realtime Database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database.getReference();

        ref.child("allTables") // O alguna ruta segura que tenga sentido leer
                .limitToFirst(1)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DataSnapshot> task) {
                        if (!task.isSuccessful()) {
                            Exception e = task.getException();
                            if (e != null) {
                                Log.e("AuthGuard", "Error getting data", e);
                            }
                            Toast.makeText(activity, "Acceso denegado. Iniciá sesión nuevamente.", Toast.LENGTH_SHORT).show();
                            mAuth.signOut();
                            activity.redirectToLogin();
                        } else {
                            // Permisos válidos
                            onSuccess.run();
                        }
                    }
                });
    }
}
