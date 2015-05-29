package james.landtop;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import org.jsoup.Connection.Response;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;


public class MainActivity extends AppCompatActivity {

    private Handler uiHandler = new Handler();
    private ListView listView;
    private HandlerThread thread;
    private Handler threadHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = (ListView)findViewById(R.id.myListView);

        thread = new HandlerThread("jsoup");
        thread.start();

        threadHandler = new Handler(thread.getLooper());
        threadHandler.post(new Runnable() {
            @Override
            public void run() {
                final ArrayList<HashMap<String, Object>> arrayList = get();

                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        SimpleAdapter adapter = new SimpleAdapter(
                                getApplicationContext(),
                                arrayList,
                                R.layout.list,
                                new String[]{ "article", "time" },
                                new int[] { R.id.textView1, R.id.textView2 });
                        listView.setAdapter(adapter);
                    }
                });
            }
        });
    }

    protected ArrayList<HashMap<String, Object>> get()  {
        ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
        Response response = null;

        try {
            while (response == null || response.statusCode() != 200) {
                response = Jsoup.connect("http://www.landtop.com.tw/products.php?types=1").timeout(3000).execute();
                Thread.sleep(1000);
            }

            Document doc = response.parse();
            Elements companys = doc.getElementsByTag("table");

            for (int i = 0; i < companys.size(); i++) {
                Element company = companys.get(i);
                Elements phones = company.getElementsByTag("tr");

                for (int j = 1; j < phones.size(); j++) {
                    Element phone = phones.get(j);
                    String name = phone.child(0).child(0).child(0).text();
                    String money = phone.child(1).text();

                    HashMap<String, Object> hashMap = new HashMap<String, Object>();

                    hashMap.put("article", name);
                    hashMap.put("time", money);
                    list.add(hashMap);
                }
            }

            return list;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
