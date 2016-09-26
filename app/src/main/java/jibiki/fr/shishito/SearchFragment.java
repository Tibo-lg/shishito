package jibiki.fr.shishito;

import android.app.SearchManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import jibiki.fr.shishito.Models.ListEntry;
import jibiki.fr.shishito.Models.Volume;
import jibiki.fr.shishito.Util.ViewUtil;
import jibiki.fr.shishito.Util.XMLUtils;

import static jibiki.fr.shishito.Util.HTTPUtils.doGet;


/**
 * A {@link Fragment} subclass that enables search and display of
 * serach results.
 * Activities that contain this fragment must implement the
 * {@link OnWordSelectedListener} interface
 * to handle interaction events.
 * Use the {@link SearchFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SearchFragment extends Fragment {

    @SuppressWarnings("unused")
    private static final String TAG = SearchFragment.class.getSimpleName();

    // the fragment initialization parameters
    private static final String QUERY = "query";
    private static final String VOLUME = "volume";

    private String query;
    private ListView listView;
    private transient ArrayList<ListEntry> curList;
    private TextView noResult;
    private OnWordSelectedListener mListener;

    private static final Pattern hiraganaPattern = Pattern.compile("[\\p{InHiragana}]+");

    public SearchFragment() {
        // Required empty public constructor
    }

    /**
     * Factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param query The search query to perform.
     * @param volume the volume object obtain by the main activity
     * @return A new instance of fragment SearchFragment.
     */
    public static SearchFragment newInstance(String query, Volume volume) {
        SearchFragment fragment = new SearchFragment();
        Bundle args = new Bundle();
        args.putString(QUERY, query);
        args.putSerializable(VOLUME, volume);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            query = getArguments().getString(QUERY);
        }
        setRetainInstance(true);
    }

    public void updateEntry(ListEntry entry) {
        for (int i = 0; i < curList.size(); i++) {
            ListEntry le = curList.get(i);
            if (le.getEntryId().equals(entry.getEntryId())) {
                EntryListAdapter ela = (EntryListAdapter) listView.getAdapter();
                ela.remove(le);
                ela.insert(entry, i);
                return;
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View v = inflater.inflate(R.layout.fragment_search, container, false);
        listView = (ListView) v.findViewById(R.id.listView);
        noResult = (TextView) v.findViewById(R.id.noResult);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View v, int position, long id) {
                mListener.onWordSelected((ListEntry) listView.getItemAtPosition(position));
            }
        });

        if (curList != null) {
            EntryListAdapter adapter = (EntryListAdapter) listView.getAdapter();
            if (adapter == null) {
                adapter = new EntryListAdapter(getActivity(), curList);
                listView.setAdapter(adapter);
            } else {
                adapter.clear();
                adapter.addAll(curList);
                adapter.notifyDataSetChanged();
            }
        }

        // Associate searchable configuration with the SearchView
        SearchManager searchManager =
                (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) v.findViewById(R.id.action_search);
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getActivity().getComponentName()));
        searchView.setIconifiedByDefault(false);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String s) {
                if (TextUtils.isEmpty(s)) {
                    EntryListAdapter adapter = (EntryListAdapter) listView.getAdapter();
                    if (adapter != null) {
                        adapter.clear();
                    }
                    curList = null;
                }
                return false;
            }

            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }
        });
        return v;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnWordSelectedListener) {
            mListener = (OnWordSelectedListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }

        if (query != null) {
            search(query);
        }
    }

    public void search(String query) {
        ConnectivityManager connMgr = (ConnectivityManager)
                getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            if (listView.getAdapter() != null) {
                EntryListAdapter adapter = (EntryListAdapter) listView.getAdapter();
                adapter.clear();
            }
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            Fragment prev = getFragmentManager().findFragmentByTag("dialog");
            if (prev != null) {
                ft.remove(prev);
            }
            ft.addToBackStack(null);

            // Create and show the dialog.
            DialogFragment newFragment = new ShishitoProgressDialog();
            newFragment.show(ft, "dialog");
            new SearchTask().execute(query);
        } else {
            Toast.makeText(getActivity().getApplicationContext(), R.string.no_network,
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * Interface to call main activity when a word in the list is selected
     * by the user.
     */
    public interface OnWordSelectedListener {
        void onWordSelected(ListEntry entry);
    }

    private class SearchTask extends AsyncTask<String, Void, ArrayList<ListEntry>> {


        SearchTask() {

        }

        @Override
        protected ArrayList<ListEntry> doInBackground(String... params) {
            InputStream stream;
            ArrayList<ListEntry> result = null;
            try {
                String word = ViewUtil.normalizeQueryString(params[0]);
                int firstCharCode = Character.codePointAt(word,1);
                // Si le mot est en romaji (attention, il peut y avoir des macrons (ā = 257 à ū = 360)
                if (firstCharCode < 0x3042) {
                    word = ViewUtil.replace_macron(word);
                    word = ViewUtil.to_hepburn(word);
                    word = URLEncoder.encode(word, "UTF-8");
                    stream = doGet(SearchActivity.VOLUME_API_URL + "cdm-writing/" + word + "/entries/?strategy=CASE_INSENSITIVE_EQUAL");
                }
                // Si le mot est en hiragana
                else {
                    Matcher hiraganaMatcher = hiraganaPattern.matcher(word);
                    word = URLEncoder.encode(word, "UTF-8");
                    if (hiraganaMatcher.matches()) {
                        stream = doGet(SearchActivity.VOLUME_API_URL + "cdm-reading/" + word + "/entries/?strategy=EQUAL");
                    }
                    // Sinon, mot en japonais (katakana, kanji, ...)
                    else {
                        stream = doGet(SearchActivity.VOLUME_API_URL + "cdm-headword/" + word + "/entries/?strategy=EQUAL");
                    }
                }

                result = XMLUtils.parseEntryList(stream, ((SearchActivity) getActivity()).getVolume());

            } catch (ParserConfigurationException | SAXException | XPathExpressionException | IOException e) {
                Toast.makeText(SearchFragment.this.getActivity().getApplicationContext(), R.string.error,
                        Toast.LENGTH_SHORT).show();
            }
            curList = result;
            return result;
        }

        @Override
        protected void onPostExecute(ArrayList<ListEntry> result) {
            Fragment prev = getFragmentManager().findFragmentByTag("dialog");
            if (prev != null && prev instanceof ShishitoProgressDialog) {
                ShishitoProgressDialog pd = (ShishitoProgressDialog) prev;
                pd.dismiss();
            }
            if (result == null) {
                Toast.makeText(getActivity().getApplicationContext(), "There was an error!",
                        Toast.LENGTH_SHORT).show();
            } else {
                ListView listView = (ListView) getActivity().findViewById(R.id.listView);
                if (result.size() == 0) {
                    noResult.setVisibility(View.VISIBLE);
                } else {
                    noResult.setVisibility(View.GONE);
                    EntryListAdapter adapter = (EntryListAdapter) listView.getAdapter();
                    if (adapter == null) {
                        adapter = new EntryListAdapter(getActivity(), result);
                        listView.setAdapter(adapter);
                    } else {
                        adapter.clear();
                        adapter.addAll(result);
                        adapter.notifyDataSetChanged();
                    }
                }
            }
        }
    }

}
