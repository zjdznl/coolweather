package club.zhengjiadi.coolweather;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import club.zhengjiadi.coolweather.db.City;
import club.zhengjiadi.coolweather.db.County;
import club.zhengjiadi.coolweather.db.Province;
import club.zhengjiadi.coolweather.util.HttpUtil;
import club.zhengjiadi.coolweather.util.Utility;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;


    private ProgressDialog progressDialog;
    private TextView titleText;
    private Button backButton;
    private ListView listView;

    private ArrayAdapter<String> adapter;//todo
    private List<String> dataList = new ArrayList<>();

    private List<Province> provinceList;
    private List<City> cityList;
    private List<County> countyList;

    private Province selectedProvince;
    private City selectedCity;

    private int currentLevel;


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);

        titleText = view.findViewById(R.id.title_text);
        backButton = view.findViewById(R.id.back_button);
        listView = view.findViewById(R.id.list_view);

        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (currentLevel) {
                    case LEVEL_PROVINCE:
                        selectedProvince = provinceList.get(position);
                        queryCities();
                        break;
                    case LEVEL_CITY:
                        selectedCity = cityList.get(position);
                        queryCounties();
                        break;
                    case LEVEL_COUNTY:
                        String weatherId = countyList.get(position).getWeatherId();
                        if (getActivity() instanceof MainActivity) {
                            Intent intent = new Intent(getActivity(), WeatherActivity.class);
                            intent.putExtra("weather_id", weatherId);
                            startActivity(intent);
                            getActivity().finish();
                        }

                        break;
                }
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (currentLevel) {
                    case LEVEL_COUNTY:
                        queryCities();
                        break;
                    case LEVEL_CITY:
                        queryProvinces();
                        break;
                }
            }
        });
        queryProvinces();
    }

    private void queryProvinces() {
        titleText.setText("China");
        backButton.setVisibility(View.GONE);
        provinceList = DataSupport.findAll(Province.class);
        if (provinceList.size() > 0) {
            dataList.clear();
            for (Province province :
                    provinceList) {
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        } else {
            String address = "http://guolin.tech/api/china";
            queryFromServer(address, "province");
        }
    }

    private void queryCities() {
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList = DataSupport.where("provinceid = ?", String.valueOf(selectedProvince.getId()))
                .find(City.class);
        if (cityList.size() > 0) {
            dataList.clear();
            for (City city :
                    cityList) {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFromServer(address, "city");
        }
    }

    private void queryCounties() {
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = DataSupport.where("cityid = ?", String.valueOf(selectedCity.getId()))
                .find(County.class);
        if (countyList.size() > 0) {
            dataList.clear();
            for (County county :
                    countyList) {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        } else {
//            Log.d("fuck", "向服务器查询 country");
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" + cityCode;
//            Log.d("fuck", address);
            queryFromServer(address, "county");
        }
    }

    private void queryFromServer(String address, final String type) {
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @RequiresApi(api = Build.VERSION_CODES.M)
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).
                                show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;

                switch (type) {
                    case "province":
                        result = Utility.handleProvinceResponse(responseText);
                        break;
                    case "city":
                        result = Utility.handleCityResponse(responseText, selectedProvince.getId());
                        break;
                    case "county":
                        result = Utility.handleCountryResponse(responseText, selectedCity.getId());
                        break;
                }

                if (result) {
                    getActivity().runOnUiThread(new Runnable() {
                        @RequiresApi(api = Build.VERSION_CODES.M)
                        @Override
                        public void run() {
                            closeProgressDialog();

                            switch (type) {
                                case "province":
                                    queryProvinces();
                                    break;
                                case "city":
                                    queryCities();
                                    break;
                                case "county":
                                    queryCounties();
                                    break;
                            }
                        }
                    });
                }

            }
        });
    }

    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("Loading...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    private void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

}

