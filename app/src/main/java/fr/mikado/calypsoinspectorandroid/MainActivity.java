package fr.mikado.calypsoinspectorandroid;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.*;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.acs.smartcard.Reader;
import fr.mikado.calypso.*;
import fr.mikado.isodep.CardException;
import fr.mikado.isodep.CommandAPDU;
import fr.mikado.isodep.IsoDepInterface;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private NfcAdapter nfc;
    private AndroidCalypsoEnvironment calypsoEnv;

    private Context context;
    private TextView statusLabel;
    private Spinner networksSpinner;
    private ExpandableListView treeView;
    private Button saveButton;
    private Button loadButton;
    private DateFormat dumpDateFormat;

    // ACS SHIT
    private Reader acsReader;
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private UsbManager usbManager;
    private static final String[] stateStrings = {"Unknown", "Absent", "Present", "Swallowed", "Powered", "Negotiable", "Specific"};
    private PendingIntent usbPersmissionIntent;
    private BroadcastReceiver acsReceiver;

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actionbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        switch(item.getItemId()){
            case R.id.moreNetworksMenuItem:
                builder.setIcon(R.mipmap.ic_launcher);
                builder.setTitle(R.string.app_name);
                builder.setView(getLayoutInflater().inflate(R.layout.niy, null, false));
                builder.create();
                builder.show();
                break;
            case R.id.aboutMenuItem:
                builder.setIcon(R.mipmap.ic_launcher);
                builder.setTitle(R.string.app_name);
                builder.setView(getLayoutInflater().inflate(R.layout.about, null, false));
                builder.create();
                builder.show();
                break;
            case R.id.clearMenuItem:
                this.treeView.setAdapter(new CalypsoDumpListAdapter(this, new ArrayList<String>(), new HashMap<String, ArrayList<String>>()));
                break;
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.dumpDateFormat = new SimpleDateFormat("dd-MM-yyy_hh-mm-ss");
        this.context = this.getApplicationContext();
        this.calypsoEnv = new AndroidCalypsoEnvironment(this.context);

        this.treeView = (ExpandableListView)findViewById(R.id.tree_view);

        this.networksSpinner = (Spinner)findViewById(R.id.networksSpinner);
        ArrayAdapter<String> networksSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, this.calypsoEnv.getAvailableNetworks());
        networksSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.networksSpinner.setAdapter(networksSpinnerAdapter);
        this.networksSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                statusLabel.setText("Loading network config...");
                ArrayList<String> networks = calypsoEnv.getAvailableNetworks();
                if(position >= networks.size())
                    return;
                try {
                    System.out.println("Loading network : " + networks.get(position));
                    calypsoEnv.clear();
                    calypsoEnv.loadEnvironment(networks.get(position));
                    statusLabel.setText(R.string.net_loaded_waiting_card);
                    Toast.makeText(getApplicationContext(), "Network loaded", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        this.networksSpinner.setSelection(1);

        this.saveButton = (Button)findViewById(R.id.saveButton);
        this.saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ListAdapter adapter = treeView.getAdapter();
                if(adapter == null || adapter.isEmpty()){
                    Toast.makeText(v.getContext(), "Nothing to save !", Toast.LENGTH_SHORT).show();
                    return;
                }
                String filename = dumpDateFormat.format(new Date()) + ".dump.xml";
                try {
                    XMLIOImpl xmlio = new XMLIOImpl(MainActivity.this, XMLIOImpl.XMLIOType.InternalStorage);
                    new CalypsoRawDump(calypsoEnv).writeXML(xmlio, filename);
                } catch (IOException e) {
                    Toast.makeText(v.getContext(), "IO: Could not save the dump !", Toast.LENGTH_SHORT).show();
                }
                Toast.makeText(v.getContext(), "Dump saved as " + filename, Toast.LENGTH_SHORT).show();
            }
        });

        this.loadButton = (Button)findViewById(R.id.loadButton);

        this.loadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File dir = getFilesDir();
                final File[] dumps = dir.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String filename) {
                        return filename.toLowerCase().endsWith(".dump.xml");
                    }
                });
                if(dumps.length == 0){
                    Toast.makeText(v.getContext(), "Cannot find any saved dumps.", Toast.LENGTH_SHORT).show();
                    return;
                }
                final CharSequence[] dumpList = new CharSequence[dumps.length];
                for(int i = 0;i<dumps.length;i++)
                    dumpList[i] = dumps[i].getName().substring(0, dumps[i].getName().length()-9);
                final AlertDialog.Builder saveLoader = new AlertDialog.Builder(v.getContext());
                saveLoader.setTitle("Load a saved dump");
                saveLoader.setItems(dumpList, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            calypsoEnv.purgeFilesContents();
                            treeView.setAdapter(new CalypsoDumpListAdapter(context, new ArrayList<String>(), new HashMap<String, ArrayList<String>>()));
                            CalypsoRawDump dumpXML = new CalypsoRawDump(calypsoEnv);
                            dumpXML.loadXML(new XMLIOImpl(MainActivity.this, XMLIOImpl.XMLIOType.InternalStorage), dumps[which].getName());

                            calypsoEnv.loadDump(dumpXML);
                            CalypsoDump dump = new CalypsoDump(calypsoEnv);
                            dump.dumpHolder("Holder");
                            dump.dumpProfiles("Profiles");
                            dump.dumpContracts("Contracts");
                            dump.dumpTrips("Trips");
                            treeView.setAdapter(dump.getListAdapter(MainActivity.this));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                saveLoader.setNegativeButton("Clear", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AlertDialog.Builder confirm = new AlertDialog.Builder(MainActivity.this);
                        confirm.setTitle("Delete all saved dumps ?");
                        confirm.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        confirm.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface confirmDial, int which) {
                                File[] dumps = getFilesDir().listFiles(new FilenameFilter() {
                                    @Override
                                    public boolean accept(File dir, String filename) {
                                        return filename.toLowerCase().endsWith(".dump.xml");
                                    }
                                });
                                for(File f : dumps)
                                    f.delete();
                                confirmDial.dismiss();
                            }
                        });
                        confirm.show();
                    }
                });
                saveLoader.show();
            }
        });

        if(this.calypsoEnv == null){
            Toast.makeText(this, "Environment not loaded !", Toast.LENGTH_SHORT).show();
            finish();
        }

        Switch externalSwitch = (Switch) findViewById(R.id.externalSwitch);
        externalSwitch.setEnabled(false);
        TextView activeReaderLabel = (TextView) findViewById(R.id.activeReaderLabel);
        activeReaderLabel.setText("None");
        this.statusLabel = (TextView) findViewById(R.id.statusLabel);
        this.statusLabel.setText("Initializing...");

        // ONBOARD NFC
        this.nfc = NfcAdapter.getDefaultAdapter(this);
        if(this.nfc == null || !this.nfc.isEnabled()){
            Toast.makeText(this, "NFC is not enabled !", Toast.LENGTH_SHORT).show();
            finish();
        }

        // ACS READERS
        this.usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        this.acsReader = new Reader(this.usbManager);
        this.acsReader.setOnStateChangeListener(new Reader.OnStateChangeListener() {
            @Override
            public void onStateChange(int slotNum, int prevState, int currState) {
                if(prevState < Reader.CARD_UNKNOWN || prevState > Reader.CARD_SPECIFIC)
                    prevState = Reader.CARD_UNKNOWN;
                if(prevState < Reader.CARD_UNKNOWN || prevState > Reader.CARD_SPECIFIC)
                    prevState = Reader.CARD_UNKNOWN;

                final String logString = "Slot "+slotNum+" : "+stateStrings[prevState]+" > " + stateStrings[currState];
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        logMsg(logString);
                    }
                });
            }
        });

        this.acsReceiver = new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent == null)
                    return;
                String action = intent.getAction();
                if(ACTION_USB_PERMISSION.equals(action)){
                    synchronized (this){
                        UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        if(intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            if (device != null) {
                                //Open reader
                                logMsg("Opening reader " + device.getDeviceName() + "...");
                            }
                        }else
                            logMsg("Permission denied for device " + device.getDeviceName()+ ".");
                    }
                }
            }
        };

        this.usbPersmissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(acsReceiver, filter);

        externalSwitch.setEnabled(true);
        externalSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    UsbManager usbMgr = (UsbManager)getSystemService("usb");
                    UsbDevice readerDev = null;
                    for(UsbDevice dev : usbMgr.getDeviceList().values())
                        if(dev.getVendorId() == 0x072f){
                            readerDev = dev;
                            break;
                        }
                    Toast t = Toast.makeText(getApplicationContext(), "lel", Toast.LENGTH_SHORT);
                    t.setDuration(Toast.LENGTH_SHORT);
                    if(readerDev == null){
                        t.setText("Reader unplugged");
                        buttonView.setChecked(false);
                    }else{
                        t.setText("Reader found !");
                        MainActivity.this.statusLabel.setText("External NFC reader");
                        buttonView.setChecked(true);
                    }
                    t.show();
                }else{
                    Toast.makeText(getApplicationContext(), "Reader deselected", Toast.LENGTH_SHORT).show();
                    MainActivity.this.statusLabel.setText("Onboard NFC reader");
                }
            }
        });

        activeReaderLabel.setText("Onbard NFC reader");
    }

    private void logMsg(String message){
        DateFormat fmt = new SimpleDateFormat("dd-MM-yyy HH:mm:ss : ");
        System.out.println(fmt.format(new Date()) + message);
    }

    @Override
    protected void onResume(){
        super.onResume();
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, this.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        this.nfc.enableForegroundDispatch(this, pendingIntent, null, null);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        unregisterReceiver(this.acsReceiver);
    }


    private void handleIsoDep(IsoDepInterface iso){
        this.calypsoEnv.purgeFilesContents();
        try {
            CalypsoCard c = new CalypsoCard(iso, this.calypsoEnv);
            c.read();
            c.dump(false);
        } catch (CardException e) {
            System.out.println("Exception while reading : " + e.getMessage());
            Toast.makeText(this, "Don't move while I read the card !", Toast.LENGTH_SHORT).show();
            return;
        }

        CalypsoDump dump = new CalypsoDump(this.calypsoEnv);
        dump.dumpHolder("Holder");
        dump.dumpProfiles("Profiles");
        dump.dumpContracts("Contracts");
        dump.dumpTrips("Trips");
        this.treeView.setAdapter(dump.getListAdapter(this));
    }

    @Override
    protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        if(intent == null)
            return;

        if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Toast.makeText(this, "NFC tag discovered", Toast.LENGTH_SHORT).show();
            Tag card = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            TextView message = (TextView) findViewById(R.id.statusLabel);
            message.setText(getString(R.string.card_found));

            IsoDep isodep = IsoDep.get(card);
            if(isodep == null){
                Toast.makeText(this, "This is not an NFC Calypso card !", Toast.LENGTH_SHORT).show();
                message.setText(R.string.net_loaded_waiting_card);
                return;
            }
            try {
                isodep.connect();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            if(isodep.isConnected()) {
                OnboardIsoDepImpl iso = new OnboardIsoDepImpl(isodep);
                // check if calypso card
                /* This may be exclusive to PassPass cards. No documentation on that so let's remove it.
                try {
                    byte[] rdata = iso.transmit(new CommandAPDU(new byte[]{0x00, 0x10, 0x00, 0x00, 0x00})).getData();
                    // rdata[45] : Celego 2D
                    // rdata[52] : Celego 34
                    // rdata[53] : Celego 35
                    // rdata[54] : Celego Chip Type
                    // rdata[55] : Celego ROM Version
                    if(rdata[54] != 0x3C || rdata[53] != 0x00){
                        Toast.makeText(this, "This is not an NFC Calypso card !", Toast.LENGTH_SHORT).show();
                        message.setText(getString(R.string.net_loaded_waiting_card));
                        return;
                    }
                } catch (CardException e) {
                    Toast.makeText(this, "Error while checking if Calypso card", Toast.LENGTH_SHORT).show();
                }*/

                handleIsoDep(iso);
                message.setText(getString(R.string.card_read_waiting));
            }else
                message.setText(getString(R.string.cannot_connect_isodep));
        }
    }
}
