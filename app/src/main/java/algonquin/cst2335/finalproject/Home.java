
/*
 * Course: 22F-CST2335-021
 * Professor: Adewole Adewumi
 * Author: Kamelia Pezeshki
 * student# 040844200
 * File name: Home.java
 * Date: 2022-12-006
 * Final Project
 */
package algonquin.cst2335.finalproject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.cst2335.finalproject.R;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.squareup.picasso.Picasso;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

/**
 * A simple {@link Fragment} subclass.
 * Author: Kamelia Pezeshki
 */
public class Home extends Fragment {
    /**
     * an instance of class arrayList to act as a container for events
     */
    private ArrayList<Event> events = new ArrayList<>();
    /**
     * create instance of ImageView class
     */
    ImageView placeholderImage;
    /**
     * create instance of ListAdapter class
     */
    ListAdapter eventAdapter;
    /**
     * create instance of ProgressBar class
     */
    ProgressBar simpleProgressBar;
    /**
     * create instance of EditText class
     */
    EditText eventTextBox;
    /**
     * create instance of EditText class
     */
    EditText radiusTextBox;

    /**
     * default constructor
     */
    public Home() {
        // Required empty public constructor
    }

    /**
     *this class is where to inflate the UI
     *
      * @param inflater : a LayoutInflater object to load an XML layout file
     * @param container  : acts as an invisible container in which other Views and Layouts are placed
     * @param savedInstanceState : a reference to a Bundle object that is passed into the onCreateView method
     * @return newView : is the root object from your XML file, It contains the widgets that are in your layout
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setTitle("Home - TicketMaster");

        SharedPreferences prefs = this.getActivity().getSharedPreferences( "SEARCH_DETAIL" , Context.MODE_PRIVATE);
        String city = prefs.getString("city", "");
        String radius = prefs.getString("radius", "");

        View newView = inflater.inflate(R.layout.fragment_home, container, false);
        eventTextBox = newView.findViewById(R.id.eventTextBox);
        radiusTextBox = newView.findViewById(R.id.radiusTextBox);
        eventTextBox.setText(city);
        radiusTextBox.setText(radius);
        // Inflate the layout for this fragment
        placeholderImage = newView.findViewById(R.id.searchPlaceholder);
        simpleProgressBar= newView.findViewById(R.id.simpleProgressBar);
        simpleProgressBar.setVisibility(View.GONE);

        Button btnSearch = newView.findViewById(R.id.btnSearch);
        ListView eventListView = newView.findViewById(R.id.eventListView);
        /*To populate the ListView with data,call setAdapter() on the ListView,to associate an adapter with the list*/
        eventListView.setAdapter(eventAdapter = new ListAdapter());

        // Add search event
        btnSearch.setOnClickListener(click -> {

            searchNearByEvents(newView);
            //moves scroll to the top when the user click search button
            eventListView.smoothScrollToPosition(0);
        });

        eventListView.setOnItemClickListener( (list, view, position , id) -> {
            // pass id to the detail fragment
            algonquin.cst2335.finalproject.Details detailFragment = new algonquin.cst2335.finalproject.Details();
            Bundle args = new Bundle();
            args.putString(detailFragment.ID, events.get(position)._id);
            detailFragment.setArguments(args);

            FragmentTransaction ft = getParentFragmentManager().beginTransaction();// begin  FragmentTransaction
            ft.setReorderingAllowed(true);
            ft.replace(R.id.frameLayout, detailFragment, "DETAIL").addToBackStack("HOME");    // add    Fragment
            ft.commit();
        });

        return newView;
    }

    /**
     * When the find button is hit, this method will be invoked.
     * It obtains the data as a string from the API and then renders it as a list.
     *
     * @param newView : is now the root object from your XML file, It contains the widgets that are in your layout
     */
    private void searchNearByEvents(View newView) {
        InputMethodManager inputManager = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);
