package fr.mikado.calypsoinspectorandroid;

import android.content.Context;
import fr.mikado.calypso.CalypsoEnvironment;
import fr.mikado.calypso.CalypsoFile;
import fr.mikado.calypso.CalypsoRecord;
import fr.mikado.calypso.CalypsoRecordField;
import org.jdom2.Document;
import org.jdom2.Element;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import static fr.mikado.calypso.CalypsoFile.CalypsoFileType.EF;

/**
 * Created by alexis on 06/04/17.
 */
public class CalypsoDump {

    private CalypsoEnvironment env;
    private ArrayList<String> dumpTreeHeaders;
    private HashMap<String, ArrayList<String>> dumpTreeChildren;

    public CalypsoDump(CalypsoEnvironment env){
        this.env = env;
        this.dumpTreeChildren = new HashMap<>();
        this.dumpTreeHeaders = new ArrayList<>();
    }

    public void dumpTrips(String headerName){
        this.dumpTreeChildren.put(headerName, dumpTrips(this.env));
        this.dumpTreeHeaders.add(headerName);
    }

    public void dumpProfiles(String headerName){
        this.dumpTreeChildren.put(headerName, dumpProfiles(this.env));
        this.dumpTreeHeaders.add(headerName);
    }

    public void dumpContracts(String headerName){
        this.dumpTreeChildren.put(headerName, dumpContracts(this.env));
        this.dumpTreeHeaders.add(headerName);
    }

    public void dumpHolder(String headerName){
        ArrayList<String> holderArray = new ArrayList<>();
        holderArray.add(dumpHolder(this.env));
        this.dumpTreeChildren.put(headerName, holderArray);
        this.dumpTreeHeaders.add(headerName);
    }

    public CalypsoDumpListAdapter getListAdapter(Context context){
        return new CalypsoDumpListAdapter(context, this.dumpTreeHeaders, this.dumpTreeChildren);
    }

