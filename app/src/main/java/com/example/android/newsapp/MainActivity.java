package com.example.android.newsapp;


import android.annotation.SuppressLint;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Layout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<String> {

    private ListView list_view;
    private static ArrayList<HashMap<String, String>> newsList;
    private static ArrayList<HashMap<String, Drawable>> imageList;
    private static NewsAdapter mAdapter;
    private static List<String> webPages = new ArrayList<>();
    private TextView warningTV;
    private ImageView warningIV;
    private static final int NEWS_LOADER_ID = 1;
    private String author;
    private static String webUrl;
    private static String GUARDIAN_REQUEST_URL;
    private static String API_KEY;
    private static String KEY;
    private static String ORDER_BY;
    private static String ORDER;
    private static String SHOW_TAGS;
    private static String CONTRIBUTOR;
    private static String PAGE_SIZE;
    private static String SHOW_FIELDS;
    private static String THUMBNAIL;
    private static String SECTION;
    private static Uri.Builder uriBuilder;
    private static String builtUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        newsList = new ArrayList<>();
        imageList = new ArrayList<>();
        list_view = findViewById(R.id.list);
        warningTV = findViewById(R.id.warning);
        warningIV = findViewById(R.id.warning_image);

        GUARDIAN_REQUEST_URL = getString(R.string.raw_url);

        ORDER_BY = getResources().getString(R.string.ORDER_BY
        );
        ORDER = getResources().getString(R.string.ORDER
        );
        SHOW_TAGS = getResources().getString(R.string.TAGS);
        CONTRIBUTOR = getResources().getString(R.string.CONTRIBUTOR);
        PAGE_SIZE = getResources().getString(R.string.PAGE_SIZE);
        API_KEY = getResources().getString(R.string.API_KEY);
        KEY = getResources().getString(R.string.NEWS_API_KEY_TOKEN);
        SHOW_FIELDS = getResources().getString(R.string.SHOW_FIELDS);
        THUMBNAIL = getResources().getString(R.string.THUMBNAIL);
        SECTION = getResources().getString(R.string.SECTION);

        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            LoaderManager loaderManager = getLoaderManager();
            Loader<String> loader = loaderManager.getLoader(NEWS_LOADER_ID);
            if (loader == null) {
                loaderManager.initLoader(NEWS_LOADER_ID, null, this);
            } else {
                loaderManager.restartLoader(NEWS_LOADER_ID, null, this);
            }
        } else {
            View loadingIndicator = findViewById(R.id.progress_bar);
            loadingIndicator.setVisibility(View.GONE);
            warningTV.setText(getString(R.string.noInternet));
            list_view.setEmptyView(warningTV);
            warningIV.setImageResource(R.drawable.ic_no_internet);
            list_view.setEmptyView(warningIV);
        }


        list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

                // Convert the String URL into a URI object (to pass into the Intent constructor)

                Uri newsUri = Uri.parse(webPages.get(position));

                // Create a new intent to view the news URI
                Intent websiteIntent = new Intent(Intent.ACTION_VIEW, newsUri);

                PackageManager packageManager = getPackageManager();
                List<ResolveInfo> activities = packageManager.queryIntentActivities(websiteIntent,
                        PackageManager.MATCH_DEFAULT_ONLY);
                boolean isIntentSafe = activities.size() > 0;

                if (isIntentSafe) {
                    // Send the intent to launch a new activity
                    startActivity(websiteIntent);
                }
                newsList.clear();
                imageList.clear();
                webPages.clear();

            }
        });

    }

    @SuppressLint("StaticFieldLeak")
    @Override
    public Loader<String> onCreateLoader(int i, Bundle bundle) {
        // TODO: Create a new loader for the given URL

        return new AsyncTaskLoader<String>(this) {

            String resultFromHttp;

            @Override
            public String loadInBackground() {
                HttpHandler handler = new HttpHandler();

                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

                String orderByCategory = sharedPrefs.getString(
                        getString(R.string.category_title),
                        getString(R.string.all));
                String[] categoryArray = orderByCategory.split(" ");
                String categoryElement = categoryArray[0];
                String category = categoryElement.toLowerCase();

                String orderByNewsNumber = sharedPrefs.getString(
                        getString(R.string.page_title),
                        getString(R.string.default_page_number));

                Uri baseUri = Uri.parse(GUARDIAN_REQUEST_URL);

                uriBuilder = baseUri.buildUpon();

                // url is appended by query parameters and their values:

                uriBuilder.appendQueryParameter(ORDER_BY, ORDER);
                uriBuilder.appendQueryParameter(SHOW_TAGS, CONTRIBUTOR);
                uriBuilder.appendQueryParameter(PAGE_SIZE, orderByNewsNumber);
                uriBuilder.appendQueryParameter(API_KEY, KEY);
                uriBuilder.appendQueryParameter(SHOW_FIELDS, THUMBNAIL);

                if (!category.equals(getResources().getString(R.string.all))) {
                    uriBuilder.appendQueryParameter(SECTION, category);
                }

                // Two-words category names are formatted properly:

                if (category.equals(getResources().getString(R.string.us))) {
                    category = getString(R.string.usNews).toLowerCase().replace(" ", "-");
                    uriBuilder.appendQueryParameter(SECTION, category);
                }

                if (category.equals(getResources().getString(R.string.world))) {
                    category = getString(R.string.world);
                    uriBuilder.appendQueryParameter(SECTION, category);
                }

                if (category.equals(getResources().getString(R.string.life))) {
                    category = getString(R.string.lifeAndStyle).toLowerCase().replace(" ", "");
                    uriBuilder.appendQueryParameter(SECTION, category);
                }

                if (category.equals(getResources().getString(R.string.art))) {
                    category = getString(R.string.artAndDesign).toLowerCase().replace(" ", "");
                    uriBuilder.appendQueryParameter(SECTION, category);
                }

                if (category.equals(getResources().getString(R.string.television))) {
                    category = getString(R.string.televisionAndRadio).toLowerCase().replace(" ", "-").replace(getResources().getString(R.string.television), getResources().getString(R.string.tv)).replace("&", getResources().getString(R.string.and));
                    uriBuilder.appendQueryParameter(SECTION, category);
                }

                if (category.equals(getResources().getString(R.string.uk))) {
                    category = getString(R.string.ukNews).toLowerCase().replace(" ", "-");
                    uriBuilder.appendQueryParameter(SECTION, category);
                }

                if (category.equals(getResources().getString(R.string.australia))) {
                    category = getString(R.string.australianNews).toLowerCase().replace(" ", "-");
                    uriBuilder.appendQueryParameter(SECTION, category);
                }

                if (category.equals(getResources().getString(R.string.inequality))) {
                    category = getString(R.string.inequality).toLowerCase().replace(getResources().getString(R.string.ı), getResources().getString(R.string.i));
                    uriBuilder.appendQueryParameter(SECTION, category);

                }


                builtUrl = uriBuilder.toString();

                String jsonString = "";
                try {
                    jsonString = handler.makeHttpRequest(createUrl(builtUrl));
                } catch (IOException e) {
                    return null;
                }

                if (jsonString != null) {
                    try {

                        JSONObject jsonObject = new JSONObject(jsonString);
                        JSONObject response = jsonObject.getJSONObject(getString(R.string.response));
                        JSONArray results = response.getJSONArray(getString(R.string.results));

                        for (int i = 0; i < results.length(); i++) {

                            try {
                                JSONObject article = results.getJSONObject(i);

                                String title = article.getString(getString(R.string.webTitle));
                                String date = article.getString(getString(R.string.webPublicationDate));
                                String section = article.getString(getString(R.string.sectionName));
                                webUrl = article.getString(getString(R.string.webUrl));

                                JSONObject field = article.getJSONObject(getString(R.string.fields));

                                String thumbnail = field.getString(getString(R.string.thumbnail));

                                Drawable image = LoadImageFromWebOperations(thumbnail);

                                JSONArray tags = article.getJSONArray(getString(R.string.tags));

                                for (int j = 0; j < tags.length(); j++) {
                                    try {

                                        JSONObject authorInfo = tags.getJSONObject(j);
                                        author = authorInfo.getString(getString(R.string.webTitle));


                                    } catch (final JSONException e) {
                                        e.printStackTrace();
                                    }
                                }


                                HashMap<String, String> result = new HashMap<>();

                                // add each child node to HashMap key => value
                                result.put(getString(R.string.title), title);
                                result.put(getString(R.string.webPublicationDate), date);
                                result.put(getString(R.string.sectionName), section);
                                result.put(getString(R.string.author), author);

                                HashMap<String, Drawable> iv = new HashMap<>();
                                iv.put(getString(R.string.thumbnail), image);

                                // adding a news to our news list
                                newsList.add(result);
                                imageList.add(iv);
                                webPages.add(webUrl);

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {

                                            mAdapter = new NewsAdapter(MainActivity.this, newsList, imageList);
                                            if (mAdapter != null) {
                                                mAdapter.notifyDataSetChanged();
                                                list_view.setAdapter(mAdapter);
                                                warningTV.setVisibility(View.GONE);
                                            }

                                        } catch (IllegalStateException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                            } catch (final JSONException e) {

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getApplicationContext(),
                                                getString(R.string.parsingError) + e.getMessage(),
                                                Toast.LENGTH_LONG).show();
                                    }
                                });

                            }
                        }
                    } catch (final JSONException e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(),
                                        getString(R.string.parsingError) + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    getString(R.string.notGetJson),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }

                return null;
            }

            private URL createUrl(String stringUrl) {
                URL url;
                try {
                    url = new URL(stringUrl);
                } catch (MalformedURLException exception) {
                    return null;
                }
                return url;
            }

            private Drawable LoadImageFromWebOperations(String url) {
                try {
                    InputStream is = (InputStream) new URL(url).getContent();
                    return Drawable.createFromStream(is, getString(R.string.thumbnail));
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            protected void onStartLoading() {
                if (resultFromHttp != null) {
                    //To skip loadInBackground call
                    deliverResult(resultFromHttp);
                } else {
                    forceLoad();
                }
            }
        };
    }

    @SuppressLint("ResourceType")
    public void onLoadFinished(Loader<String> loader, String data) {
        // TODO: Update the UI with the result

        if (newsList != null && !newsList.isEmpty()) {

            warningTV.setVisibility(View.GONE);
            warningIV.setVisibility(View.GONE);
            View loadingIndicator = findViewById(R.id.progress_bar);
            loadingIndicator.setVisibility(View.GONE);

        } else {

            View loadingIndicator = findViewById(R.id.progress_bar);
            loadingIndicator.setVisibility(View.GONE);
            warningTV.setText(getString(R.string.noNews));
            list_view.setEmptyView(warningTV);
            list_view.setEmptyView(warningIV);

        }

    }

    @Override
    public void onLoaderReset(Loader<String> loader) {
        // TODO: Loader reset, so we can clear out our existing data.

        newsList.clear();
        imageList.clear();
        webPages.clear();
    }

    public void onResume() {
        newsList.clear();
        imageList.clear();
        webPages.clear();
        super.onResume();
    }

    @Override
    // This method initialize the contents of the Activity's options menu.
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the Options Menu we specified in XML
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}




