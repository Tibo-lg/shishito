package jibiki.fr.shishito;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import jibiki.fr.shishito.Models.ListEntry;

public class DisplayEntryFragment extends Fragment {


    private static final String TAG = "DisplayEntryFragment";

    private static final String ENTRY = "entry";

    private ListEntry entry;

    private OnEditClickListener mListener;

    public DisplayEntryFragment() {
    }

    public static DisplayEntryFragment newInstance(ListEntry entry) {
        DisplayEntryFragment fragment = new DisplayEntryFragment();
        Bundle args = new Bundle();
        args.putSerializable(ENTRY, entry);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            entry = (ListEntry) getArguments().getSerializable(ENTRY);
        }
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                      Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.activity_display_entry, container, false);
        TextView vedette = (TextView) v.findViewById(R.id.vedette);
        String vText = entry.getKanji() +
                "   [<font color=#cc0000>" + entry.getHiragana() + "</font>]   " +
                "   [<font color=#cc0000>" + entry.getRomanji() + "</font>]";
        vedette.setText(Html.fromHtml(vText));
        TextView definition = (TextView) v.findViewById(R.id.definition);
        definition.setText(entry.getDefinition());
        TextView gram = (TextView) v.findViewById(R.id.gram);
        gram.setText(entry.getGram());


      /*  ListView listView = (ListView) findViewById(R.id.examples);
        ArrayList<String> examples = new ArrayList<String>();
        for(int i = 0; i< entry.getExamples().size(); i++){
            Example example = entry.getExamples().get(i);
            examples.add(entry.getExamples().get(i).getKanji() +
                    "   [<font color=#cc0000>" + entry.getExamples().get(i).getHiragana() + "</font>]   " +
                    "   [<font color=#cc0000>" + entry.getExamples().get(i).getRomanji() + "</font>]");
        }
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, examples);
        listView.setAdapter(adapter);*/

        return v;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnEditClickListener) {
            mListener = (OnEditClickListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem login = menu.findItem(R.id.action_sign_in);
        if(!login.getTitle().equals(getString(R.string.action_sign_in_short))) {
            MenuItem item = menu.add("Edit");
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        if(item.getTitle().equals("Edit")){
            mListener.onEditClick(entry);
        }
        return true;
    }

    public interface OnEditClickListener {
        // TODO: Update argument type and name
        void onEditClick(ListEntry entry);
    }

}