    public static ArrayList<String> dumpTrips(CalypsoEnvironment env) {
        ArrayList<String> res = new ArrayList<>();
        CalypsoFile events = env.getFile("Events");
        if(events == null){
            res.add("Events file not loaded");
            return res;
        }
        CalypsoFile contracts = env.getFile("Contracts");

        for(CalypsoRecord rec : events.getRecords()){
            // basic event details
            String date = rec.getRecordField("Event Date").getConvertedValue();
            String time = rec.getRecordField("Event Time").getConvertedValue();
            CalypsoRecordField event = rec.getRecordField("Event");
            String code = event.getSubfield("EventCode").getConvertedValue();
            String stop = event.getSubfield("EventLocationId").getConvertedValue();
            String route = event.getSubfield("EventRouteNumber").getConvertedValue();
            String direction = event.getSubfield("EventData").getSubfield("EventDataRouteDirection").getConvertedValue();

            String fare = "<Contracts not loaded>";
            if(contracts != null) {
                // which contract for this event ?
                fare = "Error while decoding contract pointer. - Dog, probably.";
                int farePointer = event.getSubfield("EventContractPointer").getBits().getInt();
                int contractIndex = env.getContractIndex(farePointer);
                if (contractIndex >= 0) {
                    CalypsoRecord contract = contracts.getRecords().get(contractIndex);
                    CalypsoRecordField contractBitmap = contract.getRecordField("PublicTransportContractBitmap");
                    CalypsoRecordField contractType = contractBitmap.getSubfield("ContractType");
                    fare = contractType.getConvertedValue();
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append("  Date    : " + date + " " + time + "\n");
            sb.append("  Arrêt   : " + stop + "\n");
            sb.append("  Ligne   : " + route + "\n");
            sb.append("  Sens    : " + direction + "\n");
            sb.append("  Code    : " + code + "\n");
            sb.append("  Contrat : " + fare);

            res.add(sb.toString());
        }
        return res;
    }

    public static ArrayList<String> dumpProfiles(CalypsoEnvironment env){
        ArrayList<String> res = new ArrayList<>();
        CalypsoFile envHolder= env.getFile("Environment, Holder");
        if(envHolder == null) {
            res.add("File not loaded");
            return res;
        }
        CalypsoRecord envHolderRec = envHolder.getRecords().get(0);
        if(envHolderRec == null) {
            res.add("Record #0 not found");
            return res;
        }
        CalypsoRecordField holderProfiles = envHolderRec.getRecordField("Holder Bitmap").getSubfield("Holder Profiles(0..4)");

        for(CalypsoRecordField f : holderProfiles.getSubfields()) {
            if(!f.isFilled()) {
                res.add("No profiles.");
                break;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(" Label        : " + f.getSubfield("Profile Number").getConvertedValue() + "\n");
            sb.append(" Profile date : " + f.getSubfield("Profile Date").getConvertedValue());
            res.add(sb.toString());
        }
        return res;
    }

    public static ArrayList<String> dumpContracts(CalypsoEnvironment env){
        ArrayList<String> res = new ArrayList<>();
        DateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE);
        Date now = new Date();

        CalypsoFile contracts = env.getFile("Contracts");
        if(contracts != null) {
            // which contract for this event ?
            for(CalypsoRecord contract : contracts.getRecords()) {
                CalypsoRecordField contractBitmap = contract.getRecordField("PublicTransportContractBitmap");
                CalypsoRecordField contractType = contractBitmap.getSubfield("ContractType");
                CalypsoRecordField contractStatus= contractBitmap.getSubfield("ContractStatus");
                CalypsoRecordField contractValidity = contractBitmap.getSubfield("ContractValidityInfo");
                CalypsoRecordField contractStart = contractValidity.getSubfield("ContractValidityStartDate");
                CalypsoRecordField contractEnd = contractValidity.getSubfield("ContractValidityEndDate");
                String contractEndStr = contractEnd.getConvertedValue();

                StringBuilder sb = new StringBuilder();
                sb.append("   Label      : " + contractType.getConvertedValue() + "\n");
                sb.append("   Start date : " + contractStart.getConvertedValue() + "\n");
                if(contractEndStr.length() > 0) {
                    String exp = "   End date   : " + contractEndStr;
                    try {
                        if (now.after(format.parse(contractEndStr)))
                            exp += " (EXPIRED)\n";
                        else
                            exp += "\n";
                    } catch (ParseException e) {
                    }
                    sb.append(exp);
                }
                sb.append("   Status     : " + contractStatus.getConvertedValue());
                res.add(sb.toString());
            }
        }else
            res.add("Contracts file not loaded");
        return res;
    }

    public static String dumpHolder(CalypsoEnvironment env){
        CalypsoFile envHolderFile = env.getFile("Environment, Holder");
        if(envHolderFile == null)
            return "EnvHolder file not loaded";
        CalypsoRecord envHolder = envHolderFile.getRecords().get(0);
        if(envHolder == null)
            return "Record #0 not found";
        CalypsoRecordField holder = envHolder.getRecordField("Holder Bitmap");
        if(holder == null)
            return "Holder bitmap not found";

        CalypsoRecordField holderName = holder.getSubfield("Holder Name");
        String holderSurname  = holderName.getSubfield("Holder Surname").getConvertedValue();
        String holderForename = holderName.getSubfield("Holder Forename").getConvertedValue();

        CalypsoRecordField holderBirth = holder.getSubfield("Holder Birth");
        String holderBirthDate  = holderBirth.getSubfield("Birth Date").getConvertedValue();
        String holderBirthPlace = holderBirth.getSubfield("Birth Place").getConvertedValue();

        String holderIdNumber = holder.getSubfield("Holder Id Number").getConvertedValue();
        String holderCompany  = holder.getSubfield("Holder Company").getConvertedValue();

        CalypsoRecordField holderData = holder.getSubfield("Holder Data");
        String holderCardStatus  = holderData.getSubfield("HolderDataCardStatus").getConvertedValue();
        String holderStudyPlace  = holderData.getSubfield("HolderDataStudyPlace").getConvertedValue();

        String[] holderProfileStartDates = new String[4];
        for(int i = 1;i<5;i++)
            holderProfileStartDates[i-1]  = holderData.getSubfield("HolderDataProfileStartDate"+i).getConvertedValue();

        StringBuilder sb = new StringBuilder();
        if(holderForename.length() > 0)
            sb.append("Forename    : " + holderForename + "\n");
        if(holderSurname.length() > 0)
            sb.append("Surname     : " + holderSurname + "\n");
        if(holderBirthDate.length() > 0)
            sb.append("Birth date  : " + holderBirthDate + "\n");
        if(holderBirthPlace.length() > 0)
            sb.append("Birth place : " + holderBirthPlace + "\n");
        if(holderIdNumber.length() > 0)
            sb.append("Id number   : " + holderIdNumber + "\n");
        if(holderCompany.length() > 0)
            sb.append("Company     : " + holderCompany + "\n");
        if(holderStudyPlace.length() > 0)
            sb.append("Study place : " + holderStudyPlace + "\n");
        if(holderCardStatus.length() > 0)
            sb.append("CardStatus  : " + holderCardStatus + "\n");

        return sb.toString();
    }
}
