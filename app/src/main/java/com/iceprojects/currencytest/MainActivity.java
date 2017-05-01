package com.iceprojects.currencytest;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    //API
    //http://api.fixer.io/latest?base=CAD

    SharedPreferences sharedPref;
    SharedPreferences.Editor prefEditor;

    EditText amount;
    Button checkButton;
    Spinner currencySpinner;
    GridView currencyGrid;

    ArrayAdapter<String> adp;
    Long date1, date2;
    Long dateSaved;

    Boolean buttClick = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        amount = (EditText) findViewById(R.id.etAmount);
        checkButton = (Button) findViewById(R.id.button);
        currencySpinner = (Spinner) findViewById(R.id.currencySpinner);
        currencyGrid = (GridView) findViewById(R.id.gridView);
        currencyGrid.setVisibility(View.INVISIBLE);

        Set<String> emptySet = new HashSet<>(Arrays.asList("-", "--"));

        if (!sharedPref.getStringSet("baseSet", emptySet).equals(emptySet)){
            Log.d("Checking", "Here 00");
            List<String> baseFromSharedPref = new ArrayList<>(sharedPref.getStringSet("baseSet", emptySet));
            Collections.sort(baseFromSharedPref, String.CASE_INSENSITIVE_ORDER);
            adp = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_item, baseFromSharedPref);
            adp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            currencySpinner.setAdapter(adp);
        }

        checkButton.setHapticFeedbackEnabled(true);
        checkButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                checkButton.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                return false;
            }
        });
        checkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (amount.getText().toString().trim().equals("")){
                    Toast.makeText(getApplicationContext(), "Amount cannot be blank!", Toast.LENGTH_SHORT).show();
                }
                else if (Double.parseDouble(amount.getText().toString().trim()) > 0){

                    date1 = new Date().getTime();
                    dateSaved = sharedPref.getLong("refreshTime", -1);
                    long diff = dateSaved - date1;

                    if (diff > 1800000 | !sharedPref.getString("selectedBase", "").equals(currencySpinner.getSelectedItem().toString())) {
                        prefEditor = sharedPref.edit();
                        prefEditor.putLong("refreshTime", date1);
                        prefEditor.apply();
                        buttClick = true;
                        new JSFetchRates().execute("http://api.fixer.io/latest?base=" + currencySpinner.getSelectedItem().toString());
                    }
                    else {
                        List<String> finalData = new ArrayList<>();
                        List<String> baseFromSharedPref = new ArrayList<>(sharedPref.getStringSet("baseSet", null));
                        Collections.sort(baseFromSharedPref, String.CASE_INSENSITIVE_ORDER);
                        Double converted = Double.parseDouble(amount.getText().toString().trim());
                        Double multiplier;
                        DecimalFormat df = new DecimalFormat("#.####");
                        df.setRoundingMode(RoundingMode.CEILING);
                        for (String base : baseFromSharedPref) {
                            multiplier = Double.parseDouble(sharedPref.getString(base, "0"));
                            finalData.add(base + " : " + df.format(converted*multiplier) );
                        }
                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, finalData);
                        currencyGrid.setAdapter(adapter);
                        currencyGrid.setVisibility(View.VISIBLE);
                    }

                }
                else {
                    Toast.makeText(getApplicationContext(), "Amount cannot be 0 or below!", Toast.LENGTH_SHORT).show();
                }

            }
        });

        if (isNetworkAvailable()) {

            //Create and check timer
            //DateFormat df = new SimpleDateFormat("hh:mm:ss");
                date1 = new Date().getTime();
                dateSaved = sharedPref.getLong("refreshTime", -1);

                if (dateSaved == -1) {
                    Log.d("Checking", "Here 0000");
                    prefEditor = sharedPref.edit();
                    prefEditor.putLong("refreshTime", date1);
                    prefEditor.apply();
                    Log.d("Checking", "Here 1");
                    new JSFetchRates().execute("http://api.fixer.io/latest?base=CAD");
                } else {
                    date2 = dateSaved;
                    Log.d("Checking", "Here pppp");
                    long diff = date2 - date1;
                    if (diff > 1800000) {
                        prefEditor = sharedPref.edit();
                        prefEditor.putLong("refreshTime", date1);
                        prefEditor.apply();
                        Log.d("Checking", "Here 2");
                        new JSFetchRates().execute("http://api.fixer.io/latest?base=CAD");
                    }
                }
        }

        else {
            Toast.makeText(getApplicationContext(), "Something went wrong! Check Connection!", Toast.LENGTH_SHORT).show();
        }

    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public class JSFetchRates extends AsyncTask<String, String, Boolean>
    {

        ProgressDialog dialog;
        JSONObject parentJS;

        @Override
        protected void onPreExecute() {

            dialog=new ProgressDialog(MainActivity.this);
            dialog.setMessage("Fetching Rates . . .");
            dialog.setCancelable(false);
            dialog.setInverseBackgroundForced(false);
            dialog.show();
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(params[0]);
Log.d("checking", url.toString());
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(15000);
                connection.connect();

                InputStream stream = connection.getInputStream();

                if (connection.getResponseCode() != 200){
                    return false;
                }

                reader = new BufferedReader(new InputStreamReader(stream));

                StringBuffer stringBuffer = new StringBuffer();
                String responseString = "";

                while((responseString = reader.readLine()) != null)
                {
                    stringBuffer.append(responseString);
                }

                String js = stringBuffer.toString();
                parentJS = new JSONObject(js);

                return true;

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean s) {
            super.onPostExecute(s);

            if (s) {

                ArrayList<String> baseList = new ArrayList<String>();
                //ArrayList<String> valueList = new ArrayList<String>();
                Set<String> keySet = new HashSet<String>();
                //Set<String> valueSet = new HashSet<String>();
                try {
                    JSONObject finalJS = parentJS.getJSONObject("rates");
                    //Saving in shared preference
                    prefEditor = sharedPref.edit();

                    for (int i = 0; i < finalJS.names().length(); i++) {

                        Log.d("loling", finalJS.names().getString(i));
                        String key = finalJS.names().getString(i);
                        String value = finalJS.getString(finalJS.names().getString(i));
                        prefEditor.putString(key, value);
                        baseList.add(key);
                        //valueList.add(value);
                        keySet.add(key);
                        Log.d("loling", key + " - " + value);
                    }

                    prefEditor.putString("selectedBase" , parentJS.getString("base"));
                    prefEditor.putStringSet("baseSet", keySet);
                    prefEditor.apply();
                    adp = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_spinner_item, baseList);
                    adp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    currencySpinner.setAdapter(adp);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if (buttClick){
                    currencySpinner.setSelection(baseList.indexOf(sharedPref.getString("selectedBase", "")));

                    List<String> finalData = new ArrayList<>();
                    List<String> baseFromSharedPref = new ArrayList<>(sharedPref.getStringSet("baseSet", null));
                    Collections.sort(baseFromSharedPref, String.CASE_INSENSITIVE_ORDER);

                    Double converted = Double.parseDouble(amount.getText().toString().trim());
                    Double multiplier;
                    DecimalFormat df = new DecimalFormat("#.####");
                    df.setRoundingMode(RoundingMode.CEILING);

                    for (String base : baseFromSharedPref) {
                        multiplier = Double.parseDouble(sharedPref.getString(base, "0"));
                        finalData.add(base + " : " + df.format(converted*multiplier) );
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, finalData);
                    currencyGrid.setAdapter(adapter);
                    currencyGrid.setVisibility(View.VISIBLE);
                }

            }
            else
            {
                Toast.makeText(getApplicationContext(), "Something went wrong! Check Connection!", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        }
    }

}
