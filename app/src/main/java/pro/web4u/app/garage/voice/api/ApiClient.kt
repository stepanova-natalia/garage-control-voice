package pro.web4u.app.garage.voice.api

import android.content.Context
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private var retrofit: Retrofit? = null
    private var currentUrl: String? = null

    fun getApi(context: Context): GarageApi {
        val url = ServerSettings.getServerUrl(context)
        if(url != currentUrl || retrofit == null){
            currentUrl = url
            retrofit = Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!.create(GarageApi::class.java)
    }
}