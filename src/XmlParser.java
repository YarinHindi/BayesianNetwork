import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XmlParser {

    public static boolean fileToNetwork(String fileName, HashMap<String, BayesianNode> netWork) {
        try {
            File xmlDoc = new File(fileName);
            DocumentBuilderFactory dbFact = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuild = dbFact.newDocumentBuilder();
            Document doc = dBuild.parse(xmlDoc);
            updateNodeNamesAndOutComes(doc,netWork);
            updateNodeParentsChildrenAndCpt(doc,netWork);
            return true;
        } catch (Exception e) {
            return false;
        }

    }

    private static void updateNodeNamesAndOutComes(Document doc, HashMap<String, BayesianNode> netWorkNodes) {
        NodeList variables = doc.getElementsByTagName("VARIABLE");
        for (int i = 0; i < variables.getLength(); i++) {
            Node nNode = variables.item(i);
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element) nNode;
                // name of the variable
                String name = eElement.getElementsByTagName("NAME").item(0).getTextContent();
                ArrayList<String> outcomes = new ArrayList<>();
                // outcomes of the variable
                for (int j = 0; j < eElement.getElementsByTagName("OUTCOME").getLength(); j++) {
                    String outcome = eElement.getElementsByTagName("OUTCOME").item(j).getTextContent();
                    outcomes.add(outcome);
                }
                netWorkNodes.put(name, new BayesianNode(name, outcomes));
            }
        }
    }

    private static void updateNodeParentsChildrenAndCpt(Document doc, HashMap<String, BayesianNode> netWorkNodes) {
        NodeList definitions = doc.getElementsByTagName("DEFINITION");
        ArrayList<String> currTableNodeNames = new ArrayList<>();
        for (int i = 0; i < definitions.getLength(); i++) {
            currTableNodeNames.clear();
            Node nNode = definitions.item(i);
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element) nNode;
                // given means they are the "FOR" node parents
                currTableNodeNames.add(eElement.getElementsByTagName("FOR").item(0).getTextContent());
                for (int j = 0; j < eElement.getElementsByTagName("GIVEN").getLength(); j++) {
                    netWorkNodes.get(eElement.getElementsByTagName("FOR").item(0).getTextContent()).
                            addParent(eElement.getElementsByTagName("GIVEN").item(j).getTextContent());
                    netWorkNodes.get(eElement.getElementsByTagName("GIVEN").item(j).getTextContent()).
                            addChild(eElement.getElementsByTagName("FOR").item(0).getTextContent());
                    currTableNodeNames.add(eElement.getElementsByTagName("GIVEN").item(j).getTextContent());
                }
//                String probAstr = eElement.getElementsByTagName("TABLE").item(0).getTextContent();
//                ArrayList<String> probAsList = new ArrayList<>(Arrays.asList(probAstr.split(" ")));
//                String curr = "";
//                ArrayList<Integer> currentOutCome = new ArrayList<>();
//                ArrayList<Integer> flipOutCome = new ArrayList<>();
//
//                int how_many = 1;
//                for (int j = 0; j <currTableNodeNames.size() ; j++) {
//                    flipOutCome.add(how_many);
//                    how_many*=netWorkNodes.get(currTableNodeNames.get(j)).outComes.size();
//                }
//                for (int j = 0; j <currTableNodeNames.size() ; j++) {
//                    currentOutCome.add(0);
//                }
//                for (int j = 0; j < probAsList.size(); j++) {
//                    curr = "";
//                    for (int p = 0; p < flipOutCome.size(); p++) {
//                        if(j%flipOutCome.get(p)==0){
//                            currentOutCome.set(p,(currentOutCome.get(p)+1)%
//                                    netWorkNodes.get(currTableNodeNames.get(p)).outComes.size());
//                        }
//                    }
//                    for (int k = 0; k <currTableNodeNames.size() ; k++) {
//                        curr+=currTableNodeNames.get(k)+"=";
//                        curr+=netWorkNodes.get(currTableNodeNames.get(k)).outComes.get(currentOutCome.get(k));
                updateCptHelper(netWorkNodes,eElement,currTableNodeNames);
            }
//                    netWorkNodes.get(eElement.getElementsByTagName("FOR").item(0).getTextContent())
//                            .cpt.put(curr,Double.valueOf(probAsList.get(j)));

                }
            }



    private static void updateCptHelper(HashMap<String, BayesianNode> netWorkNodes ,Element eElement,
                          ArrayList<String> currTableNodeNames) {
        String probAstr = eElement.getElementsByTagName("TABLE").item(0).getTextContent();
        ArrayList<String> probAsList = new ArrayList<>(Arrays.asList(probAstr.split(" ")));
        String curr = "";
        ArrayList<Integer> currentOutCome = new ArrayList<>();
        ArrayList<Integer> flipOutCome = new ArrayList<>();

        int how_many = 1;
        for (int j = 0; j < currTableNodeNames.size(); j++) {
            flipOutCome.add(how_many);
            how_many *= netWorkNodes.get(currTableNodeNames.get(j)).outComes.size();
        }
        for (int j = 0; j < currTableNodeNames.size(); j++) {
            currentOutCome.add(0);
        }
        for (int j = 0; j < probAsList.size(); j++) {
            curr = "";
            for (int p = 0; p < flipOutCome.size(); p++) {
                if (j % flipOutCome.get(p) == 0 &&j>0) {
                    currentOutCome.set(p, (currentOutCome.get(p) + 1) %
                            netWorkNodes.get(currTableNodeNames.get(p)).outComes.size());
                }
            }
            for (int k = 0; k < currTableNodeNames.size(); k++) {
                curr += currTableNodeNames.get(k) + "=";
                curr += netWorkNodes.get(currTableNodeNames.get(k)).outComes.get(currentOutCome.get(k));
                if (k< currTableNodeNames.size()-1){
                    curr+=" ";
                }
            }
            netWorkNodes.get(eElement.getElementsByTagName("FOR").item(0).getTextContent())
                    .cpt.put(curr, Double.valueOf(probAsList.get(j)));
        }


    }
}


