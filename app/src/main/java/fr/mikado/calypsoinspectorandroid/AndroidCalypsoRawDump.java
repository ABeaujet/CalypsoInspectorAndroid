package fr.mikado.calypsoinspectorandroid;

import android.content.Context;
import android.widget.Toast;
import fr.mikado.calypso.BitArray;
import fr.mikado.calypso.CalypsoEnvironment;
import fr.mikado.calypso.CalypsoFile;
import fr.mikado.calypso.CalypsoRawDump;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.*;
import java.util.ArrayList;

import static fr.mikado.calypso.CalypsoFile.CalypsoFileType.EF;

/*

fr.mikado.calypso.CalypsoRawDump est maintenant portable avec XMLIOInterface !

public class AndroidCalypsoRawDump extends CalypsoRawDump{

    private Context context;

    public AndroidCalypsoRawDump(Context context, CalypsoEnvironment env) {
        super(env);
    }

    public void writeXML(String filename){
        Document doc = this.getXMLDump();
        XMLIOImpl xmlio = new XMLIOImpl(context, XMLIOImpl.XMLIOType.InternalStorage);

        if(xmlio.writeDocument(doc, filename))
            Toast.makeText(context, "Saved as " + filename, Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(context, "Could not save XML ! IO error.", Toast.LENGTH_SHORT).show();
    }

    public void loadXML(String filename) throws IOException {
        Document dump = new XMLIOImpl(context, XMLIOImpl.XMLIOType.InternalStorage).loadDocument(filename);
        if(dump == null){
            Toast.makeText(context, "Could not load the dump !", Toast.LENGTH_SHORT).show();
            return;
        }
        Element root = dump.getRootElement();

        this.dump = new ArrayList<>();
        for(Element f : root.getChildren("file"))
            this.dump.add(new CalypsoFileDump(f));
    }

}
*/
