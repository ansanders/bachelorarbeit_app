package development.andre.sanders.bachelorprojectapp.controller;

import android.content.Intent;
import android.view.View;

import development.andre.sanders.bachelorprojectapp.R;
import development.andre.sanders.bachelorprojectapp.view.LoadingActivity;
import development.andre.sanders.bachelorprojectapp.view.OpenCvActivity;
import development.andre.sanders.bachelorprojectapp.view.StartActivity;

/**
 * Created by andre on 02.07.17.
 */

public class StartController implements View.OnClickListener {

    private StartActivity startActivity;

    public StartController(StartActivity startActivity){

        this.startActivity = startActivity;


    }


    @Override
    public void onClick(View v) {
//        Intent intent;
//        intent = new Intent(startActivity.getApplicationContext(), LoadingActivity.class);
//
//        switch (v.getId()){
//            case R.id.startStudentMode:
//                intent.putExtra("appMode", "studentMode");
//                startActivity.startActivity(intent);
//                break;
//
//            case R.id.startMuseumMode:
//                intent.putExtra("appMode", "museumMode");
//                startActivity.startActivity(intent);
//                break;
//
//            default:
//                break;
//
//        }
    }
}
