package com.tejus.shavedogmusic.utils;

import com.tejus.shavedogmusic.R;
import com.tejus.shavedogmusic.core.Definitions;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.widget.EditText;
import android.widget.Toast;

public class ShaveDialog implements OnDismissListener, OnCancelListener {
    final private EditText editText;
    final private AlertDialog alertDialog;
    private String positiveButtonText, title, message;
    Context mContext;

    private Boolean canceled;

    public ShaveDialog( Context context, String title, String message, String positiveButtonText ) {
        mContext = context;
        this.title = title;
        this.message = message;
        this.positiveButtonText = positiveButtonText;
        editText = new EditText( context );
        alertDialog = buildAlertDialog( context );
        alertDialog.setOnDismissListener( this );
        alertDialog.setOnCancelListener( this );
        Logger.d( "message = " + message + ", title = " + title );
        show();
    }

    private AlertDialog buildAlertDialog( Context context ) {
        return new AlertDialog.Builder( context ).setTitle( title )
                .setMessage( message )
                .setView( editText )
                .setNeutralButton( positiveButtonText, null )
                .create();
    }

    public void show() {
        canceled = false;
        alertDialog.show();
    }

    @Override
    public void onDismiss( DialogInterface dialog ) {
        if ( !canceled ) {
            final String userName = editText.getText().toString();
            if ( userName.length() < 3 ) {
                String toastText = mContext.getString( R.string.username_too_small ) + Definitions.MIN_USERNAME_LENGTH;
                Toast toast = Toast.makeText( mContext, toastText, Toast.LENGTH_SHORT );
                toast.show();
                show();

            } else {
                SharedPreferences settings = mContext.getSharedPreferences( Definitions.credsPrefFile, Context.MODE_PRIVATE );
                SharedPreferences.Editor editor = settings.edit();
                editor.putString( Definitions.prefUserName, userName );
                editor.commit();
                alertDialog.cancel();
            }
        }
    }

    @Override
    public void onCancel( DialogInterface dialog ) {
        canceled = true;
    }
}
