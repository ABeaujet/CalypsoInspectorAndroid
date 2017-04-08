package fr.mikado.calypsoinspectorandroid;

import android.os.AsyncTask;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import fr.mikado.calypso.CalypsoEnvironment;
import fr.mikado.isodep.IsoDepInterface;

/**
 * Created by alexis on 06/04/17.
 */
public class ReaderTask extends AsyncTask<IsoDepInterface, ReaderProgress, Void> {

    private CalypsoEnvironment env;

    public ReaderTask(CalypsoEnvironment env){
        this.env = env;
    }

    @Override
    protected Void doInBackground(IsoDepInterface... isoDep) {

        // On lit la carte.


        return null;
    }

    protected void onProgressUpdate(ReaderProgress progress){
        ArrayAdapter<String> output = progress.getOutput();
        output.addAll(progress.getLines());
        for(String s : progress.getLines())
            System.out.println(s);
    }
}
