package fr.mikado.calypsoinspectorandroid;

import android.content.Context;
import android.content.res.AssetManager;
import android.widget.Toast;
import fr.mikado.xmlio.XMLIOInterface;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class XMLIOImpl implements XMLIOInterface {

    public enum XMLIOType {
        InternalStorage, AssetFile
    }

    private XMLIOType type;
    private Context context;

    public XMLIOImpl(Context context, XMLIOType type){
        this.type = type;
        this.context = context;
    }

    @Override
    public Document loadDocument(String filename) throws IOException {
        InputStream is;

        switch (this.type){
            case AssetFile:
                is = this.context.getAssets().open(filename);
                break;
            case InternalStorage:
            default:
                is = this.context.openFileInput(filename);
        }

        Document doc;
        try {
            doc = this.loadDocument(is);
        } catch (JDOMException e) {
            System.out.println("Error while parsing file");
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            System.out.println("Cannot read file \""+filename+"\"");
            System.out.println(e.getMessage());
            return null;
        }
        return doc;
    }

    public Document loadDocument(InputStream is) throws JDOMException, IOException {
        SAXBuilder sax = new SAXBuilder();
        Document doc;
        doc = sax.build(is);
        return doc;
    }

    @Override
    public boolean writeDocument(Document doc, String filename) {
        FileOutputStream fos;
        try {
            fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
        } catch (FileNotFoundException e) {
            Toast.makeText(context, "Could not save XML ! FileNotFoundException.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return writeDocument(doc, fos);
    }

    public boolean writeDocument(Document doc, FileOutputStream fos){
        XMLOutputter outputter = new XMLOutputter();
        outputter.setFormat(Format.getPrettyFormat());
        try {
            outputter.output(doc, fos);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Could not save XML ! IO error.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

}
