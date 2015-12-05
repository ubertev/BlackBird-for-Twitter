package com.example.edwardst4.blackbirdfortwitter;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

/**
 * Created by Tevin on 03-Dec-15.
 */
public class AlertDialogManager {
    // displays simple alert dialog for important messages

    public void showAlert(Context context, String title, String message, Boolean status) {
        AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        //set dialog title
        alertDialog.setTitle(title);
        // set dialog message
        alertDialog.setMessage(message);

        if (status != null)
            alertDialog.setIcon((status) ? R.drawable.success : R.drawable.fail);

        //set ok button
        alertDialog.setButton("OK", new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int which){
            }
        });
        // show alert message
        alertDialog.show();
    }
}
