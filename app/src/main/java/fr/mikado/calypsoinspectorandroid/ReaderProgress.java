package fr.mikado.calypsoinspectorandroid;

import android.widget.ArrayAdapter;

/**
 * Created by alexis on 06/04/17.
 */
public class ReaderProgress {

    private String[] lines;
    private ArrayAdapter<String> output;

    public ReaderProgress(ArrayAdapter<String> output, String[] lines){
        this.output = output;
        this.lines = lines;
    }

    public ArrayAdapter<String> getOutput() {
        return output;
    }

    public void setOutput(ArrayAdapter<String> output) {
        this.output = output;
    }

    public String[] getLines() {
        return lines;
    }

    public void setLines(String[] lines) {
        this.lines = lines;
    }
}
