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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private Handler uiHandler = new Handler();
    private ListView listView;
    private HandlerThread thread;
    private Handler threadHandler;

    private HashMap<PhoneCompany.CompanyEnum, PhoneCompany> compsMap = new HashMap<>();
    ArrayList<HashMap<String, Object>> arrayList = new ArrayList<>();
    ArrayList<PhonePrice> allPhones = new ArrayList<>();
    ArrayList<PhonePrice> currentShow;
    SimpleAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = (ListView)findViewById(R.id.myListView);

        thread = new HandlerThread("jsoup");
        thread.start();

        adapter = new SimpleAdapter(
                getApplicationContext(),
                arrayList,
                R.layout.list,
                new String[]{ "article", "time" },
                new int[] { R.id.textView1, R.id.textView2 });
        listView.setAdapter(adapter);

        threadHandler = new Handler(thread.getLooper());
        threadHandler.post(new Runnable() {
            @Override
            public void run() {
                parse();
                // checkPrevPrice();

                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        currentShow = allPhones;
                        updateList();
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();

        /*
        settings = getSharedPreferences("PHONE", MODE_PRIVATE);

        for (PhonePrice pp : allPhones) {
            switch (pp.priceState) {
            }
        }
        */
    }

    private void parse() {
        Response response = null;

        allPhones.clear();
        compsMap.clear();

        try {
            while (response == null || response.statusCode() != 200) {
                response = Jsoup.connect("http://www.landtop.com.tw/products.php?types=1").timeout(3000).execute();
                Thread.sleep(1000);
            }

            Document doc = response.parse();
            Elements companies = doc.getElementsByTag("table");

            int phoneSn = 0;

            for (int i = 0; i < companies.size(); i++) {
                Element company = companies.get(i);
                Elements phones = company.getElementsByTag("tr");

                String companyPic = phones.get(0).child(0).child(1).attr("src");
                PhoneCompany phoneCompany = PhoneCompany.getCompanyFromPicture(companyPic);
                compsMap.put(phoneCompany.company, phoneCompany);

                for (int j = 1; j < phones.size(); j++) {
                    Element phone = phones.get(j);
                    String name = phone.child(0).child(0).child(0).text();
                    String money = phone.child(1).text().substring(2);

                    PhonePrice pp = new PhonePrice(phoneCompany, name, money, phoneSn);
                    phoneCompany.phones.add(pp);
                    allPhones.add(pp);
                    phoneSn++;
                }
            }
        } catch (Exception e) {
        }
    }

    private void updateList() {
        arrayList.clear();

        for (PhonePrice pp : currentShow) {
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("article", pp);
            hashMap.put("time", pp.price);
            arrayList.add(hashMap);
        }

        adapter.notifyDataSetChanged();
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

        switch (id) {
            case R.id.sort_by_money:
                Collections.sort(currentShow, new Comparator<PhonePrice>() {
                    @Override
                    public int compare(PhonePrice lhs, PhonePrice rhs) {
                        return Integer.compare(rhs.price, lhs.price);
                    }
                });
                updateList();
                return true;

            case R.id.sort_by_web_order:
                Collections.sort(currentShow, new Comparator<PhonePrice>() {
                    @Override
                    public int compare(PhonePrice lhs, PhonePrice rhs) {
                        return Integer.compare(lhs.id, rhs.id);
                    }
                });
                updateList();
                return true;

            case R.id.show_all:
                currentShow = allPhones;
                updateList();
                return true;

            case R.id.show_apple:
                return showCompany(PhoneCompany.CompanyEnum.Apple);

            case R.id.show_asus:
                return showCompany(PhoneCompany.CompanyEnum.ASUS);

            case R.id.show_htc:
                return showCompany(PhoneCompany.CompanyEnum.hTC);

            case R.id.show_lg:
                return showCompany(PhoneCompany.CompanyEnum.LG);

            case R.id.show_samsung:
                return showCompany(PhoneCompany.CompanyEnum.SAMSUNG);

            case R.id.show_sony:
                return showCompany(PhoneCompany.CompanyEnum.Sony);

            case R.id.show_infocus:
                return showCompany(PhoneCompany.CompanyEnum.InFocus);

            case R.id.show_huawei:
                return showCompany(PhoneCompany.CompanyEnum.HUAWEI);

            case R.id.show_oppo:
                return showCompany(PhoneCompany.CompanyEnum.oppo);

            case R.id.show_mi:
                return showCompany(PhoneCompany.CompanyEnum.MI);

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean showCompany(PhoneCompany.CompanyEnum c) {
        currentShow = compsMap.get(c).phones;
        updateList();
        return true;
    }
}

class PhonePrice {
    public PhonePrice(PhoneCompany c, String n, String p, int i) {
        comp = c;
        name = n;
        price = Integer.parseInt(p);
        id = i;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(comp.company);
        sb.append(" ");
        sb.append(name);

        return sb.toString();
    }

    public String getPriceString() {
        StringBuilder sb = new StringBuilder();
        sb.append("$ ");
        sb.append(price);

        return sb.toString();
    }

    public PhoneCompany comp;
    public String name;
    public int id;

    public int price;
    public ArrayList<Integer> oldPrices = new ArrayList<>();
}

class PhoneCompany {
    public String companyName;
    public ArrayList<PhonePrice> phones = new ArrayList<>();

    public enum CompanyEnum {
        Apple,
        Sony,
        GSmart,
        hTC,
        Inhon,
        Benten,
        HUAWEI,
        ASUS,
        SAMSUNG,
        InFocus,
        NOKIA,
        oppo,
        LG,
        MTO,
        COOLPAD,
        MI,
        UNKNOWN,
    }

    public CompanyEnum company;

    private static HashMap<String, CompanyEnum> companyPicMap = new HashMap<>();

    static  {
        companyPicMap.put("images/prodpt/844pSa.jpg", CompanyEnum.Apple);
        companyPicMap.put("images/prodpt/051k76.jpg", CompanyEnum.Sony);
        companyPicMap.put("images/prodpt/097SwZ.jpg", CompanyEnum.GSmart);
        companyPicMap.put("images/prodpt/149vbE.jpg", CompanyEnum.hTC);
        companyPicMap.put("images/prodpt/167sKc.jpg", CompanyEnum.Inhon);
        companyPicMap.put("images/prodpt/414ZPQ.jpg", CompanyEnum.Benten);
        companyPicMap.put("images/prodpt/502Fc1.jpg", CompanyEnum.HUAWEI);
        companyPicMap.put("images/prodpt/586Q6N.jpg", CompanyEnum.ASUS);
        companyPicMap.put("images/prodpt/593uAM.jpg", CompanyEnum.SAMSUNG);
        companyPicMap.put("images/prodpt/697qw3.jpg", CompanyEnum.InFocus);
        companyPicMap.put("images/prodpt/751lXt.jpg", CompanyEnum.NOKIA);
        companyPicMap.put("images/prodpt/881IQv.jpg", CompanyEnum.oppo);
        companyPicMap.put("images/prodpt/888Pyt.jpg", CompanyEnum.LG);
        companyPicMap.put("images/prodpt/957X0d.jpg", CompanyEnum.MTO);
        companyPicMap.put("images/prodpt/886lV7.jpg", CompanyEnum.COOLPAD);
        companyPicMap.put("images/prodpt/734wY8.gif", CompanyEnum.MI);
    };

    static public PhoneCompany getCompanyFromPicture(String name) {
        PhoneCompany comp = new PhoneCompany();

        CompanyEnum ce = companyPicMap.get(name);

        if (ce != null) {
            comp.company = ce;
        }
        else {
            comp.company = CompanyEnum.UNKNOWN;
        }

        comp.companyName = comp.company.toString();

        return comp;
    }
}