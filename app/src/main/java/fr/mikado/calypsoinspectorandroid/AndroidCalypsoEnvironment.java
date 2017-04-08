package fr.mikado.calypsoinspectorandroid;

import android.content.Context;
import fr.mikado.calypso.CalypsoEnvironment;
import org.jdom2.Document;

import java.io.IOException;

public class AndroidCalypsoEnvironment extends CalypsoEnvironment {

    private final Context context;

    public AndroidCalypsoEnvironment(Context context){
        super();
        this.context = context;
        this.buildEnvironmentList();
    }

    @Override
    public Document loadDocument(String filename) {
        try {
            return new XMLIOImpl(context, XMLIOImpl.XMLIOType.AssetFile).loadDocument(filename);
        } catch (IOException e) {
            return null;
        }
    }
}
