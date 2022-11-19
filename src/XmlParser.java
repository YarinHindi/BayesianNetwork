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

    /**
    this class will build all our network Nodes with all their fields and connections.
     **/
    /**
     *
     * @param fileName Xml file to read from
     * @param netWork the Network Nodes kept as hashmap
     * @return will return true if succeed else false
     *     base function that read the data from the Xml using the helper function below  and using helper library from dom
     *
     */
    public static boolean fileToNetwork(String fileName, HashMap<String, BayesianNode> netWork) {
        try {
            File myXml = new File(fileName);
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuild = documentBuilderFactory.newDocumentBuilder();
            Document document = dBuild.parse(myXml);
            updateNodeNamesAndOutComes(document, netWork);
            updateNodeParentsChildrenAndCpt(document, netWork);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    /**
     *
     * @param document the dom object that helps read the xml
     * @param netWorkNodes the hashmap that keeps our network key is the name of Variable value is the objects itself
     *     This function responsible for updating the Nodes name and list of outcomes
     *     getting the data from xml using the dom objects its easy to traverse the file and get
     *     Name by tags and list of outcomes.
     */

    private static void updateNodeNamesAndOutComes(Document document, HashMap<String, BayesianNode> netWorkNodes) {
        // The line below brings all variable tags in the xml file
        NodeList listOfVariable = document.getElementsByTagName("VARIABLE");
        for (int i = 0; i < listOfVariable.getLength(); i++) {
            Node currNode = listOfVariable.item(i);
            if (currNode.getNodeType() == Node.ELEMENT_NODE) {
                Element currElement = (Element) currNode;
                String varName = currElement.getElementsByTagName("NAME").item(0).getTextContent();
                ArrayList<String> outcomes = new ArrayList<>();
                for (int j = 0; j < currElement.getElementsByTagName("OUTCOME").getLength(); j++) {
                    String outcome = currElement.getElementsByTagName("OUTCOME").item(j).getTextContent();
                    outcomes.add(outcome);
                }
                netWorkNodes.put(varName, new BayesianNode(varName, outcomes));
            }
        }
    }

    /**
     *
     * @param document Same as above function
     * @param netWorkNodes same as above
     *      This function update the parents,children and cpt.
     *
     */
    private static void updateNodeParentsChildrenAndCpt(Document document, HashMap<String, BayesianNode> netWorkNodes) {
        // The line below brings all definition tags in the xml file
        NodeList definitionList = document.getElementsByTagName("DEFINITION");
        //keeps the names for later to help us to make the cpt table.
        ArrayList<String> currTableNodeNames = new ArrayList<>();
        for (int i = 0; i < definitionList.getLength(); i++) {
            currTableNodeNames.clear();
            Node currNode = definitionList.item(i);
            if (currNode.getNodeType() == Node.ELEMENT_NODE) {
                Element currElement = (Element) currNode;
                currTableNodeNames.add(currElement.getElementsByTagName("FOR").item(0).getTextContent());
                //this for loop add the children and parents for the node
                //by keeping the nodes in hashmap it made it easier to search them
                for (int j = 0; j < currElement.getElementsByTagName("GIVEN").getLength(); j++) {
                    netWorkNodes.get(currElement.getElementsByTagName("FOR").item(0).getTextContent()).
                            addParent(currElement.getElementsByTagName("GIVEN").item(j).getTextContent());
                    netWorkNodes.get(currElement.getElementsByTagName("GIVEN").item(j).getTextContent()).
                            addChild(currElement.getElementsByTagName("FOR").item(0).getTextContent());
                    currTableNodeNames.add(currElement.getElementsByTagName("GIVEN").item(j).getTextContent());
                }
                updateCptHelper(netWorkNodes, currElement, currTableNodeNames);
            }
        }
    }

    /**
     *
     * @param netWorkNodes Same as above
     * @param currElement  Dom element that helps to read the xml file.
     * @param currTableNodeNames The names of the variable inside the current DEFINITION tag.
     *                           This function update all Node cpt.
     */
    private static void updateCptHelper(HashMap<String, BayesianNode> netWorkNodes, Element currElement,
                                        ArrayList<String> currTableNodeNames) {
        //probAstr keeps the string with all the probability for the current cpt.
        String probAstr = currElement.getElementsByTagName("TABLE").item(0).getTextContent();
        ArrayList<String> probAsList = new ArrayList<>(Arrays.asList(probAstr.split(" ")));
        String curr = "";

        //currentOutCome keeps the value of a variable cahange his value example : A = T , B2 = v3 ...
        ArrayList<Integer> currentOutCome = new ArrayList<>();
        //flipOutCome keeps  the value of how often (how many iteration ) a variable should change  his value.
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
                if (j % flipOutCome.get(p) == 0 && j > 0) {
                    currentOutCome.set(p, (currentOutCome.get(p) + 1) %
                            netWorkNodes.get(currTableNodeNames.get(p)).outComes.size());
                }
            }
            for (int k = 0; k < currTableNodeNames.size(); k++) {
                curr += currTableNodeNames.get(k) + "=";
                curr += netWorkNodes.get(currTableNodeNames.get(k)).outComes.get(currentOutCome.get(k));
                if (k < currTableNodeNames.size() - 1) {
                    curr += " ";
                }
            }
            netWorkNodes.get(currElement.getElementsByTagName("FOR").item(0).getTextContent())
                    .cpt.put(curr, Double.valueOf(probAsList.get(j)));
        }
    }
}


