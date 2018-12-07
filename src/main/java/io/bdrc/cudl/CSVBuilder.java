package io.bdrc.cudl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.json.XML;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVWriter;

public class CSVBuilder {

    public static String[] headers=
                         {"rid",
                          "abstractText",
                          "abstractShort",
                          "title",
                          "manifestUrl",
                          "mainTitle",
                          "altTitle",
                          "material_support"};

    public static String[] getCsvLineAsList(String id) throws IOException{
        String[] csvLine=new String[8];
        JsonNode node=CUDLData.getNodeFromFile(CUDLData.rootDir+"/json/"+id+".json");
        csvLine[0]= id;
        csvLine[1] = node.findValue("abstractText").get(0).asText();
        csvLine[2]= node.findValue("abstractShort").get(0).asText();
        csvLine[3]= node.findValue("title").get(0).asText();
        return AddXmlData(csvLine,id);
    }

    public static String[] AddXmlData(String[] csvLine, String id) throws IOException {
        String xml=CUDLData.getXMLFileContents(CUDLData.rootDir+"/xml/"+id+".xml");
        String json=XML.toJSONObject(xml).toString();
        ObjectMapper mapper=new ObjectMapper();
        JsonNode node=mapper.readTree(json);
        //Should we process this record (does it have images ?)
        if(node.at("/TEI/facsimile").findValue("graphic")!=null) {
            csvLine[4]="https://cudl.lib.cam.ac.uk/iiif/"+id;
            String alt="";
            List<JsonNode> title=node.at("/TEI/teiHeader/fileDesc/sourceDesc/msDesc/msContents/msItem").findValues("title");
            for(JsonNode n:title.get(0)) {
                if(n.findValue("type")==null) {
                    if(n.findValue("content")!=null) {
                        csvLine[5]=n.findValue("content").asText();
                    }
                }else {
                    alt=alt+","+n.findValue("content").asText();
                }
            }
            if(!alt.equals("")) {
                csvLine[6]=alt.substring(1);
            }
            JsonNode support=node.at("/TEI/teiHeader/fileDesc/sourceDesc/msDesc/physDesc/objectDesc/supportDesc/material");
            csvLine[7]=support.asText();
            return csvLine;
        }
        return null;
    }

    public static void buildCsv() throws IOException {
        List<String> files=getJsonFiles();
        CSVWriter writer = new CSVWriter(new FileWriter(CUDLData.rootDir+"cudl.csv"));
        writer.writeNext(headers);
        for(String s:files) {
            if(s.endsWith(".json")) {
                String[] list= getCsvLineAsList(s.substring(0, s.indexOf(".")));
                if(list!=null) {
                    writer.writeNext(list);
                }
            }
        }
        writer.close();
    }

    private static List<String> getJsonFiles(){
        List<String> files=null;
        File file = new File(CUDLData.rootDir+"json");
        if (file.isDirectory()) {
            String names[] = file.list();
            files=Arrays.asList(names);
        }
        return files;
    }

    public static void main(String[] args) throws IOException {
        /*System.out.println(CSVBuilder.getCsvLineAsList("MS-ADD-01405"));
        System.out.println(CSVBuilder.getCsvLineAsList("MS-ADD-01333"));
        System.out.println(CSVBuilder.getJsonFiles());*/
        CSVBuilder.buildCsv();
    }

}
