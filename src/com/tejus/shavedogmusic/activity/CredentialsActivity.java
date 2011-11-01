package com.tejus.shavedogmusic.activity;

import com.tejus.shavedogmusic.core.Definitions;
import com.tejus.shavedogmusic.R;
import com.tejus.shavedogmusic.R.id;
import com.tejus.shavedogmusic.R.layout;
import com.tejus.shavedogmusic.R.string;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class CredentialsActivity extends Activity {

    EditText userNameField;
    String userName;
    Button setUserName, backButton;
    Context mContext;

    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.creds_activity );

        mContext = this;
        userNameField = ( EditText ) findViewById( R.id.username );
        setUserName = ( Button ) findViewById( R.id.set );
        backButton = ( Button ) findViewById( R.id.back );

        Definitions.USERNAME = userName;

        setUserName.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick( View v ) {
                userName = userNameField.getText().toString();
                if ( userName.length() < Definitions.MIN_USERNAME_LENGTH ) {
                    String toastText = getResources().getString( R.string.username_too_small ) + Definitions.MIN_USERNAME_LENGTH;

                    Toast toast = Toast.makeText( mContext, toastText, Toast.LENGTH_SHORT );
                    toast.show();

                } else {
                    SharedPreferences settings = getSharedPreferences( Definitions.credsPrefFile, Context.MODE_PRIVATE );
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString( Definitions.prefUserName, userName );
                    editor.commit();
                }
            }
        } );

        backButton.setOnClickListener( new View.OnClickListener() {
            //TODO: encapsulate new activity launches in a helper
            @Override
            public void onClick( View v ) {
                Intent intent = new Intent();
                intent.setClass( mContext, PlayActivity.class );
                startActivity( intent );
            }
        } );

    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.setClass( mContext, PlayActivity.class );
        startActivity( intent );
        super.onBackPressed();
    }
}
