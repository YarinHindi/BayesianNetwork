import java.text.DecimalFormat;
import java.util.*;

public class BayesianNetwork {

    public HashMap<String, BayesianNode> networkNodes;;
    public static double numerator = 0;
    public static double denominator = 0;
    public static int numberOfAddOp = 0;
    public static  int numberOfMulOp = 0;

    public BayesianNetwork() {
        this.networkNodes = new HashMap<>();
        this.numberOfAddOp = 0;
        this.numberOfMulOp = 0;
    }

    public void updateNetFromXml(String XmlFile) {
        XmlParser.fileToNetwork(XmlFile, this.networkNodes);
    }

    public BayesianNode returnNodebyName(String name) {
        return networkNodes.get(name);
    }

    public static void resetAns() {
        numerator = 0;
        denominator = 0;
        numberOfAddOp = 0;
        numberOfMulOp = 0;
    }
    public ArrayList<ArrayList<String>> returnEvidenceWithoutoutCome(ArrayList<String> evidence){
        ArrayList<ArrayList<String>> evidencewithoutCome = new ArrayList<>();
        ArrayList<String> temp = new ArrayList<>();
        for (int i = 0; i < evidence.size(); i++) {
            int ind = evidence.get(i).indexOf("=");
            temp.add(evidence.get(i).substring(0,ind));
            temp.add(evidence.get(i).substring(ind+1,evidence.get(i).length()));
            evidencewithoutCome.add(new ArrayList<>(temp));
            temp.clear();
        }
        return evidencewithoutCome;
    }

    public String checkIfAnsInCpt(String query) {
        for (int i = 0; i < networkNodes.size(); i++) {
            for (Map.Entry<String, Double> set : networkNodes.get(i).cpt.entrySet()) {
                if (query.equals(set.getKey())) {
                    return set.getValue()+"";
                }

            }
        }
        return "-1";
    }

    public String AnsweringQuery(ArrayList<String> evidence, ArrayList<String> hidden, String queryVar, String outComequeryVar,String whichCodeToRun,String query) {
        resetAns();
        String ans = "";
        ArrayList<ArrayList<String>> evidenceWithoutComes = returnEvidenceWithoutoutCome(evidence);
        ans = checkIfAnsInCpt(query);
        if(!ans.equals("-1")){
            return ans+","+numberOfAddOp+","+numberOfMulOp;
        }
        switch (whichCodeToRun){
            case "1":
                ArrayList<Boolean> taken = new ArrayList<>(Arrays.asList(new Boolean[hidden.size()]));
                Collections.fill(taken, Boolean.FALSE);
                AllCombinationToCompute(evidence, hidden, taken, 0, queryVar, outComequeryVar, false);
                hidden.add(queryVar);
                evidence.remove(queryVar+"="+outComequeryVar);
                ArrayList<Boolean> taken2 = new ArrayList<>(Arrays.asList(new Boolean[hidden.size()]));
                Collections.fill(taken2, Boolean.FALSE);
                AllCombinationToCompute(evidence, hidden, taken2, 0, queryVar, outComequeryVar, true);
                if (numberOfAddOp>0){
                    numberOfAddOp--;
//                    numberOfAddOp+=networkNodes.get(queryVar).outComes.size()-1;
//                    numberOfAddOp-= networkNodes.get(queryVar).outComes.size();

                }
                ans = (String.format("%.5f", numerator/(denominator+numerator)))+","+numberOfAddOp+","+numberOfMulOp;
                break;
            case "2":
                ans = variableElimination(evidenceWithoutComes,queryVar);
                break;
            case "3":
                break;
        }
        return ans;
    }

    public void AllCombinationToCompute(ArrayList<String> str, ArrayList<String> hidden, ArrayList<Boolean> taken, int index, String queryVar, String outComequeryVar, Boolean flag) {
        if (index == hidden.size()) {
            if (!flag) {
                numerator += answerForOneRow(str);
            }else {
                denominator+= answerForOneRow(str);
            }
            numberOfAddOp++;
            return;
        }
        for (int i = index; i < hidden.size(); i++) {
            for (int j = 0; j < networkNodes.get(hidden.get(i)).outComes.size(); j++) {
                if (!flag) {
                    if (taken.get(i)) continue;
                    str.add(hidden.get(i) + "=" + networkNodes.get(hidden.get(i)).outComes.get(j));
                    taken.set(i, true);
                    AllCombinationToCompute(str, hidden, taken, index + 1,queryVar,outComequeryVar, flag);
                    str.remove(str.size() - 1);
                    taken.set(i, false);
                } else {
                    if (hidden.get(i).equals(queryVar) && networkNodes.get(hidden.get(i)).outComes.get(j).equals(outComequeryVar)) continue;
                    if (taken.get(i)) continue;
                    str.add(hidden.get(i) + "=" + networkNodes.get(hidden.get(i)).outComes.get(j));
                    taken.set(i, true);
                    AllCombinationToCompute(str, hidden, taken, index + 1,queryVar,outComequeryVar, flag);
                    str.remove(str.size() - 1);
                    taken.set(i, false);
                }
            }

        }

    }