//if the search boxes are empty do nothing
         if(eventTextBox.getText().length() == 0 || radiusTextBox.getText().length() == 0) {
             return;
         }
        String result = null;
        try {
            result = new RequestTask()
                    .execute("https://app.ticketmaster.com/discovery/v2/events.json?apikey=ylAAMmRMEsPlfVwdNRsbPGzh9sUwbyNm&city="+ eventTextBox.getText() +"&radius="+ radiusTextBox.getText())
                    .get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        events.removeAll(events);

        Gson gson = new Gson();
        JsonObject eventObject = gson.fromJson(result, JsonObject.class);
        JsonObject initialResponse = eventObject.get("_embedded").getAsJsonObject();
        JsonArray eventsArray = initialResponse.get("events").getAsJsonArray();

       Toast.makeText(getActivity(), eventsArray.size() + " Close by events found!", Toast.LENGTH_LONG).show();

        for(int i=0; i < eventsArray.size(); i++) {
            JsonElement item = eventsArray.get(i);
            JsonObject obj = item.getAsJsonObject();

            String name = obj.get("name").getAsString();
            String id = obj.get("id").getAsString();
            String url = obj.get("images").getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString();
            Event event = new Event(name, id, url);
            events.add(event);


        }
        placeholderImage.setVisibility(View.INVISIBLE);
        eventAdapter.notifyDataSetChanged();
        // System.out.println(eventTextBox.getText() + " " + radiusTextBox.getText());
    }

    /**
     * this class using an AsyncTask to query data from the internet
     */
    class RequestTask extends AsyncTask<String, String, String> {

        //Network connections MUST be opened in doInBackground
        @Override
        public String doInBackground(String ... uri) {
            try {
                URL url = new URL(uri[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line+"\n");
                    }
                    br.close();
                    return sb.toString();
                } finally {
                    urlConnection.disconnect();
                }
            } catch (Exception e) {
                System.out.println("Kamelia" + e.getMessage());
            }
            return "Done";
        }

        @Override
        protected void onPreExecute() {
            simpleProgressBar.setVisibility(View.VISIBLE);
        }
        //this method updates the gui
        public void onProgressUpdate(Integer ... args) {
            simpleProgressBar.setVisibility(View.VISIBLE);
        }
        //This method will execute when the doInBackground has finished
        public void onPostExecute(String fromDoInBackground) {
            new Handler().postDelayed(new Runnable(){
                @Override
                public void run(){
                    simpleProgressBar.setVisibility(View.GONE);
                }
            }, 1000);
        }
    }

    /**
     *These 4 public functions must be written in order to implement
     * the interface known as ListAdapter.
     * Whenever a ListView needs a customised list
     */
    private class ListAdapter extends BaseAdapter {
        //1-returns the number of items to display in the list(returns the size of theArray or ArrayList)
        public int getCount(){
            return events.size();
        }

        //2-This function returnS the object to display at row position in the list
        public Event getItem(int position) {
            return events.get(position);
        }

        //3-return the database ID of the element at the given index of position
        public long getItemId(int position) {
            return getItem(position).id;
        }
        //4-creates a View object(newView) to go in a row of the ListView and returns newView
        public View getView(int position, View old, ViewGroup parent){

            LayoutInflater inflater = getLayoutInflater(); //we need a LayoutInflater object to load an XML layout file
            //load an XML layout file, make a new row
            //newView is now the root object from your XML file, It contains the widgets that are in your layout
            View newView = inflater.inflate(R.layout.row_layout, parent,false);

            LinearLayout rowLayout = newView.findViewById(R.id.layoutContainer);

            ImageView iView = newView.findViewById(R.id.eventImg);
            TextView tView = newView.findViewById(R.id.eventDetails);
            Picasso.get().load(getItem(position).imageUrl).into(iView);
            tView.setText(getItem(position).name);

            return newView;   //return the inflated view
        }
    }

    /**
     * this is a type of a class for  eventList arrayList<Event>
     */
    class Event {
        String name;
        String _id;
        String imageUrl;
        long id;

        Event(String name, String id, String imageUrl){
            this.name = name;
            this._id = id;
            this.imageUrl = imageUrl;
        }
    }

    /**
     * When the fragment is destroyed, the method's code will be run.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        SharedPreferences prefs = getActivity().getSharedPreferences( "SEARCH_DETAIL" , Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("city", eventTextBox.getText().toString());
        edit.putString("radius", radiusTextBox.getText().toString());
        edit.commit();
    }
}