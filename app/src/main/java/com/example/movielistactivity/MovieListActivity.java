package com.example.movielistactivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.movielistactivity.models.Config;
import com.example.movielistactivity.models.Movie;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;

public class MovieListActivity extends AppCompatActivity {


    // constants for string values to be utilized
    // base URL for API aka home website
    public final static String API_BASE_URL = "https://api.themoviedb.org/3";
    // the parameter name for the API key
    public final static String API_KEY_PARAM = "api_key";
    // tag for logging from this activity
    public final static String TAG = "MovieListActivity";

    AsyncHttpClient client;
    ArrayList<Movie> movies;
    // the recycler view
    RecyclerView rvMovies;
    // the adapter wired to the recycler view
    MovieAdapter adapter;
    Config config;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.movie_list_activity);
        // initialize client
        client = new AsyncHttpClient();
        // initialize the list of movies
        movies = new ArrayList<>();
        // initialize the adapter (movies array cannot be reinitialized after this point)
        adapter = new MovieAdapter(movies);
        // resolve the recycler view and connect a layout manager
        rvMovies = (RecyclerView) findViewById(R.id.rvMovies);
        rvMovies.setLayoutManager(new LinearLayoutManager(this));
        rvMovies.setAdapter(adapter);
        // get the configuration on app creation
        getConfiguration();
    }

    // get the list of currently playing movies from the API (basically config but change endpoint)
    private void getNowPlaying(){
        // create the url
        String url = API_BASE_URL + "/movie/now_playing";
        // set the request parameters, which will later get appended to url
        RequestParams params = new RequestParams();
        params.put(API_KEY_PARAM,getString(R.string.api_key));
        // execute GET request using client, expect result of JSON object response
        client.get(url, params, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    JSONArray results = response.getJSONArray("results");
                    // iterate through result set
                    for (int i = 0; i < results.length(); i++){
                        Movie movie = new Movie(results.getJSONObject(i));
                        movies.add(movie);
                        // notify adapter that a row was added
                        adapter.notifyItemInserted(movies.size()-1);
                    }
                    Log.i(TAG, String.format("Loaded %s movies", results.length()));
                } catch (JSONException e) {
                    logError("Failed to parse now playing movies", e, true);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                logError("Failed to get data from now_playing endpoint", throwable, true);
            }
        });
    }

    // get config from the API
    private void getConfiguration(){
        // create the url
        String url = API_BASE_URL + "/configuration";
        // set the request parameters, which will later get appended to url
        RequestParams params = new RequestParams();
        params.put(API_KEY_PARAM, getString(R.string.api_key));
        // execute GET request using client, expect result of JSON object response
        client.get(url, params, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
               // get the image base url
                try {
                    config = new Config(response);
                    Log.i(TAG, String.format("Loaded configuration with imageBaseUrl %s and posterSize %s", config.getImageBaseUrl(), config.getPosterSize() ));
                    // pass the config (utilizing setter in adapter)
                    adapter.setConfig(config);

                    // get the now playing movies
                    getNowPlaying();
                } catch (JSONException e) {
                    logError("Failed parsing configuration", e, true);
                }

            }
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                logError("Failed getting configuration", throwable, true);
            }
        });

    }

    // handle errors, log and alert user
    private void logError(String message, Throwable error, boolean alertUser){
        // always log the error
        Log.e(TAG, message, error);
        // alert the user, non silent
        if (alertUser){
            // show a long toast with the error message
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
        }

    }
}
