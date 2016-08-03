package com.nickiaconis.cadence;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;


public class MetronomeActivity extends Activity {

    private final int DEFAULT_TEMPO = 180;

    private TextView mBpmText;
    private int mTempo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_metronome);
        mBpmText = (TextView) findViewById(R.id.bpmText);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mTempo = loadTempo();
        mBpmText.setText(Integer.toString(mTempo));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveTempo(mTempo);
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_metronome, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

    public void onMetronomeToggle(View view) {
        this.updateService(Constants.ACTION.TOGGLE_ACTION);
    }

    public void onTempoIncrease(View view) {
        if (mTempo < 300) {
            mTempo += 10;
            mBpmText.setText(Integer.toString(mTempo));
            this.updateService(Constants.ACTION.SET_TEMPO_ACTION);
        }
    }

    public void onTempoDecrease(View view) {
        if (mTempo > 100) {
            mTempo -= 10;
            mBpmText.setText(Integer.toString(mTempo));
            this.updateService(Constants.ACTION.SET_TEMPO_ACTION);
        }
    }

    private void updateService(String action) {
        Intent intent = new Intent(MetronomeActivity.this, MetronomeService.class);
        intent.setAction(action);
        intent.putExtra(Constants.KEY.TEMPO_KEY, mTempo);
        startService(intent);
    }

    private int loadTempo() {
        SharedPreferences sharedPreferences = getSharedPreferences(
                Constants.KEY.PREFERENCES_KEY,
                MODE_PRIVATE
        );
        return sharedPreferences.getInt(Constants.KEY.TEMPO_KEY, DEFAULT_TEMPO);
    }

    private void saveTempo(int value) {
        SharedPreferences sharedPreferences = getSharedPreferences(
                Constants.KEY.PREFERENCES_KEY,
                MODE_PRIVATE
        );
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(Constants.KEY.TEMPO_KEY, value);
        editor.commit();
    }
}
