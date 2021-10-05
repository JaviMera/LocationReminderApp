package com.udacity.project4.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.locationreminders.RemindersActivity

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authentication)

        val providers = arrayListOf(
            AuthUI.IdpConfig.GoogleBuilder().build(), AuthUI.IdpConfig.EmailBuilder().build()
        )

        if(FirebaseAuth.getInstance().currentUser != null){
            val intent = Intent(this, RemindersActivity::class.java)
            startActivity(intent)
        }else{
            startActivityForResult(
                AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(
                    providers
                ).build(),
                1001
            )
        }

//          TODO: If the user was authenticated, send him to RemindersActivity

//          TODO: a bonus is to customize the sign in flow to look nice using :
        //https://github.com/firebase/FirebaseUI-Android/blob/master/auth/README.md#custom-layout
    }

    override fun onResume() {
        super.onResume()

        if(FirebaseAuth.getInstance().currentUser == null){
            val providers = arrayListOf(
                AuthUI.IdpConfig.GoogleBuilder().build(), AuthUI.IdpConfig.EmailBuilder().build()
            )

            if(FirebaseAuth.getInstance().currentUser != null){
                val intent = Intent(this, RemindersActivity::class.java)
                startActivity(intent)
            }else{
                startActivityForResult(
                    AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(
                        providers
                    ).build(),
                    1001
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == 1001){
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in user.
                Log.i(
                    "AuthenticationActivity",
                    "Successfully signed in user " +
                            "${FirebaseAuth.getInstance().currentUser?.displayName}!"
                )

                val remindersActivityIntent = Intent(this, RemindersActivity::class.java)
                startActivity(remindersActivityIntent)
            } else {
                // Sign in failed. If response is null the user canceled the sign-in flow using
                // the back button. Otherwise check response.getError().getErrorCode() and handle
                // the error.
                Log.i("AuthenticationActivity", "Sign in unsuccessful ${response?.error?.errorCode}")
            }
        }
    }
}
