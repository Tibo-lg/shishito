package jibiki.fr.shishito;

import android.app.ActionBar;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import jibiki.fr.shishito.Models.Dictionary;
import jibiki.fr.shishito.Models.ListEntry;
import jibiki.fr.shishito.Models.Volume;
import jibiki.fr.shishito.Util.XMLUtils;

import static jibiki.fr.shishito.Util.HTTPUtils.doGet;


public class SearchActivity extends BaseActivity {

    private static final String TAG = "SearchActivity";
    public final static String ENTRY = "jibiki.fr.shishito.ENTRY";

    ListView listView;

    private Dictionary dictionary;
    private Volume volume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        listView = (ListView) findViewById(R.id.listView);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View v, int position, long id) {
                Intent intent = new Intent(SearchActivity.this, DisplayEntryActivity.class);
                intent.putExtra(ENTRY, (ListEntry) listView.getItemAtPosition(position));
                startActivity(intent);
            }
        });

        //searchView.setSubmitButtonEnabled(true);
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            new InitVolumeTask().execute();
        } else {
            Toast.makeText(getApplicationContext(), "No Network",
                    Toast.LENGTH_SHORT).show();
        }

        handleIntent(getIntent());

        // Associate searchable configuration with the SearchView
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) findViewById(R.id.action_search);
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false);


    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            search(query);
        }
    }

    public void search(String query) {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            new SearchTask().execute(query);
        } else {
            Toast.makeText(getApplicationContext(), "No Network",
                    Toast.LENGTH_SHORT).show();
        }
    }


    private class SearchTask extends AsyncTask<String, Void, ArrayList<ListEntry>> {


        public SearchTask() {

        }

        @Override
        protected ArrayList<ListEntry> doInBackground(String... params) {
            InputStream stream = null;
            ArrayList<ListEntry> result = null;
            try {
                String word = URLEncoder.encode(params[0], "UTF-8");
                //stream = doGet(SEREVR_API_URL + "Cesselin/jpn/cdm-headword|cdm-reading|cdm-writing/" + word + "/entries/?strategy=CASE_INSENSITIVE_STARTS_WITH");
                stream = doGet(SERVER_API_URL + "Cesselin/jpn/cdm-headword|cdm-reading|cdm-writing/" + word + "/entries/?strategy=CASE_INSENSITIVE_EQUAL");
                result = XMLUtils.parseEntryList(stream, volume);
                Log.v(TAG, "index=" + result);

            } catch (XmlPullParserException | ParserConfigurationException | SAXException | XPathExpressionException | IOException e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(ArrayList<ListEntry> result) {

            if (result == null) {
                Toast.makeText(getApplicationContext(), "There was an error!",
                        Toast.LENGTH_SHORT).show();
            } else {

                EntryListAdapter adapter = (EntryListAdapter) listView.getAdapter();
                if(adapter == null){
                    adapter = new EntryListAdapter(SearchActivity.this, result);
                    listView.setAdapter(adapter);
                }else {
                    adapter.clear();
                    adapter.addAll(result);
                    adapter.notifyDataSetChanged();
                }
            }
        }
    }

    private class InitVolumeTask extends AsyncTask<String, Void, Volume> {


        public InitVolumeTask() {

        }

        @Override
        protected Volume doInBackground(String... params) {
            InputStream stream = null;
            Volume volume = null;
            try {
                stream = doGet(SERVER_API_URL + "Cesselin/jpn/");
                volume = XMLUtils.createVolume(stream);

                Log.v(TAG, "index=" + volume);

            } catch (XmlPullParserException | IOException e) {
                e.printStackTrace();
            }
            return volume;
        }

        @Override
        protected void onPostExecute(Volume volume) {
            SearchActivity.this.volume = volume;
        }
    }
}