    public double answerForOneRow(ArrayList<String> rowToCompute) {
        HashMap<String, String> helper = new HashMap<>();
        double ans = 0;
        boolean first = true;
        String keyToCompute;
        numberOfMulOp+=rowToCompute.size()-1;
        for (int i = 0; i < rowToCompute.size(); i++) {
            int indexForVar = rowToCompute.get(i).indexOf('=');
            String var = rowToCompute.get(i).substring(0, indexForVar);
            String varOutCome = rowToCompute.get(i).substring(indexForVar + 1);
            helper.put(var, varOutCome);
        }
        for (Map.Entry<String, String> set :
                helper.entrySet()) {
            keyToCompute = "P(";
            keyToCompute += set.getKey() + "=" + set.getValue();
            if (networkNodes.get(set.getKey()).parents.size() > 0) keyToCompute += '|';
            for (int i = 0; i < networkNodes.get(set.getKey()).parents.size(); i++) {
                String currParent = networkNodes.get(set.getKey()).parents.get(i);
                keyToCompute += currParent + "=" + helper.get(currParent);
                if (i < networkNodes.get(set.getKey()).parents.size() - 1) {
                    keyToCompute += ',';
                }
            }
            keyToCompute += ')';
                if (first) {
                    ans = networkNodes.get(set.getKey()).cpt.get(keyToCompute);
                    first = false;
                } else {
                    ans *= networkNodes.get(set.getKey()).cpt.get(keyToCompute);
                }
        }
        return ans;

    }

    public void isDe(BayesianNode var,HashSet<String> set){
        if (var==null){
            return;
        }
        for (int i = 0; i < var.parents.size(); i++) {
            set.add(var.parents.get(i));
            isDe(networkNodes.get(var.parents.get(i)),set);

        }

    }

    public String variableElimination(ArrayList<ArrayList<String>> evidence ,String queryVar){
        ArrayList<String> factorsName = dropAllVarsNotAncestor(evidence,queryVar);
        System.out.println(factorsName);
        ArrayList<HashMap<String,Double>> factors = initFactors(factorsName);
        deleteEvidenceFromFactors(factors,evidence);
        System.out.println(factors);

        return "";
    }
    public ArrayList<String> dropAllVarsNotAncestor(ArrayList<ArrayList<String>> evidence  ,String queryVar){
        HashSet<String> ancestorSet = new HashSet<>();
        isDe(networkNodes.get(queryVar),ancestorSet);
        for (int i = 0; i <evidence.size() ; i++) {
                isDe(networkNodes.get(evidence.get(i).get(0)),ancestorSet);
        }
        for (int i = 0; i < evidence.size(); i++) {
            ancestorSet.add(evidence.get(i).get(0));
        }
        ancestorSet.add(queryVar);
        ArrayList<String> factor = new ArrayList<>();
        factor.addAll(ancestorSet);
        return factor;
    }

    public ArrayList<HashMap<String,Double>> initFactors(ArrayList<String> factorsName){
        ArrayList<HashMap<String,Double>> factors = new ArrayList<>();
        for (int i = 0; i < factorsName.size(); i++) {
           factors.add(networkNodes.get(factorsName.get(i)).cpt);
        }
        return factors;
    }

    public ArrayList<String> nodeNamesFromRowOfCpt(String cptRow){
        if (cptRow.contains("|")) {
            int ind = cptRow.indexOf("|");
            String firstVar = cptRow.substring(2,ind);
            if (cptRow.length() > ind+2) {
                int start = cptRow.indexOf("|")+1;
                int end = cptRow.indexOf(")");
                String subbed = cptRow.substring(start, end);
                if (subbed.contains(",")) {
                    ArrayList<String> names = new ArrayList<>(Arrays.asList(subbed.split(",")));
                    names.add(0, firstVar);
                    return names;
                }
                else {
                    ArrayList<String> names = new ArrayList<>();
                    names.add(firstVar);
                    names.add(subbed);
                    return names;
                }
            }
            else {
                ArrayList<String> names = new ArrayList<>();
                names.add(firstVar);
                return names;
            }
        }
        else {
            ArrayList<String> names = new ArrayList<>(Arrays.asList(cptRow.split(",")));
            return names;
        }
    }




    public void deleteEmptyFactor(ArrayList<HashMap<String,Double>> factors){
        for (int i = 0; i < factors.size(); i++) {
            if(factors.get(i).size()==0)factors.remove(factors.get(i));
        }
    }
    public void deleteEvidenceFromFactors( ArrayList<HashMap<String,Double>> factors,ArrayList<ArrayList<String>> evidence) {
//        for (int i = 0; i < factors.size(); i++) {
//            ArrayList<String> keys = new ArrayList<>(factors.get(i).keySet());
//            for (int j = 0; j < keys.size(); j++) {
//                ArrayList<String> key = nodes_name(keys.get(j));
//                for (int k = 0; k < key.size(); k++) {
//                    int ind = key.get(k).indexOf("=");
//                    String var = key.get(k).substring(0,ind);
//                    for (int l = 0; l < evidences.size(); l++) {
//                        int ind1 = evidences.get(l).indexOf("=");
//                        String evid = evidences.get(l).substring(0, ind1);
//                        if (var.equals(evid)) {
//                            if (!key.get(k).equals(evidences.get(l))) {
//                                cpts.get(i).remove(keys.get(j));
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        return cpts;
//    }
//    }

    }
}
