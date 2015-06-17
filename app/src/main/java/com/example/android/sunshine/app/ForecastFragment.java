package com.example.android.sunshine.app;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;


/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {

    private ArrayAdapter<String> mForecastAdapter;
    public final String LOG_TAG = ForecastFragment.class.getSimpleName();
    private String mPostalCode = "94043";

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        setHasOptionsMenu(true);

        ArrayList<String> weekForecast = new ArrayList<>();

        mForecastAdapter = new ArrayAdapter<>(getActivity(), R.layout.list_item_forecast, R.id.list_item_forecast_textview, weekForecast);

        ListView list = (ListView) rootView.findViewById(R.id.listview_forecast);
        list.setAdapter(mForecastAdapter);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String forecast = mForecastAdapter.getItem(position);
                Intent launchDetail =
                        new Intent(getActivity(),DetailActivity.class)
                        .putExtra(Intent.EXTRA_TEXT, forecast);
                startActivity(launchDetail);
            }
        });

        FetchWeatherTask fetchWeatherTask = new FetchWeatherTask();
        fetchWeatherTask.execute(mPostalCode);

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        getActivity().getMenuInflater().inflate(R.menu.menu_forecast, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            FetchWeatherTask fetchWeatherTask = new FetchWeatherTask();
            fetchWeatherTask.execute(mPostalCode);
            return true;
        }
        if(id == R.id.action_settings){
            Intent intent = new Intent(getActivity(),SettingsActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }


    private class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        @Override
        protected String[] doInBackground(String... params) {
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String[] forecasts = new String[]{};

            if (params.length == 0) return null;

            String postalCode = params[0];
            String format = "json";
            String units = "metric";
            int days = 7;


            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast

                Uri.Builder uriBuilder = Uri
                        .parse("http://api.openweathermap.org/data/2.5/forecast/daily?").buildUpon()
                        .appendQueryParameter("q", postalCode)
                        .appendQueryParameter("mode", format)
                        .appendQueryParameter("units", units)
                        .appendQueryParameter("cnt", Integer.toString(days));

                URL url = new URL(uriBuilder.build().toString());

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuilder builder = new StringBuilder();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // builder for debugging.
                    line = line + "\n";
                    builder.append(line);
                }

                if (builder.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                forecasts = new WeatherDataParser().getWeatherDataFromJson(builder.toString(), 7);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } catch (JSONException jex) {
                Log.e(LOG_TAG, "JSON Error ", jex);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            return forecasts;
        }

        @Override
        protected void onPostExecute(String[] data) {
            if(data != null){
                mForecastAdapter.clear();
                for(String forecast: data){
                    mForecastAdapter.add(forecast);
                }
            }
        }
    }
}
