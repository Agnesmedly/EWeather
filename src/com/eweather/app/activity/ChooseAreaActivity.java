package com.eweather.app.activity;

import java.net.ResponseCache;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.List;

import com.eweather.app.model.City;
import com.eweather.app.model.County;
import com.eweather.app.model.EWeatherDB;
import com.eweather.app.model.Province;
import com.eweather.app.util.HttpCallbackListener;
import com.eweather.app.util.HttpUtil;
import com.eweather.app.util.Utility;
import com.eweather.app.*;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ChooseAreaActivity extends Activity {
	public static final int LEVEL_PROVINCE = 0;
	public static final int LEVEL_CITY = 1;
	public static final int LEVEL_COUNTY = 2;
	private List<String> dataList = new ArrayList<String>();
	private List<Province> provinceList;
	private List<City> cityList;
	private List<County> countyList;
	private ListView listView;
	private TextView titleText;
	private ArrayAdapter<String> adapter;
	private EWeatherDB eweatherDB;
	private int currentLevel;
	private Province selectedProvince;
	private City selectedCity;
	private ProgressDialog progressDialog;
	private boolean isFromWeatherActivity;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.choose_area);
		isFromWeatherActivity = getIntent().getBooleanExtra("from_weather_activity", false);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if(prefs.getBoolean("city_selected", false)){
			Intent intent = new Intent(this,WeatherActivity.class);
			startActivity(intent);
			finish();
			return;
		}

		listView = (ListView) findViewById(R.id.list_View);
		titleText = (TextView) findViewById(R.id.title_text);
		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, dataList);
		listView.setAdapter(adapter);
		eweatherDB = EWeatherDB.getInstance(this);

		Log.v("TAG","db");
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int index,
					long arg3) {
				// TODO Auto-generated method stub
				if (currentLevel == LEVEL_PROVINCE) {
					selectedProvince = provinceList.get(index);
					queryCities();
				} else if (currentLevel == LEVEL_CITY) {
					selectedCity = cityList.get(index);
					queryCounties();
				}else if(currentLevel == LEVEL_COUNTY){
					String countyCode = countyList.get(index).getCountyCode();
					Intent intent = new Intent(ChooseAreaActivity.this,WeatherActivity.class);
					intent.putExtra("county_code", countyCode);
					startActivity(intent);
					finish();
				}
			}

		});
		queryProvinces();
	}

	private void queryProvinces() {
		// TODO Auto-generated method stub

		Log.v("TAG","province");
		provinceList = eweatherDB.loadProvinces();
		if(provinceList.size() > 0){
			dataList.clear();

			Log.v("TAG","clear");
			for(Province province : provinceList){

				Log.v("TAG","provincelist");
				dataList.add(province.getProvinceName());

				Log.v("TAG","provinceadd");
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText("中国");
			currentLevel = LEVEL_PROVINCE;
		}
		else{
			queryFromServer(null,"province");
		}
	}

	private void queryCounties() {
		// TODO Auto-generated method stub
		cityList = eweatherDB.loadCities(selectedProvince.getId());
		if(cityList.size() > 0){
			dataList.clear();
			for(City city:cityList){
				dataList.add(city.getCityName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedProvince.getProvinceName());
			currentLevel = LEVEL_CITY;
		}
		else{
			queryFromServer(selectedProvince.getProvinceCode(),"city");
		}
	}

	private void queryCities() {
		// TODO Auto-generated method stub
		countyList = eweatherDB.loadCounties(selectedCity.getId());
		if(countyList.size() > 0){
			dataList.clear();
			for(County county:countyList){
				dataList.add(county.getCountyName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedCity.getCityName());
			currentLevel = LEVEL_COUNTY;
		}
		else{
			queryFromServer(selectedCity.getCityCode(),"county");
		}
	}

	private void queryFromServer(final String code, final String type) {
		// TODO Auto-generated method stub
		String address;
		if(!TextUtils.isEmpty(code)){
			address = "http://www.weather.com.cn/data/list3/city" + code + ".xml";
		}
		else{
			address = "http://www.weather.com.cn/data/list3/city.xml";
		}
		showProgressDialog();
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
			
			@Override
			public void onFinish(String response) {
				// TODO Auto-generated method stub
				boolean result = false;
				if("provinces".equals(type)){
					result = Utility.handleProvincesResponse(eweatherDB, response);
				}
				else if("city".equals(type)){
					result = Utility.handleCitiesResponse(eweatherDB, response, selectedProvince.getId());
				}
				else if("county".equals(type)){
					result = Utility.handleCountiesResponse(eweatherDB, response, selectedCity.getId());
				}
				if(result){
					runOnUiThread(new Runnable(){

						@Override
						public void run() {
							// TODO Auto-generated method stub
							closeProgressDialog();
							if("province".equals(type)){
								queryProvinces();
							}
							else if("city".equals(type)){
								queryCities();
							}
							else if("county".equals(type)){
								queryCounties();
							}
						}
						
					});
				}
			}
			
			@Override
			public void onError(Exception e) {
				// TODO Auto-generated method stub
				runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						// TODO Auto-generated method stub
						closeProgressDialog();
						Toast.makeText(ChooseAreaActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
					}

					
				});
			}
		});
	}

	private void showProgressDialog() {
		// TODO Auto-generated method stub
		if(progressDialog == null){
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("正在加載....");
			progressDialog.setCanceledOnTouchOutside(false);
		}
		progressDialog.show();
	}
	private void closeProgressDialog() {
		// TODO Auto-generated method stub
		if(progressDialog != null){
			progressDialog.dismiss();
		}
	}

	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		if(currentLevel == LEVEL_COUNTY){
			queryCities();
		}
		else if(currentLevel == LEVEL_CITY){
			queryProvinces();
		}
		else{
			if(isFromWeatherActivity){
				Intent intent = new Intent(this,WeatherActivity.class);
				startActivity(intent);
			}
			finish();
		}
	}
	
}
