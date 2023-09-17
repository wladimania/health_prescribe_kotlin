package com.example.health_prescribe

import android.os.AsyncTask
import com.example.health_prescribe.model.farmacoCompleto

class FetchDrugsTask(val context: SelectDrugActivity) : AsyncTask<Void, Void, List<farmacoCompleto>>() {

    override fun doInBackground(vararg params: Void?): List<farmacoCompleto> {
        return DatabaseConnection.fetchDrugsFromDB()
    }

    override fun onPostExecute(result: List<farmacoCompleto>?) {
        super.onPostExecute(result)
        if (result != null) {
            context.displayDrugs(result)
        }
    }
}
