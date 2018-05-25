package club.zhengjiadi.coolweather.gson;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Weather {

    public String status;

    public Basic basic;

    public AQI aqi;

    public Now now;

    public Suggestion suggestion;

    //serializedName 里面的名称要和返回的对应(如果命名一致的话就不需要加这个)
    @SerializedName("daily_forecast")
    public List<Forecast> forecastList;

}
